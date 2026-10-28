/*
 * Copyright (C) 2026 acerhizmq
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lineageos.settings.R
import org.lineageos.settings.refreshrate.RefreshUtils
import org.lineageos.settings.resolution.ResolutionUtils
import org.lineageos.settings.thermal.ThermalUtils
import org.lineageos.settings.ui.components.ChoiceRow
import org.lineageos.settings.ui.components.NoteText
import org.lineageos.settings.ui.components.PartsPage
import org.lineageos.settings.ui.components.groupShape
import java.text.Collator

private data class LaunchableApp(val packageName: String, val label: String)

/** Read and write the per-app mode index; the index matches the modes array. */
private class PerAppStore(val read: (String) -> Int, val write: (String, Int) -> Unit)

@Composable
fun RefreshRateScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val utils = remember { RefreshUtils(context) }
    PerAppScreen(
        title = stringResource(R.string.refresh_title),
        modes = stringArrayResource(R.array.refresh_mode_entries).toList(),
        note = stringResource(R.string.refresh_note),
        icons = listOf(R.drawable.ic_refresh_default, R.drawable.ic_refresh_60, R.drawable.ic_refresh_120, R.drawable.ic_refresh_60),
        store = remember { PerAppStore(utils::getStateForPackage, utils::writePackage) },
        onBack = onBack,
    )
}

@Composable
fun ResolutionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val utils = remember { ResolutionUtils(context) }
    PerAppScreen(
        title = stringResource(R.string.resolution_title),
        modes = stringArrayResource(R.array.resolution_mode_entries).toList(),
        note = stringResource(R.string.resolution_note),
        icons = listOf(R.drawable.ic_resolution_default, R.drawable.ic_resolution_480, R.drawable.ic_resolution_540, R.drawable.ic_resolution_720),
        store = remember { PerAppStore(utils::getStateForPackage, utils::writePackage) },
        onBack = onBack,
    )
}

@Composable
fun ThermalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val utils = remember { ThermalUtils(context) }
    PerAppScreen(
        title = stringResource(R.string.thermal_title),
        modes = stringArrayResource(R.array.thermal_mode_entries).toList(),
        note = stringResource(R.string.thermal_note),
        icons = listOf(
            R.drawable.ic_thermal_default, R.drawable.ic_thermal_benchmark, R.drawable.ic_thermal_browser,
            R.drawable.ic_thermal_camera, R.drawable.ic_thermal_dialer, R.drawable.ic_thermal_gaming,
            R.drawable.ic_thermal_navigation, R.drawable.ic_thermal_streaming, R.drawable.ic_thermal_video,
        ),
        store = remember { PerAppStore(utils::getStateForPackage, utils::writePackage) },
        onBack = onBack,
    )
}

@Composable
private fun PerAppScreen(
    title: String,
    modes: List<String>,
    note: String,
    icons: List<Int>,
    store: PerAppStore,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val apps by produceState<List<LaunchableApp>?>(null) {
        value = withContext(Dispatchers.IO) { loadLaunchableApps(context) }
    }
    var query by rememberSaveable { mutableStateOf("") }
    // Local copy so a choice shows immediately; the store stays the source of truth.
    val chosen = remember { mutableStateMapOf<String, Int>() }

    PartsPage(title, onBack) {
        item { NoteText(note) }
        item { SearchField(query) { query = it } }
        val list = apps
        if (list == null) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            val filtered = if (query.isBlank()) list
            else list.filter { it.label.contains(query.trim(), ignoreCase = true) }
            itemsIndexed(filtered, key = { _, app -> app.packageName }) { index, app ->
                Column(Modifier.padding(bottom = 3.dp)) {
                    ChoiceRow(
                        shape = groupShape(index, filtered.size),
                        title = app.label,
                        options = modes,
                        selected = chosen[app.packageName] ?: store.read(app.packageName),
                        showChevron = false,
                        optionIcons = icons,
                        leading = { AppIcon(app.packageName) },
                    ) { mode ->
                        if (store.read(app.packageName) != mode) store.write(app.packageName, mode)
                        chosen[app.packageName] = mode
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.search_no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp),
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(stringResource(R.string.search_apps), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(role = Role.Button) { onQueryChange("") },
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.search_clear),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AppIcon(packageName: String) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName).toBitmap(112, 112).asImageBitmap()
            }.getOrNull()
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(34.dp)) }
    }
}

/** Apps with a launcher entry, sorted by label like the previous Settings lists. */
private fun loadLaunchableApps(context: Context): List<LaunchableApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val collator = Collator.getInstance()
    return pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        .map { it.activityInfo.applicationInfo }
        .distinctBy { it.packageName }
        .map { LaunchableApp(it.packageName, it.loadLabel(pm).toString()) }
        .sortedWith { a, b -> collator.compare(a.label, b.label) }
}
