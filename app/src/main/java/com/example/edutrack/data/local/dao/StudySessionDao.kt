package com.example.edutrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.edutrack.data.local.entity.StudySessionEntity
import com.example.edutrack.data.local.relation.SessionWithSubject
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for CRUD and reactive queries on [StudySessionEntity].
 */
@Dao
interface StudySessionDao {

    @Transaction
    @Query("SELECT * FROM study_sessions ORDER BY sessionDateEpochDay DESC, createdAt DESC")
    fun observeAllSessionsWithSubject(): Flow<List<SessionWithSubject>>

    @Transaction
    @Query("SELECT * FROM study_sessions WHERE id = :sessionId LIMIT 1")
    fun observeSessionWithSubjectById(sessionId: Long): Flow<SessionWithSubject?>

    @Query("SELECT * FROM study_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): StudySessionEntity?

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM study_sessions WHERE sessionDateEpochDay = :epochDay")
    fun observeTotalMinutesForDay(epochDay: Long): Flow<Int>

    @Query(
        """
        SELECT COALESCE(SUM(durationMinutes), 0) FROM study_sessions
        WHERE sessionDateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        """
    )
    fun observeTotalMinutesInRange(startEpochDay: Long, endEpochDay: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    @Update
    suspend fun updateSession(session: StudySessionEntity)

    @Query("DELETE FROM study_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)
}
