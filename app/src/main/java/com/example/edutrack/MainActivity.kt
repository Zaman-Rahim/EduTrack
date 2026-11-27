package com.example.edutrack

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.edutrack.notifications.NotificationHelper
import com.example.edutrack.ui.navigation.EduTrackNavHost
import com.example.edutrack.ui.theme.EduTrackTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val pendingOpenTarget = MutableStateFlow<String?>(null)

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled gracefully by NotificationHelper */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestHighestRefreshRate()
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()

        pendingOpenTarget.value = intent?.getStringExtra(NotificationHelper.EXTRA_OPEN_TARGET)
        val container = (application as EduTrackApplication).container

        setContent {
            val prefsState by container.userPreferences.state.collectAsStateWithLifecycle()
            val isDarkTheme = prefsState.isDarkTheme

            DisposableEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            EduTrackTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EduTrackNavHost(
                        container = container,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = {
                            container.userPreferences.setDarkTheme(!isDarkTheme)
                        },
                        openTargetFlow = pendingOpenTarget,
                        onOpenTargetConsumed = { pendingOpenTarget.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val target = intent.getStringExtra(NotificationHelper.EXTRA_OPEN_TARGET)
        if (!target.isNullOrBlank()) {
            pendingOpenTarget.value = target
        }
    }

    override fun onResume() {
        super.onResume()
        (application as? EduTrackApplication)?.container?.focusTimerManager?.completeNowIfDue()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            requestHighestRefreshRate()
        }
    }

    @Suppress("DEPRECATION")
    private fun requestHighestRefreshRate() {
        val currentDisplay: Display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display ?: windowManager.defaultDisplay
        } else {
            windowManager.defaultDisplay
        } ?: return

        val activeMode = currentDisplay.mode ?: return
        val bestMode = currentDisplay.supportedModes
            .filter {
                it.physicalWidth == activeMode.physicalWidth &&
                    it.physicalHeight == activeMode.physicalHeight
            }
            .maxByOrNull { it.refreshRate } ?: return

        val params = window.attributes
        var changed = false
        if (params.preferredDisplayModeId != bestMode.modeId) {
            params.preferredDisplayModeId = bestMode.modeId
            changed = true
        }
        if (params.preferredRefreshRate != bestMode.refreshRate) {
            params.preferredRefreshRate = bestMode.refreshRate
            changed = true
        }
        if (changed) {
            window.attributes = params
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
