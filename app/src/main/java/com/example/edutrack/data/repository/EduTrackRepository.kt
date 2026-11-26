package com.example.edutrack.data.repository

import com.example.edutrack.data.local.dao.NotificationDao
import com.example.edutrack.data.local.dao.ReminderDao
import com.example.edutrack.data.local.dao.StudySessionDao
import com.example.edutrack.data.local.dao.SubjectDao
import com.example.edutrack.data.local.entity.NotificationEntity
import com.example.edutrack.data.local.entity.ReminderEntity
import com.example.edutrack.data.local.entity.StudySessionEntity
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.data.local.relation.SessionWithSubject
import com.example.edutrack.data.local.relation.SubjectWithSessions
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract abstracting data persistence for subjects, sessions, reminders, and notifications.
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

    fun observeAllReminders(): Flow<List<ReminderEntity>>
    fun observeReminderById(reminderId: Long): Flow<ReminderEntity?>
    suspend fun getReminderById(reminderId: Long): ReminderEntity?
    suspend fun insertReminder(reminder: ReminderEntity): Long
    suspend fun updateReminder(reminder: ReminderEntity)
    suspend fun deleteReminderById(reminderId: Long)

    fun observeAllNotifications(): Flow<List<NotificationEntity>>
    fun observeUnreadNotificationCount(): Flow<Int>
    suspend fun insertNotification(notification: NotificationEntity): Long
    suspend fun markAllNotificationsAsRead()
    suspend fun clearAllNotifications()
}

/**
 * Room-backed implementation of [EduTrackRepository].
 */
class OfflineEduTrackRepository(
    private val subjectDao: SubjectDao,
    private val studySessionDao: StudySessionDao,
    private val reminderDao: ReminderDao,
    private val notificationDao: NotificationDao
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

    override fun observeAllReminders(): Flow<List<ReminderEntity>> =
        reminderDao.observeAllReminders()

    override fun observeReminderById(reminderId: Long): Flow<ReminderEntity?> =
        reminderDao.observeReminderById(reminderId)

    override suspend fun getReminderById(reminderId: Long): ReminderEntity? =
        reminderDao.getReminderById(reminderId)

    override suspend fun insertReminder(reminder: ReminderEntity): Long =
        reminderDao.insertReminder(reminder)

    override suspend fun updateReminder(reminder: ReminderEntity) =
        reminderDao.updateReminder(reminder)

    override suspend fun deleteReminderById(reminderId: Long) =
        reminderDao.deleteReminderById(reminderId)

    override fun observeAllNotifications(): Flow<List<NotificationEntity>> =
        notificationDao.observeAllNotifications()

    override fun observeUnreadNotificationCount(): Flow<Int> =
        notificationDao.observeUnreadCount()

    override suspend fun insertNotification(notification: NotificationEntity): Long =
        notificationDao.insertNotification(notification)

    override suspend fun markAllNotificationsAsRead() =
        notificationDao.markAllAsRead()

    override suspend fun clearAllNotifications() =
        notificationDao.clearAllNotifications()
}
