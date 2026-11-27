package com.example.edutrack.ui.navigation

/**
 * Navigation route contracts for all screens in EduTrack.
 */
sealed class Screen(val route: String) {
    data object AddEditSubject : Screen("add_edit_subject?subjectId={subjectId}") {
        const val ARG_SUBJECT_ID = "subjectId"
        fun createRoute(subjectId: Long = -1L): String = "add_edit_subject?subjectId=$subjectId"
    }

    data object AddEditReminder : Screen("add_edit_reminder?reminderId={reminderId}") {
        const val ARG_REMINDER_ID = "reminderId"
        fun createRoute(reminderId: Long = -1L): String = "add_edit_reminder?reminderId=$reminderId"
    }

    data object ReminderDetail : Screen("reminder_detail/{reminderId}") {
        const val ARG_REMINDER_ID = "reminderId"
        fun createRoute(reminderId: Long): String = "reminder_detail/$reminderId"
    }

    data object Notifications : Screen("notifications")
    data object Me : Screen("me")

    data object AddEditSession : Screen("add_edit_session?sessionId={sessionId}&subjectId={subjectId}") {
        const val ARG_SESSION_ID = "sessionId"
        const val ARG_SUBJECT_ID = "subjectId"
        fun createRoute(sessionId: Long = -1L, subjectId: Long = -1L): String =
            "add_edit_session?sessionId=$sessionId&subjectId=$subjectId"
    }
    data object SessionDetail : Screen("session_detail/{sessionId}") {
        const val ARG_SESSION_ID = "sessionId"
        fun createRoute(sessionId: Long): String = "session_detail/$sessionId"
    }
}
