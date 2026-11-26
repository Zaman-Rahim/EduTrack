package com.example.edutrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.edutrack.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, dueDateEpochDay ASC, dueTimeMinutes ASC")
    fun observeAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :reminderId LIMIT 1")
    fun observeReminderById(reminderId: Long): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE id = :reminderId LIMIT 1")
    suspend fun getReminderById(reminderId: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: Long)
}
