/*
 * Copyright (C) 2026 acerhizmq
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import org.lineageos.settings.ui.screens.AppearanceScreen
import org.lineageos.settings.ui.screens.ClearSpeakerScreen
import org.lineageos.settings.ui.screens.CoreControlScreen
import org.lineageos.settings.ui.screens.CreditsScreen
import org.lineageos.settings.ui.screens.DashboardScreen
import org.lineageos.settings.ui.screens.DcDimmingScreen
import org.lineageos.settings.ui.screens.DiracScreen
import org.lineageos.settings.ui.screens.FastChargeScreen
import org.lineageos.settings.ui.screens.FpsOverlayScreen
import org.lineageos.settings.ui.screens.GpuManagerScreen
import org.lineageos.settings.ui.screens.HbmScreen
import org.lineageos.settings.ui.screens.KernelManagerScreen
import org.lineageos.settings.ui.screens.RefreshRateScreen
import org.lineageos.settings.ui.screens.ResolutionScreen
import org.lineageos.settings.ui.screens.SaturationScreen
import org.lineageos.settings.ui.screens.SystemResolutionScreen
import org.lineageos.settings.ui.screens.ThermalScreen
import org.lineageos.settings.ui.screens.UselessScreen
import org.lineageos.settings.ui.screens.ZramScreen
import org.lineageos.settings.ui.theme.ColorMode
import org.lineageos.settings.ui.theme.PartsTheme

/** Every page of the dashboard. [key] is used by intents and the saved back stack. */
enum class Route(val key: String) {
    DASHBOARD("dashboard"),
    REFRESH_RATE("refresh_rate"),
    RESOLUTION("resolution"),
    SYSTEM_RESOLUTION("system_resolution"),
    SATURATION(PartsActivity.ROUTE_SATURATION),
    DC_DIMMING("dc_dimming"),
    HBM("hbm"),
    THERMAL(PartsActivity.ROUTE_THERMAL),
    KERNEL_MANAGER("kernel_manager"),
    GPU_MANAGER("gpu_manager"),
    CORE_CONTROL("core_control"),
    FAST_CHARGE("fast_charge"),
    ZRAM("zram"),
    DIRAC("dirac"),
    CLEAR_SPEAKER("clear_speaker"),
    FPS_OVERLAY("fps_overlay"),
    USELESS("useless"),
    APPEARANCE("appearance"),
    CREDITS("credits");

    companion object {
        fun fromKey(key: String?): Route? = entries.firstOrNull { it.key == key }
    }
}

class PartsActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ROUTE = "route"
        const val ROUTE_THERMAL = "thermal"
        const val ROUTE_SATURATION = "saturation"

        /** Opens the dashboard directly on [route]; used by the QS tiles. */
        @JvmStatic
        fun intentFor(context: Context, route: String): Intent =
            Intent(context, PartsActivity::class.java)
                .putExtra(EXTRA_ROUTE, route)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingRoute = intent?.getStringExtra(EXTRA_ROUTE)
        setContent {
            PartsApp(pendingRoute) { pendingRoute = null }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingRoute = intent.getStringExtra(EXTRA_ROUTE)
    }
}

@Composable
private fun PartsApp(pendingRoute: String?, onRouteConsumed: () -> Unit) {
    val context = LocalContext.current
    var colorMode by remember { mutableStateOf(ColorMode.load(context)) }
    var stack by rememberSaveable { mutableStateOf(listOf(Route.DASHBOARD.key)) }
    // Bumped on resume and on every navigation so pages re-read live system state.
    var refreshTick by remember { mutableIntStateOf(0) }

    LifecycleResumeEffect(Unit) {
        refreshTick++
        onPauseOrDispose { }
    }

    LaunchedEffect(pendingRoute) {
        val route = Route.fromKey(pendingRoute)
        if (route != null && route != Route.DASHBOARD) {
            stack = listOf(Route.DASHBOARD.key, route.key)
            refreshTick++
        }
        if (pendingRoute != null) onRouteConsumed()
    }

    fun navigate(route: Route) {
        stack = stack + route.key
        refreshTick++
    }

    fun back() {
        if (stack.size > 1) {
            stack = stack.dropLast(1)
            refreshTick++
        }
    }

    BackHandler(enabled = stack.size > 1) { back() }

    PartsTheme(colorMode) {
        val current = Route.fromKey(stack.last()) ?: Route.DASHBOARD
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                // Pages only open from the dashboard, so leaving it is forward.
                val direction = if (targetState != Route.DASHBOARD) 1 else -1
                (slideInHorizontally(tween(320)) { it / 6 * direction } + fadeIn(tween(320)))
                    .togetherWith(slideOutHorizontally(tween(260)) { -it / 8 * direction } + fadeOut(tween(200)))
            },
            label = "route",
        ) { route ->
            val onBack = ::back
            when (route) {
                Route.DASHBOARD -> DashboardScreen(refreshTick, ::navigate)
                Route.REFRESH_RATE -> RefreshRateScreen(onBack)
                Route.RESOLUTION -> ResolutionScreen(onBack)
                Route.SYSTEM_RESOLUTION -> SystemResolutionScreen(onBack)
                Route.SATURATION -> SaturationScreen(onBack)
                Route.DC_DIMMING -> DcDimmingScreen(onBack)
                Route.HBM -> HbmScreen(onBack)
                Route.THERMAL -> ThermalScreen(onBack)
                Route.KERNEL_MANAGER -> KernelManagerScreen(onBack)
                Route.GPU_MANAGER -> GpuManagerScreen(onBack)
                Route.CORE_CONTROL -> CoreControlScreen(onBack)
                Route.FAST_CHARGE -> FastChargeScreen(onBack)
                Route.ZRAM -> ZramScreen(onBack)
                Route.DIRAC -> DiracScreen(onBack)
                Route.CLEAR_SPEAKER -> ClearSpeakerScreen(onBack)
                Route.FPS_OVERLAY -> FpsOverlayScreen(onBack)
                Route.USELESS -> UselessScreen(onBack)
                Route.APPEARANCE -> AppearanceScreen(colorMode, onBack) {
                    ColorMode.save(context, it)
                    colorMode = it
                }
                Route.CREDITS -> CreditsScreen(onBack)
            }
        }
    }
}
