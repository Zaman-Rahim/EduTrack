package com.example.edutrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.edutrack.data.local.dao.StudySessionDao
import com.example.edutrack.data.local.dao.SubjectDao
import com.example.edutrack.data.local.entity.StudySessionEntity
import com.example.edutrack.data.local.entity.SubjectEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Room database for EduTrack storing [SubjectEntity] and [StudySessionEntity] tables.
 * Includes initial dummy data seeding via [SeedDatabaseCallback] on fresh database creation
 * so all dashboard metrics, streaks, charts, and filters can be demonstrated and tested.
 */
@Database(
    entities = [SubjectEntity::class, StudySessionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class EduTrackDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao
    abstract fun studySessionDao(): StudySessionDao

    companion object {
        @Volatile
        private var INSTANCE: EduTrackDatabase? = null

        fun getInstance(context: Context): EduTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): EduTrackDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                EduTrackDatabase::class.java,
                "edutrack_database.db"
            )
                .addCallback(SeedDatabaseCallback(context.applicationContext))
                .build()
        }
    }

    private class SeedDatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                seedData(getInstance(context))
            }
        }

        private suspend fun seedData(database: EduTrackDatabase) {
            val subjectDao = database.subjectDao()
            val sessionDao = database.studySessionDao()

            val mobileDevId = subjectDao.insertSubject(
                SubjectEntity(
                    name = "Mobile Apps Development",
                    colorHex = 0xFFD8F478, // Figma Lime
                    weeklyGoalHours = 6.0
                )
            )
            val dsaId = subjectDao.insertSubject(
                SubjectEntity(
                    name = "Algorithms & Data Structures",
                    colorHex = 0xFFE9E2F7, // Figma Lavender
                    weeklyGoalHours = 5.0
                )
            )
            val dbSystemsId = subjectDao.insertSubject(
                SubjectEntity(
                    name = "Database Systems",
                    colorHex = 0xFFF7E8D5, // Figma Peach
                    weeklyGoalHours = 4.0
                )
            )
            val seId = subjectDao.insertSubject(
                SubjectEntity(
                    name = "Software Engineering",
                    colorHex = 0xFFDCEBF2, // Figma Sky Blue
                    weeklyGoalHours = 3.5
                )
            )

            val today = LocalDate.now().toEpochDay()

            // Seed realistic sessions across the last few days to produce streak + 7-day chart data
            val sampleSessions = listOf(
                StudySessionEntity(
                    subjectId = mobileDevId,
                    durationMinutes = 90,
                    sessionDateEpochDay = today,
                    notes = "Jetpack Compose Scaffold, BottomBar, and Navigation graph setup"
                ),
                StudySessionEntity(
                    subjectId = dsaId,
                    durationMinutes = 60,
                    sessionDateEpochDay = today,
                    notes = "Graph traversal: BFS and DFS implementation and complexity"
                ),
                StudySessionEntity(
                    subjectId = mobileDevId,
                    durationMinutes = 120,
                    sessionDateEpochDay = today - 1,
                    notes = "Room Database, DAO queries, and StateFlow integration"
                ),
                StudySessionEntity(
                    subjectId = dbSystemsId,
                    durationMinutes = 75,
                    sessionDateEpochDay = today - 2,
                    notes = "SQL schema design, foreign keys with CASCADE, and indexing"
                ),
                StudySessionEntity(
                    subjectId = seId,
                    durationMinutes = 45,
                    sessionDateEpochDay = today - 3,
                    notes = "MVVM architecture pattern and repository design review"
                ),
                StudySessionEntity(
                    subjectId = dsaId,
                    durationMinutes = 90,
                    sessionDateEpochDay = today - 4,
                    notes = "Dynamic Programming: Knapsack and Longest Common Subsequence"
                ),
                StudySessionEntity(
                    subjectId = mobileDevId,
                    durationMinutes = 60,
                    sessionDateEpochDay = today - 5,
                    notes = "Material 3 dynamic theming and custom Canvas charts"
                )
            )

            sampleSessions.forEach { sessionDao.insertSession(it) }
        }
    }
}
