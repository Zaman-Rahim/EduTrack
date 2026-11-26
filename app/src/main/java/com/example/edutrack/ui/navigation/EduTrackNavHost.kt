package com.example.edutrack.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.edutrack.AppContainer
import com.example.edutrack.R
import com.example.edutrack.ui.components.LiveFocusBar
import com.example.edutrack.ui.screens.addedit.AddEditSessionScreen
import com.example.edutrack.ui.screens.detail.SessionDetailScreen
import com.example.edutrack.ui.screens.focus.FocusTimerScreen
import com.example.edutrack.ui.screens.history.HistoryScreen
import com.example.edutrack.ui.screens.home.HomeScreen
import com.example.edutrack.ui.screens.me.MeScreen
import com.example.edutrack.ui.screens.notifications.NotificationsScreen
import com.example.edutrack.ui.screens.reminders.AddEditReminderScreen
import com.example.edutrack.ui.screens.reminders.ReminderDetailScreen
import com.example.edutrack.ui.screens.reminders.RemindersScreen
import com.example.edutrack.ui.screens.stats.StatsScreen
import com.example.edutrack.ui.screens.subjects.AddEditSubjectScheduleScreen
import com.example.edutrack.ui.screens.subjects.SubjectManagerScreen
import com.example.edutrack.ui.theme.ForestGreenPrimary
import com.example.edutrack.ui.theme.LimeAccent
import com.example.edutrack.ui.theme.MutedText
import com.example.edutrack.ui.theme.TrackBorder
import com.example.edutrack.ui.timer.TimerStatus
import com.example.edutrack.ui.viewmodel.AddEditSessionEvent
import com.example.edutrack.ui.viewmodel.AddEditSessionViewModel
import com.example.edutrack.ui.viewmodel.AppViewModelFactory
import com.example.edutrack.ui.viewmodel.HistoryViewModel
import com.example.edutrack.ui.viewmodel.HomeViewModel
import com.example.edutrack.ui.viewmodel.NotificationsViewModel
import com.example.edutrack.ui.viewmodel.RemindersEvent
import com.example.edutrack.ui.viewmodel.RemindersViewModel
import com.example.edutrack.ui.viewmodel.SessionDetailEvent
import com.example.edutrack.ui.viewmodel.SessionDetailViewModel
import com.example.edutrack.ui.viewmodel.StatsViewModel
import com.example.edutrack.ui.viewmodel.SubjectManagerEvent
import com.example.edutrack.ui.viewmodel.SubjectManagerViewModel
import kotlinx.coroutines.launch

private data class BottomNavTab(
    val route: String,
    val baseRoutePrefix: String,
    val label: String,
    @DrawableRes val iconRes: Int
)

private val bottomNavTabs = listOf(
    BottomNavTab(
        route = Screen.Home.route,
        baseRoutePrefix = "home",
        label = "Home",
        iconRes = R.drawable.ic_figma_home
    ),
    BottomNavTab(
        route = Screen.History.createRoute(),
        baseRoutePrefix = "history",
        label = "History",
        iconRes = R.drawable.ic_figma_history
    ),
    BottomNavTab(
        route = Screen.Subjects.route,
        baseRoutePrefix = "subjects",
        label = "Subjects",
        iconRes = R.drawable.ic_figma_book
    ),
    BottomNavTab(
        route = Screen.Reminders.route,
        baseRoutePrefix = "reminders",
        label = "Reminders",
        iconRes = R.drawable.ic_figma_reminder
    ),
    BottomNavTab(
        route = Screen.Stats.route,
        baseRoutePrefix = "stats",
        label = "Progress",
        iconRes = R.drawable.ic_figma_stats
    )
)

