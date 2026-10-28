/*
 * Copyright (C) 2026 acerhizmq
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager

/** Color source picked in Appearance. */
enum class ColorMode(val key: String) {
    DYNAMIC("dynamic"),
    SIGNATURE("signature");

    companion object {
        private const val PREF = "ui_color_mode"

        fun load(context: Context): ColorMode {
            val saved = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF, DYNAMIC.key)
            return entries.firstOrNull { it.key == saved } ?: DYNAMIC
        }

        fun save(context: Context, mode: ColorMode) {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(PREF, mode.key).apply()
        }
    }
}

// Signature: graphite surfaces with a single warm amber accent.
private val SignatureDark = darkColorScheme(
    primary = Color(0xFFF4B860),
    onPrimary = Color(0xFF3D2700),
    primaryContainer = Color(0xFF3A2F1C),
    onPrimaryContainer = Color(0xFFF4B860),
    secondary = Color(0xFFD8C4A0),
    onSecondary = Color(0xFF3B2F15),
    secondaryContainer = Color(0xFF2E2A22),
    onSecondaryContainer = Color(0xFFF4E0BB),
    background = Color(0xFF0F1214),
    onBackground = Color(0xFFE7ECEF),
    surface = Color(0xFF0F1214),
    onSurface = Color(0xFFE7ECEF),
    surfaceVariant = Color(0xFF22282C),
    onSurfaceVariant = Color(0xFFA8B2B8),
    surfaceContainerLowest = Color(0xFF0B0D0F),
    surfaceContainerLow = Color(0xFF14181A),
    surfaceContainer = Color(0xFF181C1F),
    surfaceContainerHigh = Color(0xFF1D2225),
    surfaceContainerHighest = Color(0xFF22282C),
    outline = Color(0xFF5C666C),
    outlineVariant = Color(0xFF2C3338),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val SignatureLight = lightColorScheme(
    primary = Color(0xFF9A5F00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF7E1BC),
    onPrimaryContainer = Color(0xFF4A2E00),
    secondary = Color(0xFF6D5C3F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF5E0BB),
    onSecondaryContainer = Color(0xFF261A04),
    background = Color(0xFFF6F4EF),
    onBackground = Color(0xFF1C1F21),
    surface = Color(0xFFF6F4EF),
    onSurface = Color(0xFF1C1F21),
    surfaceVariant = Color(0xFFECE8DF),
    onSurfaceVariant = Color(0xFF5A6267),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBFAF6),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF1EEE7),
    surfaceContainerHighest = Color(0xFFECE8DF),
    outline = Color(0xFF8A9095),
    outlineVariant = Color(0xFFDDD7CB),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

private val PartsTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Medium, letterSpacing = (-0.5).sp),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Medium, fontSize = 30.sp, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Medium, fontSize = 26.sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Medium, fontSize = 22.sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Medium, fontSize = 20.sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium, fontSize = 16.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
        labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 1.6.sp),
    )
}

/** Section headers: small caps style with wide tracking, as in the approved design. */
val SectionTitleStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.7.sp)

private val PartsShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun PartsTheme(colorMode: ColorMode, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val scheme: ColorScheme = when (colorMode) {
        ColorMode.DYNAMIC -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        ColorMode.SIGNATURE -> if (dark) SignatureDark else SignatureLight
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = PartsTypography,
        shapes = PartsShapes,
        content = content,
    )
}
