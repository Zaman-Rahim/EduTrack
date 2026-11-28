package com.example.edutrack.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.Alignment
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
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
import com.example.edutrack.data.local.entity.SubjectEntity
import com.example.edutrack.notifications.NotificationHelper
import com.example.edutrack.notifications.ReminderAlarmScheduler
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
import com.example.edutrack.ui.timer.FocusTimerManager
import com.example.edutrack.ui.timer.FocusTimerUiState
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
import com.example.edutrack.util.HomeMessageAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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

private fun shouldShowLiveBar(state: FocusTimerUiState, route: String): Boolean =
    state.isActive &&
        state.isMinimized &&
        !state.isBarHidden &&
        route != Screen.Me.route &&
        !route.startsWith("add_edit_subject")

@Composable
fun EduTrackNavHost(
    container: AppContainer,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
    openTargetFlow: StateFlow<String?> = MutableStateFlow(null),
    onOpenTargetConsumed: () -> Unit = {}
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

    fun navigateSingleTop(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    fun navigateUtilityRoute(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            popUpTo(MAIN_TABS_ROUTE) {
                inclusive = false
            }
        }
    }

    fun popSafely() {
        val currentEntry = navController.currentBackStackEntry
        if (currentEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true) {
            navController.popBackStack()
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

    val appContext = LocalContext.current.applicationContext
    var blockedSubjectTarget by remember { mutableStateOf<SubjectEntity?>(null) }

    val homeViewModel: HomeViewModel = viewModel(
        factory = AppViewModelFactory(repository, preferencesManager = userPreferencesManager)
    )
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    val subjectManagerViewModel: SubjectManagerViewModel = viewModel(
        factory = AppViewModelFactory(repository)
    )
    val remindersViewModel: RemindersViewModel = viewModel(
        factory = AppViewModelFactory(repository, appContext = appContext)
    )
    val notificationsViewModel: NotificationsViewModel = viewModel(
        factory = AppViewModelFactory(repository)
    )

    val notificationsState by notificationsViewModel.uiState.collectAsStateWithLifecycle()
    val prefsState by userPreferencesManager.state.collectAsStateWithLifecycle()
    val timerStatus by remember(focusTimerManager) {
        focusTimerManager.state.map { it.status }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = focusTimerManager.state.value.status)
    val isTimerExpanded by remember(focusTimerManager) {
        focusTimerManager.state.map { it.isActive && !it.isMinimized }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(
        initialValue = focusTimerManager.state.value.let { it.isActive && !it.isMinimized }
    )

    val openTarget by openTargetFlow.collectAsStateWithLifecycle()
    LaunchedEffect(openTarget) {
        when (openTarget) {
            NotificationHelper.OPEN_TARGET_FOCUS_TIMER -> {
                if (focusTimerManager.state.value.isActive) {
                    focusTimerManager.expand()
                }
                onOpenTargetConsumed()
            }
            NotificationHelper.OPEN_TARGET_REMINDERS -> {
                navigateToMainTab(3)
                onOpenTargetConsumed()
            }
        }
    }

    val unreadCount = notificationsState.unreadCount
    val showLiveBarInitial = remember(currentRoute) {
        shouldShowLiveBar(focusTimerManager.state.value, currentRoute)
    }
    val showLiveBar by remember(focusTimerManager, currentRoute) {
        focusTimerManager.state.map { state ->
            shouldShowLiveBar(state, currentRoute)
        }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = showLiveBarInitial)

    LaunchedEffect(pagerState.currentPage) {
        val state = focusTimerManager.state.value
        if (
            pagerState.currentPage == 2 &&
            state.isActive &&
            state.isMinimized &&
            state.isBarHidden
        ) {
            focusTimerManager.unhideLiveBar()
        }
    }

    val snappyEasing = remember { CubicBezierEasing(0.22f, 1f, 0.36f, 1f) }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.statusBars,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = MAIN_TABS_ROUTE,
                modifier = Modifier
                    .padding(innerPadding)
                    .then(if (!isTopLevelScreen) Modifier.navigationBarsPadding() else Modifier),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(durationMillis = 260, easing = snappyEasing)
                    ) + fadeIn(animationSpec = tween(220))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 4 },
                        animationSpec = tween(durationMillis = 260, easing = snappyEasing)
                    ) + fadeOut(animationSpec = tween(200))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it / 4 },
                        animationSpec = tween(durationMillis = 260, easing = snappyEasing)
                    ) + fadeIn(animationSpec = tween(220))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(durationMillis = 260, easing = snappyEasing)
                    ) + fadeOut(animationSpec = tween(200))
                }
            ) {
                composable(MAIN_TABS_ROUTE) {
                    val historyViewModel: HistoryViewModel = viewModel(
                        factory = AppViewModelFactory(repository, preferencesManager = userPreferencesManager)
                    )
                    val statsViewModel: StatsViewModel = viewModel(
                        factory = AppViewModelFactory(repository, preferencesManager = userPreferencesManager)
                    )

                    BackHandler(enabled = !isTimerExpanded && pagerState.currentPage != 0) {
                        scope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pagerState,
                            beyondViewportPageCount = 1,
                            modifier = Modifier.weight(1f)
                        ) { page ->
                    when (page) {
                        0 -> {
                            HomeScreen(
                                uiState = homeUiState,
                                unreadNotificationsCount = unreadCount,
                                onNotificationsClick = { navigateUtilityRoute(Screen.Notifications.route) },
                                onMeClick = { navigateUtilityRoute(Screen.Me.route) },
                                onAddFirstSubjectClick = {
                                    navigateSingleTop(Screen.AddEditSubject.createRoute())
                                },
                                onAddSessionClick = {
                                    navigateSingleTop(Screen.AddEditSession.createRoute())
                                },
                                onQuickAddForSubject = { subjectId ->
                                    navigateSingleTop(
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
                                    navigateSingleTop(Screen.SessionDetail.createRoute(sessionId))
                                },
                                onManageSubjectsClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(2)
                                    }
                                },
                                onDynamicActionClick = { action ->
                                    val targetPage = when (action) {
                                        HomeMessageAction.OPEN_REMINDERS -> 3
                                        HomeMessageAction.OPEN_SUBJECTS -> 2
                                        HomeMessageAction.OPEN_PROGRESS -> 4
                                    }
                                    scope.launch {
                                        pagerState.animateScrollToPage(targetPage)
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
                                onNotificationsClick = { navigateUtilityRoute(Screen.Notifications.route) },
                                onMeClick = { navigateUtilityRoute(Screen.Me.route) },
                                onSearchQueryChanged = historyViewModel::onSearchQueryChanged,
                                onSelectSubjectFilter = historyViewModel::selectSubjectFilter,
                                onSelectDateFilter = historyViewModel::selectDateFilter,
                                onClearFilters = historyViewModel::clearFilters,
                                onSessionClick = { sessionId ->
                                    navigateSingleTop(Screen.SessionDetail.createRoute(sessionId))
                                },
                                onLogSessionClick = {
                                    navigateSingleTop(Screen.AddEditSession.createRoute())
                                }
                            )
                        }

                        2 -> {
                            val uiState by subjectManagerViewModel.uiState.collectAsStateWithLifecycle()

                            SubjectManagerScreen(
                                uiState = uiState,
                                unreadNotificationsCount = unreadCount,
                                onNotificationsClick = { navigateUtilityRoute(Screen.Notifications.route) },
                                onMeClick = { navigateUtilityRoute(Screen.Me.route) },
                                onAddClick = {
                                    navigateSingleTop(Screen.AddEditSubject.createRoute())
                                },
                                onEditScheduleClick = { subjectId ->
                                    navigateSingleTop(Screen.AddEditSubject.createRoute(subjectId))
                                },
                                onStartFocusClick = { subject ->
                                    if (focusTimerManager.isAnotherSubjectRunning(subject.id)) {
                                        blockedSubjectTarget = subject
                                    } else {
                                        val currentState = focusTimerManager.state.value
                                        if (currentState.subject?.id == subject.id && currentState.isActive) {
                                            focusTimerManager.expand()
                                        } else {
                                            focusTimerManager.startFocus(subject)
                                        }
                                    }
                                }
                            )
                        }

                        3 -> {
                            val uiState by remindersViewModel.uiState.collectAsStateWithLifecycle()

                            LaunchedEffect(Unit) {
                                remindersViewModel.listEvents.collect { event ->
                                    if (event is RemindersEvent.Feedback) {
                                        showFeedbackSnackbar(event.message)
                                    }
                                }
                            }

                            RemindersScreen(
                                uiState = uiState,
                                unreadNotificationsCount = unreadCount,
                                onNotificationsClick = { navigateUtilityRoute(Screen.Notifications.route) },
                                onMeClick = { navigateUtilityRoute(Screen.Me.route) },
                                onAddReminderClick = {
                                    navigateSingleTop(Screen.AddEditReminder.createRoute())
                                },
                                onSelectTab = remindersViewModel::selectTab,
                                onToggleComplete = remindersViewModel::toggleComplete,
                                onReminderClick = { reminderId ->
                                    navigateSingleTop(Screen.ReminderDetail.createRoute(reminderId))
                                }
                            )
                        }

                        4 -> {
                            val uiState by statsViewModel.uiState.collectAsStateWithLifecycle()

                            StatsScreen(
                                uiState = uiState,
                                unreadNotificationsCount = unreadCount,
                                onNotificationsClick = { navigateUtilityRoute(Screen.Notifications.route) },
                                onMeClick = { navigateUtilityRoute(Screen.Me.route) },
                                onLogSessionClick = {
                                    navigateSingleTop(Screen.AddEditSession.createRoute())
                                },
                                onPrevWeekClick = { statsViewModel.shiftWeek(-1) },
                                onNextWeekClick = { statsViewModel.shiftWeek(1) }
                            )
                        }
                    }
                    }

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

                LaunchedEffect(entry) {
                    subjectManagerViewModel.events.collect { event ->
                        when (event) {
                            is SubjectManagerEvent.Feedback -> showFeedbackSnackbar(event.message)
                            is SubjectManagerEvent.SavedSuccess -> popSafely()
                            is SubjectManagerEvent.DeletedSuccess -> {
                                showFeedbackSnackbar(event.message)
                                popSafely()
                            }
                            is SubjectManagerEvent.ErrorMessage -> showFeedbackSnackbar(event.message)
                        }
                    }
                }

                val editingSubject = uiState.allSubjects
                    .firstOrNull { it.subject.id == subjectId }
                    ?.subject

                AddEditSubjectScheduleScreen(
                    formState = uiState.formState,
                    editingSubject = editingSubject,
                    onBackClick = { popSafely() },
                    onNameChanged = subjectManagerViewModel::onNameChanged,
                    onDailyHoursChanged = subjectManagerViewModel::onDailyHoursChanged,
                    onDailyMinutesChanged = subjectManagerViewModel::onDailyMinutesChanged,
                    onStartDateSelected = subjectManagerViewModel::onStartDateSelected,
                    onRepeatModeSelected = subjectManagerViewModel::onRepeatModeSelected,
                    onToggleDayBit = subjectManagerViewModel::onToggleDayBit,
                    onColorSelected = subjectManagerViewModel::onColorSelected,
                    onSaveClick = subjectManagerViewModel::saveSubject,
                    onDeleteClick = { subject ->
                        focusTimerManager.dismissIfSubjectDeleted(subject.id)
                        subjectManagerViewModel.deleteSubject(subject)
                    },
                    weekStartsOn = prefsState.weekStartsOn
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

                LaunchedEffect(entry) {
                    remindersViewModel.editorEvents.collect { event ->
                        when (event) {
                            is RemindersEvent.Feedback -> showFeedbackSnackbar(event.message)
                            is RemindersEvent.SavedSuccess -> popSafely()
                            else -> Unit
                        }
                    }
                }

                AddEditReminderScreen(
                    state = uiState.editorState,
                    onBackClick = { popSafely() },
                    onTitleChanged = remindersViewModel::onTitleChanged,
                    onNotesChanged = remindersViewModel::onNotesChanged,
                    onDueDateSelected = remindersViewModel::onDueDateSelected,
                    onDueTimeSelected = remindersViewModel::onDueTimeSelected,
                    onRepeatSelected = remindersViewModel::onRepeatTypeSelected,
                    onToggleRepeatDayBit = remindersViewModel::onToggleRepeatDayBit,
                    onRepeatDaysMaskChanged = remindersViewModel::onRepeatDaysMaskChanged,
                    onPrioritySelected = remindersViewModel::onPrioritySelected,
                    onNotifyToggle = remindersViewModel::onNotifyToggle,
                    onDismissPastTimeError = remindersViewModel::dismissPastTimeError,
                    onSaveClick = remindersViewModel::saveReminder,
                    weekStartsOn = prefsState.weekStartsOn
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
                val reminderState by remember(reminderId) {
                    remindersViewModel.observeReminder(reminderId).map<_, Pair<Boolean, com.example.edutrack.data.local.entity.ReminderEntity?>> {
                        false to it
                    }
                }.collectAsStateWithLifecycle(initialValue = true to null)
                val (isLoadingReminder, reminder) = reminderState

                LaunchedEffect(entry) {
                    remindersViewModel.detailEvents.collect { event ->
                        when (event) {
                            is RemindersEvent.Feedback -> showFeedbackSnackbar(event.message)
                            is RemindersEvent.DeletedSuccess -> popSafely()
                            else -> Unit
                        }
                    }
                }

                ReminderDetailScreen(
                    reminder = reminder,
                    isLoading = isLoadingReminder,
                    onBackClick = { popSafely() },
                    onToggleComplete = remindersViewModel::toggleComplete,
                    onSnoozeClick = remindersViewModel::snoozeReminder,
                    onSnoozeTomorrowClick = remindersViewModel::snoozeReminderToTomorrowMorning,
                    onEditClick = { id ->
                        navigateSingleTop(Screen.AddEditReminder.createRoute(id))
                    },
                    onDeleteConfirm = remindersViewModel::deleteReminder,
                    onPreviewAlertClick = {
                        if (NotificationHelper.hasNotificationPermission(appContext)) {
                            NotificationHelper.showReminderNotification(
                                context = appContext,
                                notificationId = 9999,
                                title = reminder?.title ?: "Reminder preview",
                                body = "This is how your scheduled reminder notification will appear.",
                                toneUriString = prefsState.reminderToneUri
                            )
                            showFeedbackSnackbar("Preview reminder alert sent")
                        } else {
                            showFeedbackSnackbar("Enable notifications in Android Settings to preview alerts")
                        }
                    }
                )
            }

            composable(Screen.Notifications.route) { entry ->
                LaunchedEffect(entry) {
                    notificationsViewModel.events.collect { message ->
                        showFeedbackSnackbar(message)
                    }
                }
                NotificationsScreen(
                    uiState = notificationsState,
                    onBackClick = { popSafely() },
                    onMeClick = { navigateUtilityRoute(Screen.Me.route) },
                    onMarkAllReadClick = notificationsViewModel::markAllAsRead,
                    onClearAllClick = notificationsViewModel::clearAll
                )
            }

            composable(Screen.Me.route) {
                MeScreen(
                    currentStreakDays = homeUiState.currentStreakDays,
                    unreadNotificationsCount = unreadCount,
                    prefs = prefsState,
                    isDarkTheme = isDarkTheme,
                    onBackClick = { popSafely() },
                    onNotificationsClick = { navigateUtilityRoute(Screen.Notifications.route) },
                    onViewProgressClick = { navigateToMainTab(4) },
                    onMySchedulesClick = { navigateToMainTab(2) },
                    onDailyReminderToggle = { enabled ->
                        userPreferencesManager.setDailyReminderEnabled(enabled)
                        ReminderAlarmScheduler.scheduleDailyStudyReminder(
                            context = appContext,
                            enabled = enabled,
                            timeMinutes = prefsState.dailyReminderTimeMinutes
                        )
                    },
                    onReminderTimeChanged = { mins ->
                        userPreferencesManager.setDailyReminderTimeMinutes(mins)
                        ReminderAlarmScheduler.scheduleDailyStudyReminder(
                            context = appContext,
                            enabled = prefsState.dailyReminderEnabled,
                            timeMinutes = mins
                        )
                    },
                    onTimerSoundToggle = userPreferencesManager::setTimerCompletionSound,
                    onToggleWeekStart = {
                        val next = if (prefsState.weekStartsOn == "Monday") "Sunday" else "Monday"
                        userPreferencesManager.setWeekStartsOn(next)
                    },
                    onToggleDarkTheme = { onToggleTheme() },
                    onTimerToneSelected = userPreferencesManager::setTimerTone,
                    onReminderToneSelected = userPreferencesManager::setReminderTone,
                    onNotificationToneSelected = userPreferencesManager::setNotificationTone
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

                LaunchedEffect(entry) {
                    addEditViewModel.events.collect { event ->
                        when (event) {
                            is AddEditSessionEvent.SavedSuccess -> {
                                showFeedbackSnackbar(event.message)
                                popSafely()
                            }
                            is AddEditSessionEvent.ErrorMessage -> {
                                showFeedbackSnackbar(event.message)
                            }
                        }
                    }
                }

                AddEditSessionScreen(
                    uiState = uiState,
                    onBackClick = { popSafely() },
                    onSubjectSelected = addEditViewModel::onSubjectSelected,
                    onDurationChanged = addEditViewModel::onDurationChanged,
                    onDateSelected = addEditViewModel::onDateSelected,
                    onNotesChanged = addEditViewModel::onNotesChanged,
                    onSaveClick = addEditViewModel::saveSession,
                    onManageSubjectsClick = { navigateToMainTab(2) }
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

                LaunchedEffect(entry) {
                    detailViewModel.events.collect { event ->
                        when (event) {
                            is SessionDetailEvent.Deleted -> {
                                showFeedbackSnackbar(event.message)
                                popSafely()
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
                        navigateSingleTop(Screen.AddEditSession.createRoute(sessionId = id))
                    },
                    onDeleteConfirm = detailViewModel::deleteSession,
                    onBackClick = { popSafely() }
                )
            }
        }
    }

        // Overlay LiveFocusBar without triggering Scaffold innerPadding relayout
        AnimatedVisibility(
            visible = showLiveBar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(
                    if (isTopLevelScreen) {
                        Modifier.navigationBarsPadding().padding(bottom = 78.dp)
                    } else {
                        Modifier.navigationBarsPadding()
                    }
                ),
            enter = slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
        ) {
            val liveBarTimerState by focusTimerManager.state.collectAsStateWithLifecycle()
            LiveFocusBar(
                timerState = liveBarTimerState,
                onExpandClick = {
                    focusTimerManager.expand()
                },
                onPauseResumeOrDismissClick = {
                    if (liveBarTimerState.status == TimerStatus.COMPLETED) {
                        focusTimerManager.dismissOrCancel()
                    } else {
                        focusTimerManager.togglePauseResume()
                    }
                },
                onSwipeDismiss = { focusTimerManager.hideLiveBar() }
            )
        }

        // Full-screen vertical overlay for Focus Timer & Focus Complete
        FocusTimerOverlayHost(
            focusTimerManager = focusTimerManager,
            isVisible = isTimerExpanded,
            snappyEasing = snappyEasing,
            onOpenNotificationsClick = { navigateUtilityRoute(Screen.Notifications.route) }
        )
    }

    // Automatically bring the Focus Complete overlay up from any screen when timer hits 00:00
    LaunchedEffect(timerStatus) {
        if (timerStatus == TimerStatus.COMPLETED) {
            focusTimerManager.expand()
        }
    }

    blockedSubjectTarget?.let { targetSubject ->
        val activeSubjectName = focusTimerManager.state.value.subject?.name ?: "your current subject"
        AlertDialog(
            onDismissRequest = { blockedSubjectTarget = null },
            title = {
                Text(
                    text = "\"$activeSubjectName\" is running",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "You are currently studying \"$activeSubjectName\". Please pause it, finish it, or choose \"End without completing\" before starting \"${targetSubject.name}\"."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        blockedSubjectTarget = null
                        focusTimerManager.expand()
                    }
                ) {
                    Text(
                        text = "Open $activeSubjectName",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { blockedSubjectTarget = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun FocusTimerOverlayHost(
    focusTimerManager: FocusTimerManager,
    isVisible: Boolean,
    snappyEasing: CubicBezierEasing,
    onOpenNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val liveTimerState by focusTimerManager.state.collectAsStateWithLifecycle()
    var retainedState by remember { mutableStateOf(liveTimerState) }
    if (liveTimerState.isActive) {
        retainedState = liveTimerState
    }

    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier.fillMaxSize(),
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 560, easing = snappyEasing)
        ) + fadeIn(animationSpec = tween(durationMillis = 360)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 580, easing = snappyEasing)
        ) + fadeOut(animationSpec = tween(durationMillis = 360))
    ) {
        FocusTimerScreen(
            timerState = if (liveTimerState.isActive) liveTimerState else retainedState,
            isBackEnabled = isVisible,
            onMinimizeClick = focusTimerManager::minimize,
            onPauseResumeClick = focusTimerManager::togglePauseResume,
            onEndWithoutCompletingClick = focusTimerManager::completeNowManually,
            onOpenNotificationsClick = {
                focusTimerManager.dismissOrCancel()
                onOpenNotificationsClick()
            },
            onBackToPlanClick = focusTimerManager::dismissOrCancel,
            onStopAlarmClick = focusTimerManager::stopAlarm
        )
    }
}
