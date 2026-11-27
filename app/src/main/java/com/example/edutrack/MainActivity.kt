package com.example.edutrack

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.edutrack.ui.navigation.EduTrackNavHost
import com.example.edutrack.ui.theme.EduTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestHighestRefreshRate()
        enableEdgeToEdge()
        val container = (application as EduTrackApplication).container
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by rememberSaveable { mutableStateOf(systemDark) }

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
                        onToggleTheme = { isDarkTheme = !isDarkTheme }
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun requestHighestRefreshRate() {
        val currentDisplay: Display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display ?: windowManager.defaultDisplay
        } else {
            windowManager.defaultDisplay
        } ?: return

        val bestMode = currentDisplay.supportedModes
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
}