@Composable
fun EduTrackNavHost(
    container: AppContainer,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = container.repository
    val focusTimerManager = container.focusTimerManager
    val userPreferencesManager = container.userPreferences

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showFeedbackSnackbar(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route.orEmpty()

    val isTopLevelScreen = bottomNavTabs.any { currentRoute.startsWith(it.baseRoutePrefix) }

    val homeViewModel: HomeViewModel = viewModel(factory = AppViewModelFactory(repository))
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    val subjectManagerViewModel: SubjectManagerViewModel = viewModel(
        factory = AppViewModelFactory(repository)
    )
    val remindersViewModel: RemindersViewModel = viewModel(
        factory = AppViewModelFactory(repository)
    )
    val notificationsViewModel: NotificationsViewModel = viewModel(
        factory = AppViewModelFactory(repository)
    )

    val notificationsState by notificationsViewModel.uiState.collectAsStateWithLifecycle()
    val timerState by focusTimerManager.state.collectAsStateWithLifecycle()
    val prefsState by userPreferencesManager.state.collectAsStateWithLifecycle()

    val unreadCount = notificationsState.unreadCount
    val showLiveBar = timerState.isActive &&
        timerState.isMinimized &&
        currentRoute != Screen.FocusTimer.route

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Column {
                AnimatedVisibility(
                    visible = showLiveBar,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
                ) {
                    LiveFocusBar(
                        timerState = timerState,
                        onExpandClick = {
                            focusTimerManager.expand()
                            navController.navigate(Screen.FocusTimer.route) {
                                launchSingleTop = true
                            }
                        },
                        onPauseResumeOrDismissClick = {
                            if (timerState.status == TimerStatus.COMPLETED) {
                                focusTimerManager.dismissOrCancel()
                            } else {
                                focusTimerManager.togglePauseResume()
                            }
                        }
                    )
                }

                if (isTopLevelScreen) {
                    HorizontalDivider(color = TrackBorder, thickness = 1.dp)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavTabs.forEach { tab ->
                            val selected = currentRoute.startsWith(tab.baseRoutePrefix)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        if (tab.baseRoutePrefix == "home") {
                                            val popped = navController.popBackStack(
                                                Screen.Home.route,
                                                inclusive = false
                                            )
                                            if (!popped) {
                                                navController.navigate(Screen.Home.route) {
                                                    launchSingleTop = true
                                                }
                                            }
                                        } else {
                                            navController.navigate(tab.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    inclusive = false
                                                }
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(id = tab.iconRes),
                                        contentDescription = tab.label,
                                        modifier = Modifier.size(21.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ForestGreenPrimary,
                                    selectedTextColor = if (isDarkTheme) LimeAccent else ForestGreenPrimary,
                                    indicatorColor = LimeAccent,
                                    unselectedIconColor = MutedText,
                                    unselectedTextColor = MutedText
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    uiState = homeUiState,
                    unreadNotificationsCount = unreadCount,
                    onNotificationsClick = {
                        navController.navigate(Screen.Notifications.route)
                    },
                    onMeClick = {
                        navController.navigate(Screen.Me.route)
                    },
                    onAddFirstSubjectClick = {
                        navController.navigate(Screen.AddEditSubject.createRoute())
                    },
                    onAddSessionClick = {
                        navController.navigate(Screen.AddEditSession.createRoute())
                    },
                    onQuickAddForSubject = { subjectId ->
                        navController.navigate(
                            Screen.AddEditSession.createRoute(subjectId = subjectId)
                        )
                    },
                    onSubjectClick = { subjectId ->
                        navController.navigate(Screen.History.createRoute(subjectId = subjectId)) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onSessionClick = { sessionId ->
                        navController.navigate(Screen.SessionDetail.createRoute(sessionId))
                    },
                    onManageSubjectsClick = {
                        navController.navigate(Screen.Subjects.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onViewAllHistoryClick = {
                        navController.navigate(Screen.History.createRoute()) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = false
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = Screen.History.route,
                arguments = listOf(
                    navArgument(Screen.History.ARG_SUBJECT_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { entry ->
                val initialSubjectId = entry.arguments?.getLong(Screen.History.ARG_SUBJECT_ID) ?: -1L
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = AppViewModelFactory(repository, initialSubjectId = initialSubjectId)
                )
                val uiState by historyViewModel.uiState.collectAsStateWithLifecycle()

                HistoryScreen(
                    uiState = uiState,
                    unreadNotificationsCount = unreadCount,
                    onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                    onMeClick = { navController.navigate(Screen.Me.route) },
                    onSearchQueryChanged = historyViewModel::onSearchQueryChanged,
                    onSelectSubjectFilter = historyViewModel::selectSubjectFilter,
                    onSelectDateFilter = historyViewModel::selectDateFilter,
                    onCustomDateRangeSelected = historyViewModel::setCustomDateRange,
                    onClearFilters = historyViewModel::clearFilters,
                    onSessionClick = { sessionId ->
                        navController.navigate(Screen.SessionDetail.createRoute(sessionId))
                    },
                    onLogSessionClick = {
                        navController.navigate(Screen.AddEditSession.createRoute())
                    }
                )
            }

            composable(Screen.Subjects.route) {
                val uiState by subjectManagerViewModel.uiState.collectAsStateWithLifecycle()

                SubjectManagerScreen(
                    uiState = uiState,
                    unreadNotificationsCount = unreadCount,
                    onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                    onMeClick = { navController.navigate(Screen.Me.route) },
                    onAddClick = {
                        navController.navigate(Screen.AddEditSubject.createRoute())
                    },
                    onEditScheduleClick = { subjectId ->
                        navController.navigate(Screen.AddEditSubject.createRoute(subjectId))
                    },
                    onStartFocusClick = { subject ->
                        focusTimerManager.startFocus(subject)
                        navController.navigate(Screen.FocusTimer.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = Screen.AddEditSubject.route,
                arguments = listOf(
                    navArgument(Screen.AddEditSubject.ARG_SUBJECT_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { entry ->
                val subjectId = entry.arguments?.getLong(Screen.AddEditSubject.ARG_SUBJECT_ID) ?: -1L
                val uiState by subjectManagerViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(subjectId) {
                    subjectManagerViewModel.loadSubjectForEdit(subjectId)
                }

                LaunchedEffect(Unit) {
                    subjectManagerViewModel.events.collect { event ->
                        when (event) {
                            is SubjectManagerEvent.Feedback -> showFeedbackSnackbar(event.message)
                            is SubjectManagerEvent.SavedSuccess -> navController.popBackStack()
                        }
                    }
                }

                val editingSubject = uiState.allSubjects
                    .firstOrNull { it.subject.id == subjectId }
                    ?.subject

                AddEditSubjectScheduleScreen(
                    formState = uiState.formState,
                    editingSubject = editingSubject,
                    onBackClick = { navController.popBackStack() },
                    onNameChanged = subjectManagerViewModel::onNameChanged,
                    onDailyHoursChanged = subjectManagerViewModel::onDailyHoursChanged,
                    onDailyMinutesChanged = subjectManagerViewModel::onDailyMinutesChanged,
                    onStartDateSelected = subjectManagerViewModel::onStartDateSelected,
                    onRepeatModeSelected = subjectManagerViewModel::onRepeatModeSelected,
                    onToggleDayBit = subjectManagerViewModel::onToggleDayBit,
                    onColorSelected = subjectManagerViewModel::onColorSelected,
                    onSaveClick = subjectManagerViewModel::saveSubject,
                    onDeleteClick = { subject ->
                        subjectManagerViewModel.deleteSubject(subject)
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.FocusTimer.route) {
                FocusTimerScreen(
                    timerState = timerState,
                    onMinimizeClick = {
                        focusTimerManager.minimize()
                        navController.popBackStack()
                    },
                    onPauseResumeClick = focusTimerManager::togglePauseResume,
                    onEndWithoutCompletingClick = {
                        focusTimerManager.dismissOrCancel()
                        navController.popBackStack()
                    },
                    onOpenNotificationsClick = {
                        focusTimerManager.dismissOrCancel()
                        navController.navigate(Screen.Notifications.route) {
                            popUpTo(Screen.FocusTimer.route) { inclusive = true }
                        }
                    },
                    onBackToPlanClick = {
                        focusTimerManager.dismissOrCancel()
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Reminders.route) {
                val uiState by remindersViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    remindersViewModel.events.collect { event ->
                        if (event is RemindersEvent.Feedback) {
                            showFeedbackSnackbar(event.message)
                        }
                    }
                }

                RemindersScreen(
                    uiState = uiState,
                    unreadNotificationsCount = unreadCount,
                    onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                    onMeClick = { navController.navigate(Screen.Me.route) },
                    onAddReminderClick = {
                        navController.navigate(Screen.AddEditReminder.createRoute())
                    },
                    onSelectTab = remindersViewModel::selectTab,
                    onToggleComplete = remindersViewModel::toggleComplete,
                    onReminderClick = { reminderId ->
                        navController.navigate(Screen.ReminderDetail.createRoute(reminderId))
                    }
                )
            }

            composable(
                route = Screen.AddEditReminder.route,
                arguments = listOf(
                    navArgument(Screen.AddEditReminder.ARG_REMINDER_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { entry ->
                val reminderId = entry.arguments?.getLong(Screen.AddEditReminder.ARG_REMINDER_ID) ?: -1L
                val uiState by remindersViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(reminderId) {
                    remindersViewModel.loadReminderForEdit(reminderId)
                }

                LaunchedEffect(Unit) {
                    remindersViewModel.events.collect { event ->
                        when (event) {
                            is RemindersEvent.Feedback -> showFeedbackSnackbar(event.message)
                            is RemindersEvent.SavedSuccess -> navController.popBackStack()
                            else -> Unit
                        }
                    }
                }

                AddEditReminderScreen(
                    state = uiState.editorState,
                    onBackClick = { navController.popBackStack() },
                    onTitleChanged = remindersViewModel::onTitleChanged,
                    onNotesChanged = remindersViewModel::onNotesChanged,
                    onDueDateSelected = remindersViewModel::onDueDateSelected,
                    onDueTimeSelected = remindersViewModel::onDueTimeSelected,
                    onRepeatSelected = remindersViewModel::onRepeatTypeSelected,
                    onPrioritySelected = remindersViewModel::onPrioritySelected,
                    onNotifyToggle = remindersViewModel::onNotifyToggle,
                    onSaveClick = remindersViewModel::saveReminder
                )
            }

            composable(
                route = Screen.ReminderDetail.route,
                arguments = listOf(
                    navArgument(Screen.ReminderDetail.ARG_REMINDER_ID) {
                        type = NavType.LongType
                    }
                )
            ) { entry ->
                val reminderId = entry.arguments?.getLong(Screen.ReminderDetail.ARG_REMINDER_ID) ?: -1L
                val reminder by remember(reminderId) {
                    remindersViewModel.observeReminder(reminderId)
                }.collectAsStateWithLifecycle(initialValue = null)

                LaunchedEffect(Unit) {
                    remindersViewModel.events.collect { event ->
                        when (event) {
                            is RemindersEvent.Feedback -> showFeedbackSnackbar(event.message)
                            is RemindersEvent.DeletedSuccess -> navController.popBackStack()
                            else -> Unit
                        }
                    }
                }

                ReminderDetailScreen(
                    reminder = reminder,
                    onBackClick = { navController.popBackStack() },
                    onToggleComplete = remindersViewModel::toggleComplete,
                    onSnoozeClick = remindersViewModel::snoozeReminder,
                    onEditClick = { id ->
                        navController.navigate(Screen.AddEditReminder.createRoute(id))
                    },
                    onDeleteConfirm = remindersViewModel::deleteReminder
                )
            }

            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    uiState = notificationsState,
                    onBackClick = { navController.popBackStack() },
                    onMeClick = { navController.navigate(Screen.Me.route) },
                    onMarkAllReadClick = notificationsViewModel::markAllAsRead
                )
            }

            composable(Screen.Me.route) {
                MeScreen(
                    currentStreakDays = homeUiState.currentStreakDays,
                    unreadNotificationsCount = unreadCount,
                    prefs = prefsState,
                    isDarkTheme = isDarkTheme,
                    onBackClick = { navController.popBackStack() },
                    onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                    onViewProgressClick = {
                        navController.navigate(Screen.Stats.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onMySchedulesClick = {
                        navController.navigate(Screen.Subjects.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onDailyReminderToggle = userPreferencesManager::setDailyReminderEnabled,
                    onReminderTimeChanged = userPreferencesManager::setDailyReminderTimeMinutes,
                    onTimerSoundToggle = userPreferencesManager::setTimerCompletionSound,
                    onToggleWeekStart = {
                        val next = if (prefsState.weekStartsOn == "Monday") "Sunday" else "Monday"
                        userPreferencesManager.setWeekStartsOn(next)
                    },
                    onToggleDarkTheme = { onToggleTheme() }
                )
            }

            composable(Screen.Stats.route) {
                val statsViewModel: StatsViewModel = viewModel(
                    factory = AppViewModelFactory(repository)
                )
                val uiState by statsViewModel.uiState.collectAsStateWithLifecycle()

                StatsScreen(
                    uiState = uiState,
                    unreadNotificationsCount = unreadCount,
                    onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                    onMeClick = { navController.navigate(Screen.Me.route) },
                    onLogSessionClick = {
                        navController.navigate(Screen.AddEditSession.createRoute())
                    }
                )
            }

            composable(
                route = Screen.AddEditSession.route,
                arguments = listOf(
                    navArgument(Screen.AddEditSession.ARG_SESSION_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument(Screen.AddEditSession.ARG_SUBJECT_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { entry ->
                val sessionId = entry.arguments?.getLong(Screen.AddEditSession.ARG_SESSION_ID) ?: -1L
                val subjectId = entry.arguments?.getLong(Screen.AddEditSession.ARG_SUBJECT_ID) ?: -1L
                val addEditViewModel: AddEditSessionViewModel = viewModel(
                    factory = AppViewModelFactory(
                        repository = repository,
                        sessionId = sessionId,
                        initialSubjectId = subjectId
                    )
                )
                val uiState by addEditViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    addEditViewModel.events.collect { event ->
                        when (event) {
                            is AddEditSessionEvent.SavedSuccess -> {
                                showFeedbackSnackbar(event.message)
                                navController.popBackStack()
                            }
                            is AddEditSessionEvent.ErrorMessage -> {
                                showFeedbackSnackbar(event.message)
                            }
                        }
                    }
                }

                AddEditSessionScreen(
                    uiState = uiState,
                    onBackClick = { navController.popBackStack() },
                    onSubjectSelected = addEditViewModel::onSubjectSelected,
                    onDurationChanged = addEditViewModel::onDurationChanged,
                    onDateSelected = addEditViewModel::onDateSelected,
                    onNotesChanged = addEditViewModel::onNotesChanged,
                    onSaveClick = addEditViewModel::saveSession,
                    onManageSubjectsClick = {
                        navController.navigate(Screen.Subjects.route) {
                            popUpTo(Screen.Home.route)
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = Screen.SessionDetail.route,
                arguments = listOf(
                    navArgument(Screen.SessionDetail.ARG_SESSION_ID) {
                        type = NavType.LongType
                    }
                )
            ) { entry ->
                val sessionId = entry.arguments?.getLong(Screen.SessionDetail.ARG_SESSION_ID) ?: -1L
                val detailViewModel: SessionDetailViewModel = viewModel(
                    factory = AppViewModelFactory(repository = repository, sessionId = sessionId)
                )
                val uiState by detailViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    detailViewModel.events.collect { event ->
                        when (event) {
                            is SessionDetailEvent.Deleted -> {
                                showFeedbackSnackbar(event.message)
                                navController.popBackStack()
                            }
                            is SessionDetailEvent.Error -> {
                                showFeedbackSnackbar(event.message)
                            }
                        }
                    }
                }

                SessionDetailScreen(
                    uiState = uiState,
                    onEditClick = { id ->
                        navController.navigate(Screen.AddEditSession.createRoute(sessionId = id))
                    },
                    onDeleteConfirm = detailViewModel::deleteSession,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
