/*
 * Copyright (C) 2026 acerhizmq
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.lineageos.settings.R
import org.lineageos.settings.display.DcDimmingUtils
import org.lineageos.settings.hbm.HbmUtils
import org.lineageos.settings.resolution.ResolutionUtils
import org.lineageos.settings.saturation.SaturationTileService
import org.lineageos.settings.saturation.SaturationUtils
import org.lineageos.settings.ui.components.ChipGroupCard
import org.lineageos.settings.ui.components.Group
import org.lineageos.settings.ui.components.MainSwitchCard
import org.lineageos.settings.ui.components.NoteText
import org.lineageos.settings.ui.components.PartsCard
import org.lineageos.settings.ui.components.PartsPage
import org.lineageos.settings.ui.components.PillButton
import org.lineageos.settings.ui.components.RoundIconButton
import org.lineageos.settings.ui.components.SliderCard
import org.lineageos.settings.ui.components.SwitchRow
import org.lineageos.settings.ui.components.WarningCard
import org.lineageos.settings.utils.TileUtils
import java.text.NumberFormat

@Composable
fun SystemResolutionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val utils = remember { ResolutionUtils(context) }
    var state by remember { mutableIntStateOf(utils.globalState) }
    val modes = stringArrayResource(R.array.resolution_mode_entries).toList()
    PartsPage(stringResource(R.string.system_resolution_title), onBack) {
        item {
            PartsCard {
                Text(stringResource(R.string.system_resolution_current), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(utils.getResolutionString(state), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        item {
            ChipGroupCard(stringResource(R.string.system_resolution_mode), modes, state) { mode ->
                if (utils.globalState != mode) utils.setGlobalState(mode)
                state = mode
            }
        }
        item { NoteText(stringResource(R.string.system_resolution_note)) }
    }
}

@Composable
fun SaturationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var value by remember { mutableIntStateOf(SaturationUtils.get(context)) }
    PartsPage(stringResource(R.string.saturation_title), onBack) {
        item { SaturationPreview() }
        item {
            SliderCard(
                title = stringResource(R.string.saturation_title),
                value = value,
                range = SaturationUtils.MIN..SaturationUtils.MAX,
                defaultValue = SaturationUtils.DEFAULT,
                format = { context.getString(R.string.percent_value, it) },
                onValueChange = {
                    value = it
                    SaturationUtils.set(context, it)
                },
            )
        }
        item { NoteText(stringResource(R.string.saturation_note)) }
        item {
            PillButton(
                text = stringResource(R.string.tile_add),
                primary = false,
                icon = Icons.Rounded.AddCircleOutline,
                onClick = {
                    TileUtils.requestAddTileService(
                        context, SaturationTileService::class.java,
                        R.string.saturation_title, R.drawable.ic_saturation_tile,
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun SaturationPreview() {
    val images = listOf(R.drawable.image_preview1, R.drawable.image_preview2, R.drawable.image_preview3)
    val pager = rememberPagerState { images.size }
    val scope = rememberCoroutineScope()
    val description = stringResource(R.string.image_preview_content_description)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            Image(
                painter = painterResource(images[page]),
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
        ) {
            if (pager.currentPage > 0) {
                RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.preview_previous), {
                    scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
                }, size = 36.dp)
            } else {
                Spacer(Modifier.size(36.dp))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                modifier = Modifier.weight(1f),
            ) {
                images.indices.forEach { index ->
                    Box(
                        Modifier
                            .size(if (index == pager.currentPage) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pager.currentPage) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                    )
                }
            }
            if (pager.currentPage < images.size - 1) {
                RoundIconButton(Icons.AutoMirrored.Rounded.ArrowForward, stringResource(R.string.preview_next), {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                }, size = 36.dp)
            } else {
                Spacer(Modifier.size(36.dp))
            }
        }
    }
}

@Composable
fun DcDimmingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val supported = remember { DcDimmingUtils.isSupported() }
    var enabled by remember { mutableStateOf(DcDimmingUtils.isEnabled(context)) }
    PartsPage(stringResource(R.string.dc_dimming_title), onBack) {
        item {
            MainSwitchCard(stringResource(R.string.dc_dimming_switch), enabled && supported, enabled = supported) {
                DcDimmingUtils.setEnabled(context, it)
                enabled = it
            }
        }
        item {
            NoteText(
                stringResource(if (supported) R.string.dc_dimming_note else R.string.dc_dimming_not_supported)
            )
        }
    }
}

@Composable
fun HbmScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val supported = remember { HbmUtils.isSupported() }
    val dcOn = remember { DcDimmingUtils.isEnabled(context) }
    var hbm by remember { mutableStateOf(HbmUtils.isHbmEnabled(context)) }
    var autoHbm by remember { mutableStateOf(HbmUtils.isAutoHbmEnabled(context)) }
    var threshold by remember { mutableIntStateOf(HbmUtils.getThreshold(context)) }
    var disableTime by remember { mutableIntStateOf(HbmUtils.getDisableTime(context)) }
    val numbers = remember { NumberFormat.getIntegerInstance() }
    PartsPage(stringResource(R.string.hbm_title), onBack) {
        item {
            Group(
                { shape ->
                    SwitchRow(
                        shape = shape,
                        title = stringResource(R.string.hbm_switch),
                        summary = stringResource(
                            when {
                                !supported -> R.string.hbm_not_supported
                                dcOn -> R.string.hbm_blocked_by_dc
                                else -> R.string.hbm_switch_summary
                            }
                        ),
                        checked = hbm,
                        enabled = supported && !dcOn,
                    ) {
                        if (HbmUtils.setHbmEnabled(context, it)) hbm = it
                    }
                },
                { shape ->
                    SwitchRow(
                        shape = shape,
                        title = stringResource(R.string.auto_hbm_switch),
                        summary = stringResource(R.string.auto_hbm_summary),
                        checked = autoHbm,
                    ) {
                        HbmUtils.setAutoHbmEnabled(context, it)
                        autoHbm = it
                    }
                },
            )
        }
        item {
            SliderCard(
                title = stringResource(R.string.auto_hbm_threshold),
                value = threshold,
                range = HbmUtils.THRESHOLD_MIN..HbmUtils.THRESHOLD_MAX,
                step = HbmUtils.THRESHOLD_STEP,
                defaultValue = HbmUtils.THRESHOLD_DEFAULT,
                enabled = autoHbm,
                format = { context.getString(R.string.lux_value, numbers.format(it)) },
                onValueChange = { threshold = it },
                onValueChangeFinished = { HbmUtils.setThreshold(context, it) },
            )
        }
        item {
            SliderCard(
                title = stringResource(R.string.auto_hbm_disable_time),
                value = disableTime,
                range = HbmUtils.DISABLE_TIME_MIN..HbmUtils.DISABLE_TIME_MAX,
                defaultValue = HbmUtils.DISABLE_TIME_DEFAULT,
                enabled = autoHbm,
                format = { context.getString(R.string.seconds_value, it) },
                onValueChange = { disableTime = it },
                onValueChangeFinished = { HbmUtils.setDisableTime(context, it) },
            )
        }
        item { WarningCard(stringResource(R.string.hbm_warning)) }
    }
}
