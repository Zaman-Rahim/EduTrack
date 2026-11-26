package com.example.edutrack.ui.navigation

/**
 * Navigation route contracts for all screens in EduTrack.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object History : Screen("history?subjectId={subjectId}") {
        const val ARG_SUBJECT_ID = "subjectId"
        fun createRoute(subjectId: Long = -1L): String = "history?subjectId=$subjectId"
    }
    data object Subjects : Screen("subjects")
    data object Stats : Screen("stats")
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
