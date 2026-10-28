/*
 * Copyright (C) 2026 acerhizmq
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemProperties
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lineageos.settings.R
import org.lineageos.settings.corecontrol.CoreControlUtils
import org.lineageos.settings.dirac.DiracUtils
import org.lineageos.settings.display.DcDimmingUtils
import org.lineageos.settings.display.FpsInfoService
import org.lineageos.settings.fastcharge.FastChargeUtils
import org.lineageos.settings.gpumanager.GpuManagerUtils
import org.lineageos.settings.hbm.HbmUtils
import org.lineageos.settings.kernelmanager.KernelManagerUtils
import org.lineageos.settings.resolution.ResolutionUtils
import org.lineageos.settings.saturation.SaturationUtils
import org.lineageos.settings.ui.Route
import org.lineageos.settings.ui.components.FeatureCard
import org.lineageos.settings.ui.components.PartsCard
import org.lineageos.settings.ui.components.QuickToggle
import org.lineageos.settings.ui.components.RoundIconButton
import org.lineageos.settings.ui.components.ScreenPadding
import org.lineageos.settings.ui.components.SectionTitle
import org.lineageos.settings.zram.ZramUtils
import java.io.File

/** Live values shown on the dashboard cards, read off the main thread. */
private data class DashboardState(
    val systemResolution: Int = 0,
    val saturation: Int = SaturationUtils.DEFAULT,
    val governor: String = "",
    val gpuModel: String = "",
    val coresOnline: Int = CoreControlUtils.NUM_CORES,
    val fastChargeMode: Int = -1,
    val zramCompression: String = "",
    val swappiness: Int = 0,
    val useless: Boolean = false,
)

private class Card(val route: Route, val icon: ImageVector, val title: Int, val summary: String)

private class Section(val title: Int, val cards: List<Card>)

