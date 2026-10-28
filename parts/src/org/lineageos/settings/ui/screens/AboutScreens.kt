/*
 * Copyright (C) 2026 acerhizmq
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.ui.screens

import android.app.LocaleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.LocaleList
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lineageos.settings.R
import org.lineageos.settings.ui.components.ChipGroupCard
import org.lineageos.settings.ui.components.IconTile
import org.lineageos.settings.ui.components.PartsCard
import org.lineageos.settings.ui.components.PartsPage
import org.lineageos.settings.ui.components.SectionTitle
import org.lineageos.settings.ui.components.groupShape
import org.lineageos.settings.ui.theme.ColorMode
import org.lineageos.settings.ui.theme.SectionTitleStyle

/** Languages offered in Appearance; names are shown in their own language. */
private val AppLanguages = listOf(
    "en" to "English",
    "tr" to "Türkçe",
    "de" to "Deutsch",
    "hi" to "हिन्दी",
    "es" to "Español",
    "pt-BR" to "Português (Brasil)",
    "ru" to "Русский",
    "id" to "Bahasa Indonesia",
    "fr" to "Français",
    "it" to "Italiano",
    "ar" to "العربية",
    "zh-CN" to "简体中文",
    "vi" to "Tiếng Việt",
)

private fun currentLanguageTag(context: Context): String? {
    val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
    if (locales.isEmpty) return null
    val locale = locales[0]
    return AppLanguages.firstOrNull { (tag, _) ->
        val wanted = java.util.Locale.forLanguageTag(tag)
        wanted.language == locale.language && (wanted.country.isEmpty() || wanted.country == locale.country)
    }?.first
}

@Composable
fun AppearanceScreen(colorMode: ColorMode, onBack: () -> Unit, onColorModeChange: (ColorMode) -> Unit) {
    val context = LocalContext.current
    var language by remember { mutableStateOf(currentLanguageTag(context)) }
    val options = listOf(null to stringResource(R.string.language_system)) + AppLanguages
    PartsPage(stringResource(R.string.appearance_title), onBack) {
        item {
            ChipGroupCard(
                title = stringResource(R.string.appearance_colors),
                options = listOf(stringResource(R.string.colors_material_you), stringResource(R.string.colors_signature)),
                selected = colorMode.ordinal,
            ) { onColorModeChange(ColorMode.entries[it]) }
        }
        item {
            Text(
                stringResource(if (colorMode == ColorMode.DYNAMIC) R.string.colors_material_you_summary else R.string.colors_signature_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 6.dp),
            )
        }
        item { SectionTitle(stringResource(R.string.appearance_language)) }
        options.forEachIndexed { index, (tag, name) ->
            item(key = tag ?: "system") {
                val selected = tag == language
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(bottom = 3.dp)
                        .fillMaxWidth()
                        .clip(groupShape(index, options.size))
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer)
                        .selectable(selected = selected, role = Role.RadioButton) {
                            language = tag
                            val locales = if (tag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
                            // The system recreates the activity in the new language.
                            context.getSystemService(LocaleManager::class.java).applicationLocales = locales
                        }
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

private const val GITHUB_URL = "https://github.com/acerhizmq"
private const val TELEGRAM_URL = "https://t.me/acerbuilds"

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // No browser installed; nothing to open.
    }
}

private class Source(val name: String, val description: Int, val icon: ImageVector)

private val Sources = listOf(
    Source("KamiKaonashi", R.string.credits_source_kami, Icons.Rounded.AutoAwesome),
    Source("PocoF3Releases · johnmart19", R.string.credits_source_pocof3, Icons.Rounded.GraphicEq),
    Source("GuidixX", R.string.credits_source_guidixx, Icons.Rounded.Speed),
    Source("Chad Froebel · engstk", R.string.credits_source_fastcharge, Icons.Rounded.Bolt),
)

private val Projects = listOf(
    "LineageOS", "CyanogenMod", "OmniROM", "YAAP", "Paranoid Android", "crDroid",
    "Evolution X", "Havoc-OS", "Chaldeastudio", "AOSP",
)

private val Contributors = listOf(
    "Arian", "Adithya R", "EmanuelCN", "someone5678", "Mudit200408", "kssrao13882",
    "Nauval Rizky", "TheScarastic", "Adhitya Mohan", "alibei", "Chaitanyakm", "AnierinB",
    "Amrita Das", "Anush02198",
)

@Composable
fun CreditsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    PartsPage(stringResource(R.string.credits_title), onBack) {
        item { DeveloperCard { openUrl(context, it) } }
        item { SectionTitle(stringResource(R.string.credits_built_by)) }
        item {
            PartsCard {
                listOf(
                    Icons.Rounded.Dashboard to R.string.credits_work_dashboard,
                    Icons.Rounded.Bolt to R.string.credits_work_fastcharge,
                    Icons.Rounded.GraphicEq to R.string.credits_work_dirac,
                    Icons.Rounded.Speed to R.string.credits_work_fps,
                    Icons.Rounded.LightMode to R.string.credits_work_hbm,
                    Icons.Rounded.DeveloperBoard to R.string.credits_work_fixes,
                    Icons.Rounded.Build to R.string.credits_work_integration,
                ).forEach { (icon, text) ->
                    Row(modifier = Modifier.padding(vertical = 5.dp)) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        item { SectionTitle(stringResource(R.string.credits_original_code)) }
        Sources.forEachIndexed { index, source ->
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(bottom = 3.dp)
                        .fillMaxWidth()
                        .clip(groupShape(index, Sources.size))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    IconTile(source.icon, size = 42.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(source.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(2.dp))
                        Text(stringResource(source.description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { SectionTitle(stringResource(R.string.credits_projects)) }
        item { NameChips(Projects) }
        item { SectionTitle(stringResource(R.string.credits_contributors)) }
        item { NameChips(Contributors) }
    }
}

@Composable
private fun DeveloperCard(open: (String) -> Unit) {
    PartsCard(color = MaterialTheme.colorScheme.primary) {
        Text(
            stringResource(R.string.credits_developed_by).uppercase(),
            style = SectionTitleStyle,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimary),
            ) {
                Text("A", color = MaterialTheme.colorScheme.primary, fontSize = 26.sp, style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("acerhizmq", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.credits_developer_summary), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LinkChip(Icons.Rounded.Code, stringResource(R.string.credits_github)) { open(GITHUB_URL) }
            LinkChip(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.credits_telegram)) { open(TELEGRAM_URL) }
        }
    }
}

@Composable
private fun LinkChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onPrimary)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 44.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun NameChips(names: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        names.forEach { name ->
            Text(
                name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}
