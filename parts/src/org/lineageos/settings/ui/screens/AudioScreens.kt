/*
 * Copyright (C) 2026 acerhizmq
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.ui.screens

import android.app.Activity
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import org.lineageos.settings.R
import org.lineageos.settings.dirac.DiracUtils
import org.lineageos.settings.ui.components.ChoiceRow
import org.lineageos.settings.ui.components.Group
import org.lineageos.settings.ui.components.MainSwitchCard
import org.lineageos.settings.ui.components.NoteText
import org.lineageos.settings.ui.components.PartsCard
import org.lineageos.settings.ui.components.PartsPage
import org.lineageos.settings.ui.components.SectionTitle
import org.lineageos.settings.ui.components.SwitchRow
import java.io.IOException

@Composable
fun DiracScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dirac = remember { DiracUtils.getInstance(context) }
    val sceneValues = stringArrayResource(R.array.scenario_selector_values).map { it.toInt() }
    val headsetValues = stringArrayResource(R.array.dirac_headset_pref_values).map { it.toInt() }
    val presetValues = stringArrayResource(R.array.dirac_preset_pref_values).toList()

    var enabled by remember { mutableStateOf(dirac.isDiracEnabled) }
    var scene by remember { mutableIntStateOf(sceneValues.indexOf(dirac.scenario).coerceAtLeast(0)) }
    var headset by remember { mutableIntStateOf(headsetValues.indexOf(dirac.headsetType).coerceAtLeast(0)) }
    var preset by remember { mutableIntStateOf(presetValues.indexOf(dirac.preset).coerceAtLeast(0)) }
    var equalizer by remember { mutableStateOf(dirac.isEqualizerEnabled) }
    var pauseDuringCalls by remember { mutableStateOf(dirac.isPauseDuringCallsEnabled) }
    var hifi by remember { mutableStateOf(dirac.hifiMode) }

    PartsPage(stringResource(R.string.dirac_title), onBack) {
        item {
            Box(Modifier.fillMaxWidth().padding(bottom = 12.dp), contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(R.drawable.ic_dirac_logo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.height(36.dp),
                )
            }
        }
        item {
            MainSwitchCard(stringResource(R.string.dirac_enable), enabled) {
                dirac.setEnabled(it)
                enabled = it
                if (!it) {
                    // Hi-Fi only makes sense with the effect on.
                    hifi = false
                    dirac.setHifiMode(0)
                }
            }
        }
        item {
            Group(
                { shape ->
                    ChoiceRow(shape, stringResource(R.string.dirac_scenario), stringArrayResource(R.array.scenario_selector_titles).toList(),
                        scene, enabled = enabled) {
                        scene = it
                        dirac.setScenario(sceneValues[it])
                    }
                },
                { shape ->
                    ChoiceRow(shape, stringResource(R.string.dirac_headset_title), stringArrayResource(R.array.dirac_headset_pref_entries).toList(),
                        headset, enabled = enabled && !hifi) {
                        headset = it
                        dirac.setHeadsetType(headsetValues[it])
                    }
                },
                { shape ->
                    ChoiceRow(shape, stringResource(R.string.dirac_preset_title), stringArrayResource(R.array.dirac_preset_pref_entries).toList(),
                        preset, enabled = enabled && !hifi) {
                        preset = it
                        dirac.setLevel(presetValues[it])
                    }
                },
            )
        }
        item {
            Group(
                { shape ->
                    SwitchRow(shape, stringResource(R.string.dirac_eq_enable_title), stringResource(R.string.dirac_eq_enable_summary),
                        equalizer, enabled = enabled && !hifi) {
                        equalizer = it
                        dirac.setEqualizerEnabled(it)
                    }
                },
                { shape ->
                    SwitchRow(shape, stringResource(R.string.dirac_pause_calls_title), stringResource(R.string.dirac_pause_calls_summary),
                        pauseDuringCalls, enabled = enabled) {
                        pauseDuringCalls = it
                        dirac.setPauseDuringCallsEnabled(it)
                    }
                },
            )
        }
        item { SectionTitle(stringResource(R.string.dirac_hifi_title)) }
        item {
            Group(
                { shape ->
                    SwitchRow(shape, stringResource(R.string.dirac_hifi_enable_title), stringResource(R.string.dirac_hifi_summary),
                        hifi, enabled = enabled) {
                        hifi = it
                        dirac.setHifiMode(if (it) 1 else 0)
                    }
                },
            )
        }
    }
}

private const val CLEAN_DURATION_SECONDS = 30
private const val TAG = "ClearSpeaker"

/** Loops the low tone on the media stream with the earpiece clean mode on. */
private class ClearSpeakerPlayer(private val context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var player: MediaPlayer? = null

    fun start(): Boolean {
        audioManager.setParameters("status_earpiece_clean=on")
        val mp = MediaPlayer()
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        mp.isLooping = true
        return try {
            context.resources.openRawResourceFd(R.raw.clear_speaker_sound).use { file ->
                mp.setDataSource(file.fileDescriptor, file.startOffset, file.length)
            }
            mp.setVolume(1.0f, 1.0f)
            mp.prepare()
            mp.start()
            player = mp
            true
        } catch (e: IOException) {
            Log.e(TAG, "Failed to play speaker clean sound!", e)
            mp.release()
            audioManager.setParameters("status_earpiece_clean=off")
            false
        }
    }

    fun stop() {
        player?.let {
            if (it.isPlaying) it.stop()
            it.reset()
            it.release()
        }
        player = null
        audioManager.setParameters("status_earpiece_clean=off")
    }
}

@Composable
fun ClearSpeakerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember { ClearSpeakerPlayer(context) }
    var remaining by remember { mutableIntStateOf(0) }
    val playing = remaining > 0

    DisposableEffect(Unit) {
        (context as? Activity)?.volumeControlStream = AudioManager.STREAM_MUSIC
        onDispose { player.stop() }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (remaining > 0) {
            player.stop()
            remaining = 0
        }
    }
    LaunchedEffect(playing) {
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
        if (!playing) return@LaunchedEffect
        player.stop()
    }

    PartsPage(stringResource(R.string.clear_speaker_title), onBack) {
        item {
            val bubble by animateColorAsState(
                if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                label = "speaker",
            )
            PartsCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(bubble)
                            .clickable(enabled = !playing, role = Role.Button) {
                                if (player.start()) remaining = CLEAN_DURATION_SECONDS
                            },
                    ) {
                        Icon(
                            if (playing) Icons.Rounded.HourglassTop else Icons.Rounded.WaterDrop,
                            contentDescription = null,
                            tint = if (playing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (playing) stringResource(R.string.clear_speaker_running, remaining)
                        else stringResource(R.string.clear_speaker_start),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        item { NoteText(stringResource(R.string.clear_speaker_description)) }
    }
}
