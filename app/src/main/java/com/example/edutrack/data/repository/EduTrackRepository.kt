package com.example.edutrack.data.repository

import com.example.edutrack.data.local.dao.StudySessionDao
import com.example.edutrack.data.local.dao.SubjectDao
import com.example.edutrack.data.local.entity.StudySessionEntity
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.data.local.relation.SessionWithSubject
import com.example.edutrack.data.local.relation.SubjectWithSessions
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract abstracting data persistence for subjects and study sessions.
 */
interface EduTrackRepository {
    fun observeAllSubjects(): Flow<List<SubjectEntity>>
    fun observeSubjectsWithSessions(): Flow<List<SubjectWithSessions>>
    suspend fun getSubjectById(subjectId: Long): SubjectEntity?
    suspend fun isSubjectNameTaken(name: String, excludeId: Long = 0L): Boolean
    suspend fun insertSubject(subject: SubjectEntity): Long
    suspend fun updateSubject(subject: SubjectEntity)
    suspend fun deleteSubject(subject: SubjectEntity)

    fun observeAllSessionsWithSubject(): Flow<List<SessionWithSubject>>
    fun observeSessionWithSubjectById(sessionId: Long): Flow<SessionWithSubject?>
    suspend fun getSessionById(sessionId: Long): StudySessionEntity?
    fun observeTotalMinutesForDay(epochDay: Long): Flow<Int>
    fun observeTotalMinutesInRange(startEpochDay: Long, endEpochDay: Long): Flow<Int>
    suspend fun insertSession(session: StudySessionEntity): Long
    suspend fun updateSession(session: StudySessionEntity)
    suspend fun deleteSessionById(sessionId: Long)
}

/**
 * Room-backed implementation of [EduTrackRepository].
 */
class OfflineEduTrackRepository(
    private val subjectDao: SubjectDao,
    private val studySessionDao: StudySessionDao
) : EduTrackRepository {

    override fun observeAllSubjects(): Flow<List<SubjectEntity>> =
        subjectDao.observeAllSubjects()

    override fun observeSubjectsWithSessions(): Flow<List<SubjectWithSessions>> =
        subjectDao.observeSubjectsWithSessions()

    override suspend fun getSubjectById(subjectId: Long): SubjectEntity? =
        subjectDao.getSubjectById(subjectId)

    override suspend fun isSubjectNameTaken(name: String, excludeId: Long): Boolean =
        subjectDao.countSubjectsWithName(name, excludeId) > 0

    override suspend fun insertSubject(subject: SubjectEntity): Long =
        subjectDao.insertSubject(subject)

    override suspend fun updateSubject(subject: SubjectEntity) =
        subjectDao.updateSubject(subject)

    override suspend fun deleteSubject(subject: SubjectEntity) =
        subjectDao.deleteSubject(subject)

    override fun observeAllSessionsWithSubject(): Flow<List<SessionWithSubject>> =
        studySessionDao.observeAllSessionsWithSubject()

    override fun observeSessionWithSubjectById(sessionId: Long): Flow<SessionWithSubject?> =
        studySessionDao.observeSessionWithSubjectById(sessionId)

    override suspend fun getSessionById(sessionId: Long): StudySessionEntity? =
        studySessionDao.getSessionById(sessionId)

    override fun observeTotalMinutesForDay(epochDay: Long): Flow<Int> =
        studySessionDao.observeTotalMinutesForDay(epochDay)

    override fun observeTotalMinutesInRange(startEpochDay: Long, endEpochDay: Long): Flow<Int> =
        studySessionDao.observeTotalMinutesInRange(startEpochDay, endEpochDay)

    override suspend fun insertSession(session: StudySessionEntity): Long =
        studySessionDao.insertSession(session)

    override suspend fun updateSession(session: StudySessionEntity) =
        studySessionDao.updateSession(session)

    override suspend fun deleteSessionById(sessionId: Long) =
        studySessionDao.deleteSessionById(sessionId)
}
