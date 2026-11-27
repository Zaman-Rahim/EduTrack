package com.example.edutrack.util

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper

enum class SystemToneFilter(val label: String) {
    ALL("All"),
    ALARM("Alarms"),
    RINGTONE("Ringtones"),
    NOTIFICATION("Messages")
}

data class SystemSoundItem(
    val title: String,
    val uriString: String,
    val filter: SystemToneFilter
)

/**
 * Discovers installed Alarm, Ringtone, and Notification/Message tones from the Android device
 * via [RingtoneManager] and handles live audio previews without needing storage permissions.
 */
object SystemSoundHelper {
    private const val PREVIEW_AUTO_STOP_MS = 6_000L

    private var previewRingtone: Ringtone? = null
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val autoStopRunnable = Runnable { stopPreview() }

    fun loadDeviceSounds(context: Context): List<SystemSoundItem> {
        val result = mutableListOf<SystemSoundItem>()
        val seenUris = HashSet<String>()

        // Default system entries first
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.let { uri ->
            val key = uri.toString()
            if (seenUris.add(key)) {
                result += SystemSoundItem("Default Alarm", key, SystemToneFilter.ALARM)
            }
        }
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.let { uri ->
            val key = uri.toString()
            if (seenUris.add(key)) {
                result += SystemSoundItem("Default Notification", key, SystemToneFilter.NOTIFICATION)
            }
        }
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)?.let { uri ->
            val key = uri.toString()
            if (seenUris.add(key)) {
                result += SystemSoundItem("Default Ringtone", key, SystemToneFilter.RINGTONE)
            }
        }

        val queryTypes = listOf(
            RingtoneManager.TYPE_ALARM to SystemToneFilter.ALARM,
            RingtoneManager.TYPE_NOTIFICATION to SystemToneFilter.NOTIFICATION,
            RingtoneManager.TYPE_RINGTONE to SystemToneFilter.RINGTONE
        )

        for ((type, filter) in queryTypes) {
            runCatching {
                val manager = RingtoneManager(context).apply { setType(type) }
                val cursor = manager.cursor
                if (cursor != null) {
                    try {
                        var count = 0
                        while (cursor.moveToNext() && count < 40) {
                            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: continue
                            val uri = manager.getRingtoneUri(cursor.position)?.toString() ?: continue
                            if (seenUris.add(uri)) {
                                result += SystemSoundItem(title = title, uriString = uri, filter = filter)
                                count++
                            }
                        }
                    } finally {
                        runCatching { cursor.close() }
                    }
                }
            }
        }

        return result
    }

    fun resolveUri(uriString: String, fallbackType: Int): Uri? {
        return if (uriString.isNotBlank()) {
            runCatching { Uri.parse(uriString) }.getOrNull()
        } else {
            RingtoneManager.getDefaultUri(fallbackType)
        }
    }

    fun playPreview(context: Context, uriString: String, useAlarmUsage: Boolean = false) {
        stopPreview()
        val fallbackType = if (useAlarmUsage) RingtoneManager.TYPE_ALARM else RingtoneManager.TYPE_NOTIFICATION
        val uri = resolveUri(uriString, fallbackType) ?: return
        runCatching {
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri) ?: return
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(if (useAlarmUsage) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = false
            }
            ringtone.play()
            previewRingtone = ringtone
            mainHandler.postDelayed(autoStopRunnable, PREVIEW_AUTO_STOP_MS)
        }
    }

    fun stopPreview() {
        mainHandler.removeCallbacks(autoStopRunnable)
        runCatching {
            previewRingtone?.stop()
        }
        previewRingtone = null
    }
}
