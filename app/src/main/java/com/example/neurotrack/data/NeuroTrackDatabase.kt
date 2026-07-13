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
import com.example.neurotrack.domain.MindfulnessSessionStatus
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "weekly_assessments",
    indices = [Index(value = ["weekStartEpochDay"], unique = true)],
)
data class AssessmentRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekStartEpochDay: Long,
    val createdAtMillis: Long,
    val answersCsv: String,
    val totalScore: Int,
)

@Entity(tableName = "mindfulness_sessions")
data class MindfulnessSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val plannedDurationMinutes: Int,
    val status: String,
)

@Dao
interface AssessmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: AssessmentRecordEntity): Long

    @Query("SELECT * FROM weekly_assessments ORDER BY weekStartEpochDay DESC")
    fun observeAll(): Flow<List<AssessmentRecordEntity>>
}

@Dao
interface MindfulnessDao {
    @Insert
    suspend fun insert(record: MindfulnessSessionEntity): Long

    @Query("UPDATE mindfulness_sessions SET endedAtMillis = :endedAtMillis, status = :status WHERE id = :id")
    suspend fun finish(id: Long, endedAtMillis: Long, status: String)

    @Query("SELECT * FROM mindfulness_sessions ORDER BY startedAtMillis DESC")
    fun observeAll(): Flow<List<MindfulnessSessionEntity>>
}

@Database(
    entities = [AssessmentRecordEntity::class, MindfulnessSessionEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class NeuroTrackDatabase : RoomDatabase() {
    abstract fun assessmentDao(): AssessmentDao
    abstract fun mindfulnessDao(): MindfulnessDao

    companion object {
        @Volatile private var instance: NeuroTrackDatabase? = null

        fun getInstance(context: Context): NeuroTrackDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NeuroTrackDatabase::class.java,
                    "neurotrack.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}

class NeuroRepository(
    private val database: NeuroTrackDatabase,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    val assessmentHistory: Flow<List<AssessmentRecordEntity>> = database.assessmentDao().observeAll()
    val mindfulnessHistory: Flow<List<MindfulnessSessionEntity>> = database.mindfulnessDao().observeAll()

    suspend fun submitAssessment(
        weekStartEpochDay: Long,
        answers: List<Int>,
    ): AssessmentRecordEntity {
        require(answers.size == 10 && answers.all { it in 0..3 })
        val record = AssessmentRecordEntity(
            weekStartEpochDay = weekStartEpochDay,
            createdAtMillis = nowMillis(),
            answersCsv = answers.joinToString(","),
            totalScore = answers.sum(),
        )
        val id = database.assessmentDao().insert(record)
        return record.copy(id = id)
    }

    suspend fun startMindfulness(plannedDurationMinutes: Int): MindfulnessSessionEntity {
        require(plannedDurationMinutes > 0)
        val record = MindfulnessSessionEntity(
            startedAtMillis = nowMillis(),
            plannedDurationMinutes = plannedDurationMinutes,
            status = MindfulnessSessionStatus.IN_PROGRESS.name,
        )
        return record.copy(id = database.mindfulnessDao().insert(record))
    }

    suspend fun finishMindfulness(id: Long, status: MindfulnessSessionStatus) {
        require(status != MindfulnessSessionStatus.IN_PROGRESS)
        database.mindfulnessDao().finish(id, nowMillis(), status.name)
    }
}
