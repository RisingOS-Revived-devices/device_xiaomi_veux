/*
 * Copyright (C) 2026 acerhizmq
 * Useless mode: Copyright (C) 2025 KamiKaonashi
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.ui.screens

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.lineageos.settings.R
import org.lineageos.settings.display.FpsInfoService
import org.lineageos.settings.ui.components.MainSwitchCard
import org.lineageos.settings.ui.components.NoteText
import org.lineageos.settings.ui.components.PartsPage
import org.lineageos.settings.ui.components.WarningCard
import java.io.File
import kotlin.random.Random

@Composable
fun FpsOverlayScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val supported = remember { File(FpsInfoService.FPS_PATH).exists() }
    var running by remember { mutableStateOf(prefs.getBoolean(FpsInfoService.PREF_KEY_FPS_STATE, false)) }
    PartsPage(stringResource(R.string.fps_overlay_title), onBack) {
        item {
            MainSwitchCard(stringResource(R.string.fps_overlay_switch), running && supported, enabled = supported) {
                setFpsOverlay(context, it)
                running = it
            }
        }
        item { NoteText(stringResource(if (supported) R.string.fps_overlay_note else R.string.fps_overlay_not_supported)) }
    }
}

const val USELESS_PREF = "useless_mode"
private const val SOUND_PROBABILITY = 0.15f

// Vibration patterns (delay, vibrate, pause, vibrate...)
private val VibrationPatterns = arrayOf(
    longArrayOf(0, 100, 100, 100),
    longArrayOf(0, 200, 50, 200, 50, 200),
    longArrayOf(0, 500),
    longArrayOf(0, 50, 50, 50, 50, 50),
    longArrayOf(0, 100, 200, 300, 100, 100),
    longArrayOf(0, 800),
    longArrayOf(0, 25, 25, 25, 25, 25, 25, 25),
)

/** The pointless effects of useless mode, ported from the original fragment. */
private class UselessEffects(private val context: Context, private val scope: CoroutineScope) {
    private val vibrator = context.getSystemService(Vibrator::class.java)
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var mediaPlayer: MediaPlayer? = null
    private var originalVolume = 0

    private val canVibrate get() = vibrator != null && vibrator.hasVibrator()

    fun vibrate(pattern: LongArray) {
        if (canVibrate) vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    fun vibrate(millis: Long) {
        if (canVibrate) vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun randomEffects() {
        if (Random.nextFloat() < 0.7f) vibrate(VibrationPatterns.random())
        if (Random.nextFloat() < 0.5f) flicker()
        playRandomSound()
    }

    fun flicker() {
        val window = (context as? Activity)?.window ?: return
        scope.launch {
            val params = window.attributes
            val original = params.screenBrightness
            try {
                repeat(3) {
                    params.screenBrightness = 0.1f
                    window.attributes = params
                    delay(100)
                    params.screenBrightness = 1.0f
                    window.attributes = params
                    delay(100)
                }
            } finally {
                params.screenBrightness = original
                window.attributes = params
            }
        }
    }

    private fun playRandomSound() {
        if (Random.nextFloat() >= SOUND_PROBABILITY) return
        try {
            release()
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
            val player = MediaPlayer.create(context, R.raw.useless_sound) ?: return restoreVolume()
            mediaPlayer = player
            player.setOnCompletionListener { release(); restoreVolume() }
            player.setOnErrorListener { _, _, _ -> release(); restoreVolume(); true }
            player.start()
        } catch (e: Exception) {
            Log.e("UselessMode", "Error playing sound", e)
            release()
            restoreVolume()
        }
    }

    private fun restoreVolume() {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
    }

    fun release() {
        mediaPlayer?.let {
            runCatching { it.stop() }
            it.release()
        }
        mediaPlayer = null
    }
}

@Composable
fun UselessScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val effects = remember { UselessEffects(context, scope) }
    val warnings = remember { context.resources.getStringArray(R.array.useless_warnings) }
    val messages = remember { context.resources.getStringArray(R.array.useless_messages) }
    var enabled by remember { mutableStateOf(prefs.getBoolean(USELESS_PREF, false)) }
    var warning by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) { onDispose { effects.release() } }

    fun setUseless(on: Boolean) {
        prefs.edit().putBoolean(USELESS_PREF, on).apply()
        enabled = on
        if (on) {
            effects.flicker()
            effects.vibrate(longArrayOf(0, 50, 50, 50, 50, 200))
        } else {
            effects.vibrate(300)
        }
    }

    fun showRandomMessage() {
        effects.randomEffects()
        message = messages.random()
    }

    PartsPage(stringResource(R.string.useless_mode_title), onBack) {
        item {
            MainSwitchCard(stringResource(R.string.useless_mode_switch), enabled) { requested ->
                effects.randomEffects()
                if (requested) {
                    effects.vibrate(200)
                    warning = warnings.random()
                } else {
                    setUseless(false)
                    showRandomMessage()
                }
            }
        }
        item {
            NoteText(stringResource(if (enabled) R.string.useless_mode_enabled_summary else R.string.useless_mode_disabled_summary))
        }
        item { WarningCard(stringResource(R.string.useless_mode_warning)) }
    }

    warning?.let { text ->
        AlertDialog(
            onDismissRequest = { warning = null },
            icon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null) },
            title = { Text(stringResource(R.string.useless_mode_warning_title)) },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = {
                    warning = null
                    effects.vibrate(longArrayOf(0, 100, 50, 100, 50, 300))
                    setUseless(true)
                    showRandomMessage()
                }) { Text(stringResource(R.string.useless_mode_accept)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    warning = null
                    effects.vibrate(500)
                }) { Text(stringResource(R.string.useless_mode_cancel)) }
            },
        )
    }

    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null },
            icon = { Icon(Icons.Rounded.Celebration, contentDescription = null) },
            title = { Text(stringResource(R.string.useless_mode_status_title)) },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = {
                    message = null
                    if (Random.nextBoolean()) effects.vibrate(100)
                }) { Text(stringResource(R.string.useless_mode_amazing)) }
            },
        )
    }
}
