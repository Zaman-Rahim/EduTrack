package com.example.edutrack.data.repository

import com.example.edutrack.data.local.dao.NotificationDao
import com.example.edutrack.data.local.dao.ReminderDao
import com.example.edutrack.data.local.dao.StudySessionDao
import com.example.edutrack.data.local.dao.SubjectDao
import com.example.edutrack.data.local.entity.NotificationEntity
import com.example.edutrack.data.local.entity.NotificationType
import com.example.edutrack.data.local.entity.ReminderEntity
import com.example.edutrack.data.local.entity.ReminderPriority
import com.example.edutrack.data.local.entity.ReminderRepeatType
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

        override suspend fun deleteSessionById(sessionId: Long) {
            sessionsFlow.value = sessionsFlow.value.filterNot { it.id == sessionId }
        }
    }

    private class FakeReminderDao : ReminderDao {
        private val remindersFlow = MutableStateFlow<List<ReminderEntity>>(emptyList())
        private var nextId = 1L

        override fun observeAllReminders(): Flow<List<ReminderEntity>> = remindersFlow

        override fun observeReminderById(reminderId: Long): Flow<ReminderEntity?> =
            remindersFlow.map { list -> list.firstOrNull { it.id == reminderId } }

        override suspend fun getReminderById(reminderId: Long): ReminderEntity? =
            remindersFlow.value.firstOrNull { it.id == reminderId }

        override suspend fun insertReminder(reminder: ReminderEntity): Long {
            val id = nextId++
            remindersFlow.value = remindersFlow.value + reminder.copy(id = id)
            return id
        }

        override suspend fun updateReminder(reminder: ReminderEntity) {
            remindersFlow.value = remindersFlow.value.map {
                if (it.id == reminder.id) reminder else it
            }
        }

        override suspend fun deleteReminderById(reminderId: Long) {
            remindersFlow.value = remindersFlow.value.filterNot { it.id == reminderId }
        }
    }

    private class FakeNotificationDao : NotificationDao {
        private val notificationsFlow = MutableStateFlow<List<NotificationEntity>>(emptyList())
        private var nextId = 1L

        override fun observeAllNotifications(): Flow<List<NotificationEntity>> = notificationsFlow

        override fun observeUnreadCount(): Flow<Int> =
            notificationsFlow.map { list -> list.count { !it.isRead } }

        override suspend fun insertNotification(notification: NotificationEntity): Long {
            val id = nextId++
            notificationsFlow.value = listOf(notification.copy(id = id)) + notificationsFlow.value
            return id
        }

        override suspend fun markAllAsRead() {
            notificationsFlow.value = notificationsFlow.value.map { it.copy(isRead = true) }
        }

        override suspend fun clearAllNotifications() {
            notificationsFlow.value = emptyList()
        }
    }

    private fun createRepository(): OfflineEduTrackRepository {
        val subjects = MutableStateFlow<List<SubjectEntity>>(emptyList())
        val sessions = MutableStateFlow<List<StudySessionEntity>>(emptyList())
        return OfflineEduTrackRepository(
            subjectDao = FakeSubjectDao(subjects, sessions),
            studySessionDao = FakeStudySessionDao(subjects, sessions),
            reminderDao = FakeReminderDao(),
            notificationDao = FakeNotificationDao()
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

    @Test
    fun repository_remindersAndNotificationsWorkCorrectly() = runBlocking {
        val repo = createRepository()

        val reminderId = repo.insertReminder(
            ReminderEntity(
                title = "Submit Lab Report",
                notes = "Upload PDF before 5 PM",
                dueDateEpochDay = 20050L,
                dueTimeMinutes = 1020,
                repeatType = ReminderRepeatType.WEEKLY.name,
                priority = ReminderPriority.HIGH.name
            )
        )
        val savedReminder = repo.getReminderById(reminderId)
        assertEquals("Submit Lab Report", savedReminder?.title)
        assertFalse(savedReminder?.isCompleted ?: true)

        repo.updateReminder(savedReminder!!.copy(isCompleted = true))
        assertTrue(repo.observeReminderById(reminderId).first()?.isCompleted ?: false)

        repo.insertNotification(
            NotificationEntity(
                title = "Focus complete",
                message = "Logged 25 min for Mobile Apps",
                type = NotificationType.FOCUS_COMPLETE.name
            )
        )
        assertEquals(1, repo.observeUnreadNotificationCount().first())

        repo.markAllNotificationsAsRead()
        assertEquals(0, repo.observeUnreadNotificationCount().first())

        repo.deleteReminderById(reminderId)
        assertNull(repo.getReminderById(reminderId))
    }

    @Test
    fun repository_deleteSessionById_removesSessionAndUpdatesTotals() = runBlocking {
        val repo = createRepository()

        val subjectId = repo.insertSubject(
            SubjectEntity(name = "Algorithms", colorHex = 0xFF0EA5E9, weeklyGoalHours = 5.0)
        )
        val sessionId = repo.insertSession(
            StudySessionEntity(
                subjectId = subjectId,
                durationMinutes = 45,
                sessionDateEpochDay = 20051L,
                notes = "Dynamic programming practice"
            )
        )
        assertEquals(45, repo.observeTotalMinutesInRange(20050L, 20052L).first())

        repo.deleteSessionById(sessionId)

        assertNull(repo.getSessionById(sessionId))
        assertEquals(0, repo.observeTotalMinutesInRange(20050L, 20052L).first())
    }

    @Test
    fun repository_clearAllNotifications_clearsInboxAndUnreadCount() = runBlocking {
        val repo = createRepository()

        repo.insertNotification(
            NotificationEntity(
                title = "Focus complete",
                message = "Logged 25 min for Algorithms",
                type = NotificationType.FOCUS_COMPLETE.name,
                isRead = false
            )
        )
        repo.insertNotification(
            NotificationEntity(
                title = "Reminder due",
                message = "Review lecture notes",
                type = NotificationType.REMINDER.name,
                isRead = true
            )
        )

        assertEquals(2, repo.observeAllNotifications().first().size)
        assertEquals(1, repo.observeUnreadNotificationCount().first())

        repo.clearAllNotifications()

        assertTrue(repo.observeAllNotifications().first().isEmpty())
        assertEquals(0, repo.observeUnreadNotificationCount().first())
    }
}
