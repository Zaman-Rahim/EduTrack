package com.example.edutrack.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.data.local.relation.SubjectWithSessions
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing [SubjectEntity] records and subject-session aggregates.
 */
@Dao
interface SubjectDao {

    @Query("SELECT * FROM subjects ORDER BY name COLLATE NOCASE ASC")
    fun observeAllSubjects(): Flow<List<SubjectEntity>>

    @Transaction
    @Query("SELECT * FROM subjects ORDER BY name COLLATE NOCASE ASC")
    fun observeSubjectsWithSessions(): Flow<List<SubjectWithSessions>>

    @Query("SELECT * FROM subjects WHERE id = :subjectId LIMIT 1")
    suspend fun getSubjectById(subjectId: Long): SubjectEntity?

    @Query("SELECT COUNT(*) FROM subjects WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND id != :excludeId")
    suspend fun countSubjectsWithName(name: String, excludeId: Long = 0L): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)
}
