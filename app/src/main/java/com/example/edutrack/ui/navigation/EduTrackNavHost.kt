package com.example.edutrack.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.edutrack.AppContainer
import com.example.edutrack.R
import com.example.edutrack.ui.components.ClayBottomNavBar
import com.example.edutrack.ui.components.ClayNavTab
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

private const val MAIN_TABS_ROUTE = "main_tabs"

private val bottomNavTabs = listOf(
    ClayNavTab(
        label = "Home",
        iconRes = R.drawable.ic_figma_home
    ),
    ClayNavTab(
        label = "History",
        iconRes = R.drawable.ic_figma_history
    ),
    ClayNavTab(
        label = "Subjects",
        iconRes = R.drawable.ic_figma_book
    ),
    ClayNavTab(
        label = "Reminders",
        iconRes = R.drawable.ic_figma_reminder
    ),
    ClayNavTab(
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
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { bottomNavTabs.size })

    fun showFeedbackSnackbar(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    fun navigateToMainTab(pageIndex: Int) {
        navController.popBackStack(MAIN_TABS_ROUTE, inclusive = false)
        scope.launch {
            pagerState.animateScrollToPage(pageIndex)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route.orEmpty()

    val isTopLevelScreen = currentRoute == MAIN_TABS_ROUTE

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
                    enter = slideInVertically(
                        initialOffsetY = { -it / 2 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(),
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
                    ClayBottomNavBar(
                        tabs = bottomNavTabs,
                        selectedIndex = pagerState.currentPage,
                        pageOffsetProvider = { pagerState.currentPageOffsetFraction },
                        isDarkTheme = isDarkTheme,
                        onTabSelected = { index ->
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MAIN_TABS_ROUTE,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MAIN_TABS_ROUTE) {
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = AppViewModelFactory(repository)
                )
                val statsViewModel: StatsViewModel = viewModel(
                    factory = AppViewModelFactory(repository)
                )

                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 2,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> {
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
                                    historyViewModel.selectSubjectFilter(subjectId)
                                    scope.launch {
                                        pagerState.animateScrollToPage(1)
                                    }
                                },
                                onSessionClick = { sessionId ->
                                    navController.navigate(Screen.SessionDetail.createRoute(sessionId))
                                },
                                onManageSubjectsClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(2)
                                    }
                                },
                                onViewAllHistoryClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(1)
                                    }
                                }
                            )
                        }

                        1 -> {
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

                        2 -> {
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

                        3 -> {
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

                        4 -> {
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
                    }
                }
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

            composable(
                route = Screen.FocusTimer.route,
                enterTransition = {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(340)
                    ) + fadeIn(tween(260))
                },
                exitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(380)
                    ) + fadeOut(tween(300))
                },
                popExitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(380)
                    ) + fadeOut(tween(300))
                }
            ) {
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
                    },
                    onStopAlarmClick = focusTimerManager::stopAlarm
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
                        navigateToMainTab(4)
                    },
                    onMySchedulesClick = {
                        navigateToMainTab(2)
                    },
                    onDailyReminderToggle = userPreferencesManager::setDailyReminderEnabled,
                    onReminderTimeChanged = userPreferencesManager::setDailyReminderTimeMinutes,
                    onTimerSoundToggle = userPreferencesManager::setTimerCompletionSound,
                    onToggleWeekStart = {
                        val next = if (prefsState.weekStartsOn == "Monday") "Sunday" else "Monday"
                        userPreferencesManager.setWeekStartsOn(next)
                    },
                    onToggleDarkTheme = { onToggleTheme() },
                    onAlarmToneSelected = { tone -> userPreferencesManager.setAlarmTone(tone.name) },
                    onPreviewAlarmTone = focusTimerManager::previewAlarmTone
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
                        navigateToMainTab(2)
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