@Composable
fun DashboardScreen(refreshTick: Int, navigate: (Route) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val dirac = remember { DiracUtils.getInstance(context) }

    val state by produceState(DashboardState(), refreshTick) {
        value = withContext(Dispatchers.IO) { readDashboardState(context) }
    }

    // Quick toggle states, re-read whenever the dashboard comes back into view.
    var dcOn by remember(refreshTick) { mutableStateOf(DcDimmingUtils.isEnabled(context)) }
    var hbmOn by remember(refreshTick) { mutableStateOf(HbmUtils.isHbmEnabled(context)) }
    var fpsOn by remember(refreshTick) { mutableStateOf(prefs.getBoolean(FpsInfoService.PREF_KEY_FPS_STATE, false)) }
    var diracOn by remember(refreshTick) { mutableStateOf(dirac.isDiracEnabled) }
    val dcSupported = remember { DcDimmingUtils.isSupported() }
    val hbmSupported = remember { HbmUtils.isSupported() }
    val fpsSupported = remember { File(FpsInfoService.FPS_PATH).exists() }

    val on = stringResource(R.string.state_on)
    val off = stringResource(R.string.state_off)
    val unsupported = stringResource(R.string.state_not_supported)
    val resolutions = stringArrayResource(R.array.resolution_mode_entries)
    val fastChargeModes = stringArrayResource(R.array.fastcharge_mode_entries)

    val sections = listOf(
        Section(R.string.section_display, listOf(
            Card(Route.REFRESH_RATE, Icons.Rounded.Refresh, R.string.refresh_title, stringResource(R.string.refresh_card_summary)),
            Card(Route.RESOLUTION, Icons.Rounded.AspectRatio, R.string.resolution_title, stringResource(R.string.resolution_card_summary)),
            Card(Route.SYSTEM_RESOLUTION, Icons.Rounded.Smartphone, R.string.system_resolution_title,
                resolutions.getOrElse(state.systemResolution) { resolutions[0] }),
            Card(Route.SATURATION, Icons.Rounded.Palette, R.string.saturation_title,
                stringResource(R.string.percent_value, state.saturation)),
            Card(Route.DC_DIMMING, Icons.Rounded.BrightnessMedium, R.string.dc_dimming_title,
                if (!dcSupported) unsupported else if (dcOn) on else off),
            Card(Route.HBM, Icons.Rounded.LightMode, R.string.hbm_title,
                when {
                    hbmOn -> on
                    HbmUtils.isAutoHbmEnabled(context) -> stringResource(R.string.hbm_card_auto)
                    else -> off
                }),
        )),
        Section(R.string.section_performance, listOf(
            Card(Route.THERMAL, Icons.Rounded.Thermostat, R.string.thermal_title, stringResource(R.string.thermal_card_summary)),
            Card(Route.KERNEL_MANAGER, Icons.Rounded.Memory, R.string.kernel_manager_title, state.governor),
            Card(Route.GPU_MANAGER, Icons.Rounded.DeveloperBoard, R.string.gpu_manager_title, state.gpuModel),
            Card(Route.CORE_CONTROL, Icons.Rounded.GridView, R.string.core_control_title,
                stringResource(R.string.core_card_summary, state.coresOnline, CoreControlUtils.NUM_CORES)),
        )),
        Section(R.string.section_battery_memory, listOf(
            Card(Route.FAST_CHARGE, Icons.Rounded.Bolt, R.string.fastcharge_title,
                fastChargeModes.getOrNull(state.fastChargeMode) ?: unsupported),
            Card(Route.ZRAM, Icons.Rounded.Layers, R.string.zram_title,
                stringResource(R.string.zram_card_summary, state.zramCompression, state.swappiness)),
        )),
        Section(R.string.section_audio, listOf(
            Card(Route.DIRAC, Icons.Rounded.GraphicEq, R.string.dirac_title, if (diracOn) on else off),
            Card(Route.CLEAR_SPEAKER, Icons.Rounded.WaterDrop, R.string.clear_speaker_title, stringResource(R.string.clear_speaker_card_summary)),
        )),
        Section(R.string.section_extras, listOf(
            Card(Route.FPS_OVERLAY, Icons.Rounded.Speed, R.string.fps_overlay_title,
                if (!fpsSupported) unsupported else if (fpsOn) on else off),
            Card(Route.USELESS, Icons.Rounded.SentimentVerySatisfied, R.string.useless_mode_title, if (state.useless) on else off),
        )),
        Section(R.string.section_about, listOf(
            Card(Route.APPEARANCE, Icons.Rounded.Brush, R.string.appearance_title, stringResource(R.string.appearance_card_summary)),
            Card(Route.CREDITS, Icons.Rounded.VolunteerActivism, R.string.credits_title, stringResource(R.string.credits_card_summary)),
        )),
    )

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalItemSpacing = 10.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Column {
                    Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 16.dp),
                    ) {
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        RoundIconButton(
                            icon = Icons.Rounded.Tune,
                            contentDescription = stringResource(R.string.appearance_title),
                            onClick = { navigate(Route.APPEARANCE) },
                            size = 44.dp,
                        )
                    }
                    DeviceCard()
                }
            }
            item(span = StaggeredGridItemSpan.FullLine) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickToggle(Icons.Rounded.BrightnessMedium, stringResource(R.string.quick_dc_dimming), dcOn, dcSupported) {
                        DcDimmingUtils.setEnabled(context, it)
                        dcOn = it
                        hbmOn = HbmUtils.isHbmEnabled(context)
                    }
                    QuickToggle(Icons.Rounded.LightMode, stringResource(R.string.quick_hbm), hbmOn, hbmSupported && !dcOn) {
                        if (HbmUtils.setHbmEnabled(context, it)) hbmOn = it
                    }
                    QuickToggle(Icons.Rounded.Speed, stringResource(R.string.quick_fps), fpsOn, fpsSupported) {
                        setFpsOverlay(context, it)
                        fpsOn = it
                    }
                    QuickToggle(Icons.Rounded.GraphicEq, stringResource(R.string.quick_dirac), diracOn, true) {
                        dirac.setEnabled(it)
                        diracOn = it
                    }
                }
            }
            sections.forEach { section ->
                item(span = StaggeredGridItemSpan.FullLine) { SectionTitle(stringResource(section.title)) }
                section.cards.forEachIndexed { index, card ->
                    item(key = card.route.key) {
                        FeatureCard(
                            icon = card.icon,
                            title = stringResource(card.title),
                            summary = card.summary,
                            tall = index % 3 == 0,
                            onClick = { navigate(card.route) },
                        )
                    }
                }
            }
            item(span = StaggeredGridItemSpan.FullLine) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        stringResource(R.string.made_for_veux),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        }
    }
}

@Composable
private fun DeviceCard() {
    val androidVersion = remember { Build.VERSION.RELEASE }
    val kernelVersion = remember { System.getProperty("os.version").orEmpty().substringBefore('-') }
    val codename = remember { SystemProperties.get("ro.product.device", Build.DEVICE) }
    PartsCard(contentPadding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.device_prompt, codename),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.device_family),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.device_details, codename, androidVersion, kernelVersion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(14.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(66.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primary),
            ) {
                Text("mi", color = MaterialTheme.colorScheme.onPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun setFpsOverlay(context: Context, enabled: Boolean) {
    val intent = Intent(context, FpsInfoService::class.java)
    if (enabled) context.startService(intent) else context.stopService(intent)
}

private fun readDashboardState(context: Context): DashboardState {
    val zram = ZramUtils(context)
    val coresOnline = (0 until CoreControlUtils.NUM_CORES).count { CoreControlUtils.isCoreOnline(it) }
    return DashboardState(
        systemResolution = ResolutionUtils(context).globalState,
        saturation = SaturationUtils.get(context),
        governor = KernelManagerUtils().getCurrentGovernor(KernelManagerUtils.EFFICIENCY_CLUSTER),
        gpuModel = GpuManagerUtils().gpuModel,
        coresOnline = coresOnline,
        fastChargeMode = if (FastChargeUtils.isModeSupported())
            FastChargeUtils(context).mode.toIntOrNull() ?: -1 else -1,
        zramCompression = zram.currentCompression,
        swappiness = zram.currentSwappiness,
        useless = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(USELESS_PREF, false),
    )
}
