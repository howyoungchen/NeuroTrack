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
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.neurotrack.domain.SleepRecord
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "assessment_records")
data class AssessmentRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAtMillis: Long,
    val answersCsv: String,
    val totalScore: Int,
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
interface SleepRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(record: SleepRecordEntity): Long

    @Query("SELECT * FROM sleep_records WHERE dateEpochDay >= :sinceEpochDay ORDER BY dateEpochDay DESC")
    fun observeSince(sinceEpochDay: Long): Flow<List<SleepRecordEntity>>

    @Query("SELECT * FROM sleep_records WHERE dateEpochDay >= :sinceEpochDay ORDER BY dateEpochDay DESC")
    suspend fun getSince(sinceEpochDay: Long): List<SleepRecordEntity>

    @Query("SELECT * FROM sleep_records WHERE dateEpochDay = :dateEpochDay LIMIT 1")
    suspend fun getByDate(dateEpochDay: Long): SleepRecordEntity?

    @Transaction
    suspend fun insertUnlessMissingDowngrade(record: SleepRecordEntity): Boolean {
        val existing = getByDate(record.dateEpochDay)
        if (record.isMissing && existing?.isMissing == false) return false
        insertOrReplace(record)
        return true
    }
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
        SleepRecordEntity::class,
        AppLogEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class NeuroTrackDatabase : RoomDatabase() {
    abstract fun assessmentDao(): AssessmentDao
    abstract fun sleepRecordDao(): SleepRecordDao
    abstract fun logDao(): LogDao

    companion object {
        @Volatile private var instance: NeuroTrackDatabase? = null
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS screen_events")
            }
        }

        fun getInstance(context: Context): NeuroTrackDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NeuroTrackDatabase::class.java,
                    "neurotrack.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
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

    suspend fun saveSleepRecord(record: SleepRecord) {
        val saved = database.sleepRecordDao().insertUnlessMissingDowngrade(record.toEntity())
        if (!saved) {
            log(
                "INFO",
                "Sleep",
                "Kept usable sleep record date=${record.dateEpochDay} instead of replacing it with missing data",
            )
            return
        }
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
