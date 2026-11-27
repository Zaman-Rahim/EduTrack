package com.example.edutrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.edutrack.data.local.dao.NotificationDao
import com.example.edutrack.data.local.dao.ReminderDao
import com.example.edutrack.data.local.dao.StudySessionDao
import com.example.edutrack.data.local.dao.SubjectDao
import com.example.edutrack.data.local.entity.NotificationEntity
import com.example.edutrack.data.local.entity.ReminderEntity
import com.example.edutrack.data.local.entity.StudySessionEntity
import com.example.edutrack.data.local.entity.SubjectEntity

/**
 * Room database for EduTrack storing subjects, study sessions, reminders, and notifications.
 * Starts completely empty for new users (no pre-populated demo data).
 */
@Database(
    entities = [
        SubjectEntity::class,
        StudySessionEntity::class,
        ReminderEntity::class,
        NotificationEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class EduTrackDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun reminderDao(): ReminderDao
    abstract fun notificationDao(): NotificationDao

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
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
