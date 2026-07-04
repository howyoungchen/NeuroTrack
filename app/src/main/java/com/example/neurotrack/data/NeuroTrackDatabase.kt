package com.example.neurotrack.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

const val SCREEN_ON = "SCREEN_ON"
const val SCREEN_OFF = "SCREEN_OFF"

@Entity(tableName = "assessment_records")
data class AssessmentRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAtMillis: Long,
    val answersCsv: String,
    val totalScore: Int,
)

@Entity(tableName = "screen_events")
data class ScreenEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val eventType: String,
)

@Entity(
    tableName = "sleep_records",
    indices = [Index(value = ["dateEpochDay"], unique = true)],
)
data class SleepRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val sleepStartMillis: Long,
    val sleepEndMillis: Long,
    val durationMinutes: Int,
    val wakeUpCount: Int,
    val isMissing: Boolean,
    val createdAtMillis: Long,
)

@Entity(tableName = "app_logs")
data class AppLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val level: String,
    val tag: String,
    val message: String,
    val stackTrace: String? = null,
)

@Dao
interface AssessmentDao {
    @Insert
    suspend fun insert(record: AssessmentRecordEntity): Long

    @Query("SELECT * FROM assessment_records ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<AssessmentRecordEntity>>

    @Query("SELECT * FROM assessment_records WHERE createdAtMillis >= :sinceMillis ORDER BY createdAtMillis DESC")
    suspend fun getSince(sinceMillis: Long): List<AssessmentRecordEntity>

    @Query("SELECT * FROM assessment_records ORDER BY createdAtMillis DESC LIMIT 1")
    suspend fun getLatest(): AssessmentRecordEntity?
}

@Dao
interface ScreenEventDao {
    @Insert
    suspend fun insert(event: ScreenEventEntity): Long

    @Query("SELECT * FROM screen_events WHERE timestampMillis BETWEEN :startMillis AND :endMillis ORDER BY timestampMillis ASC")
    suspend fun getBetween(startMillis: Long, endMillis: Long): List<ScreenEventEntity>

    @Query("SELECT * FROM screen_events ORDER BY timestampMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ScreenEventEntity>>
}

@Dao
interface SleepRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(record: SleepRecordEntity): Long

    @Query("SELECT * FROM sleep_records WHERE dateEpochDay >= :sinceEpochDay ORDER BY dateEpochDay DESC")
    fun observeSince(sinceEpochDay: Long): Flow<List<SleepRecordEntity>>

    @Query("SELECT * FROM sleep_records WHERE dateEpochDay >= :sinceEpochDay ORDER BY dateEpochDay DESC")
    suspend fun getSince(sinceEpochDay: Long): List<SleepRecordEntity>
}

@Dao
interface LogDao {
    @Insert
    suspend fun insert(log: AppLogEntity): Long

    @Query("SELECT * FROM app_logs WHERE timestampMillis >= :sinceMillis ORDER BY timestampMillis DESC")
    suspend fun getSince(sinceMillis: Long): List<AppLogEntity>

    @Query("DELETE FROM app_logs WHERE timestampMillis < :beforeMillis")
    suspend fun deleteBefore(beforeMillis: Long)
}

@Database(
    entities = [
        AssessmentRecordEntity::class,
        ScreenEventEntity::class,
        SleepRecordEntity::class,
        AppLogEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class NeuroTrackDatabase : RoomDatabase() {
    abstract fun assessmentDao(): AssessmentDao
    abstract fun screenEventDao(): ScreenEventDao
    abstract fun sleepRecordDao(): SleepRecordDao
    abstract fun logDao(): LogDao

    companion object {
        @Volatile private var instance: NeuroTrackDatabase? = null

        fun getInstance(context: Context): NeuroTrackDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NeuroTrackDatabase::class.java,
                    "neurotrack.db",
                ).build().also { instance = it }
            }
        }
    }
}

class NeuroRepository(
    private val database: NeuroTrackDatabase,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    val assessmentHistory: Flow<List<AssessmentRecordEntity>> =
        database.assessmentDao().observeAll()

    fun observeSleepRecords(sinceEpochDay: Long): Flow<List<SleepRecordEntity>> =
        database.sleepRecordDao().observeSince(sinceEpochDay)

    suspend fun submitAssessment(answers: List<Int>): AssessmentRecordEntity {
        val total = answers.sum()
        val record = AssessmentRecordEntity(
            createdAtMillis = nowMillis(),
            answersCsv = answers.joinToString(","),
            totalScore = total,
        )
        val id = database.assessmentDao().insert(record)
        log("INFO", "Assessment", "Submitted assessment total=$total answers=${record.answersCsv}")
        return record.copy(id = id)
    }

    suspend fun saveSleepRecord(record: SleepRecordEntity) {
        database.sleepRecordDao().insertOrReplace(record)
        log(
            "INFO",
            "Sleep",
            "Saved sleep record date=${record.dateEpochDay} duration=${record.durationMinutes} missing=${record.isMissing}",
        )
    }

    suspend fun getRecentAssessments(sinceMillis: Long): List<AssessmentRecordEntity> =
        database.assessmentDao().getSince(sinceMillis)

    suspend fun getRecentLogs(sinceMillis: Long): List<AppLogEntity> =
        database.logDao().getSince(sinceMillis)

    suspend fun pruneLogs(retentionMillis: Long) {
        database.logDao().deleteBefore(nowMillis() - retentionMillis)
    }

    suspend fun log(level: String, tag: String, message: String, throwable: Throwable? = null) {
        database.logDao().insert(
            AppLogEntity(
                timestampMillis = nowMillis(),
                level = level,
                tag = tag,
                message = message,
                stackTrace = throwable?.stackTraceToString(),
            ),
        )
    }
}
