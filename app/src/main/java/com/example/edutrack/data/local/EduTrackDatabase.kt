package com.example.edutrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE subjects ADD COLUMN dailyGoalMinutes INTEGER NOT NULL DEFAULT 25")
                db.execSQL("ALTER TABLE subjects ADD COLUMN repeatMode TEXT NOT NULL DEFAULT 'EVERY_DAY'")
                db.execSQL("ALTER TABLE subjects ADD COLUMN scheduledDaysMask INTEGER NOT NULL DEFAULT 127")
                db.execSQL("ALTER TABLE subjects ADD COLUMN startDateEpochDay INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `notes` TEXT NOT NULL, `dueDateEpochDay` INTEGER NOT NULL, `dueTimeMinutes` INTEGER NOT NULL, `repeatType` TEXT NOT NULL, `priority` TEXT NOT NULL, `notifyEnabled` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `completedAt` INTEGER, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notifications` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `message` TEXT NOT NULL, `type` TEXT NOT NULL, `isRead` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN repeatDaysMask INTEGER NOT NULL DEFAULT 21")
            }
        }

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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }
    }
}
