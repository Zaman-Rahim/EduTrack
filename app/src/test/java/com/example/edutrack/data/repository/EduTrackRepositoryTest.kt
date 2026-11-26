package com.example.edutrack.data.repository

import com.example.edutrack.data.local.dao.StudySessionDao
import com.example.edutrack.data.local.dao.SubjectDao
import com.example.edutrack.data.local.entity.StudySessionEntity
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.data.local.relation.SessionWithSubject
import com.example.edutrack.data.local.relation.SubjectWithSessions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying [OfflineEduTrackRepository] CRUD, cascade deletion,
 * duplicate subject detection, and reactive aggregation streams.
 */
class EduTrackRepositoryTest {

    private class FakeSubjectDao(
        private val subjectsFlow: MutableStateFlow<List<SubjectEntity>>,
        private val sessionsFlow: MutableStateFlow<List<StudySessionEntity>>
    ) : SubjectDao {
        private var nextId = 1L

        override fun observeAllSubjects(): Flow<List<SubjectEntity>> =
            subjectsFlow.map { list -> list.sortedBy { it.name.lowercase() } }

        override fun observeSubjectsWithSessions(): Flow<List<SubjectWithSessions>> =
            combine(subjectsFlow, sessionsFlow) { subjects, sessions ->
                subjects.sortedBy { it.name.lowercase() }.map { subject ->
                    SubjectWithSessions(
                        subject = subject,
                        sessions = sessions.filter { it.subjectId == subject.id }
                    )
                }
            }

        override suspend fun getSubjectById(subjectId: Long): SubjectEntity? =
            subjectsFlow.value.firstOrNull { it.id == subjectId }

        override suspend fun countSubjectsWithName(name: String, excludeId: Long): Int =
            subjectsFlow.value.count {
                it.name.trim().equals(name.trim(), ignoreCase = true) && it.id != excludeId
            }

        override suspend fun insertSubject(subject: SubjectEntity): Long {
            val id = nextId++
            subjectsFlow.value = subjectsFlow.value + subject.copy(id = id)
            return id
        }

        override suspend fun updateSubject(subject: SubjectEntity) {
            subjectsFlow.value = subjectsFlow.value.map {
                if (it.id == subject.id) subject else it
            }
        }

        override suspend fun deleteSubject(subject: SubjectEntity) {
            subjectsFlow.value = subjectsFlow.value.filterNot { it.id == subject.id }
            sessionsFlow.value = sessionsFlow.value.filterNot { it.subjectId == subject.id }
        }
    }

    private class FakeStudySessionDao(
        private val subjectsFlow: MutableStateFlow<List<SubjectEntity>>,
        private val sessionsFlow: MutableStateFlow<List<StudySessionEntity>>
    ) : StudySessionDao {
        private var nextId = 1L

        override fun observeAllSessionsWithSubject(): Flow<List<SessionWithSubject>> =
            combine(subjectsFlow, sessionsFlow) { subjects, sessions ->
                val subjectMap = subjects.associateBy { it.id }
                sessions
                    .sortedWith(
                        compareByDescending<StudySessionEntity> { it.sessionDateEpochDay }
                            .thenByDescending { it.createdAt }
                    )
                    .mapNotNull { session ->
                        subjectMap[session.subjectId]?.let { subject ->
                            SessionWithSubject(session = session, subject = subject)
                        }
                    }
            }

        override fun observeSessionWithSubjectById(sessionId: Long): Flow<SessionWithSubject?> =
            observeAllSessionsWithSubject().map { list ->
                list.firstOrNull { it.session.id == sessionId }
            }

        override suspend fun getSessionById(sessionId: Long): StudySessionEntity? =
            sessionsFlow.value.firstOrNull { it.id == sessionId }

        override fun observeTotalMinutesForDay(epochDay: Long): Flow<Int> =
            sessionsFlow.map { list ->
                list.filter { it.sessionDateEpochDay == epochDay }.sumOf { it.durationMinutes }
            }

        override fun observeTotalMinutesInRange(startEpochDay: Long, endEpochDay: Long): Flow<Int> =
            sessionsFlow.map { list ->
                list.filter { it.sessionDateEpochDay in startEpochDay..endEpochDay }
                    .sumOf { it.durationMinutes }
            }

        override suspend fun insertSession(session: StudySessionEntity): Long {
            val id = nextId++
            sessionsFlow.value = sessionsFlow.value + session.copy(id = id)
            return id
        }

        override suspend fun updateSession(session: StudySessionEntity) {
            sessionsFlow.value = sessionsFlow.value.map {
                if (it.id == session.id) session else it
            }
        }

        override suspend fun deleteSession(session: StudySessionEntity) {
            sessionsFlow.value = sessionsFlow.value.filterNot { it.id == session.id }
        }

        override suspend fun deleteSessionById(sessionId: Long) {
            sessionsFlow.value = sessionsFlow.value.filterNot { it.id == sessionId }
        }
    }

    private fun createRepository(): OfflineEduTrackRepository {
        val subjects = MutableStateFlow<List<SubjectEntity>>(emptyList())
        val sessions = MutableStateFlow<List<StudySessionEntity>>(emptyList())
        return OfflineEduTrackRepository(
            subjectDao = FakeSubjectDao(subjects, sessions),
            studySessionDao = FakeStudySessionDao(subjects, sessions)
        )
    }

    @Test
    fun repository_crudAndCascadeDeleteWorkCorrectly() = runBlocking {
        val repo = createRepository()

        val subjectId = repo.insertSubject(
            SubjectEntity(name = "Mobile Apps", colorHex = 0xFF4F46E5, weeklyGoalHours = 6.0)
        )
        assertTrue(repo.isSubjectNameTaken("mobile apps"))
        assertFalse(repo.isSubjectNameTaken("mobile apps", excludeId = subjectId))

        val sessionId = repo.insertSession(
            StudySessionEntity(
                subjectId = subjectId,
                durationMinutes = 90,
                sessionDateEpochDay = 20050L,
                notes = "Room and Compose navigation"
            )
        )

        val fetched = repo.observeSessionWithSubjectById(sessionId).first()
        assertEquals(90, fetched?.session?.durationMinutes)
        assertEquals("Mobile Apps", fetched?.subject?.name)
        assertEquals(90, repo.observeTotalMinutesForDay(20050L).first())

        repo.updateSession(fetched!!.session.copy(durationMinutes = 120))
        assertEquals(120, repo.observeTotalMinutesForDay(20050L).first())

        val subject = repo.getSubjectById(subjectId)!!
        repo.deleteSubject(subject)

        assertNull(repo.getSessionById(sessionId))
        assertTrue(repo.observeAllSessionsWithSubject().first().isEmpty())
    }
}
