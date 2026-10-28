/*
 * Copyright (C) 2026 acerhizmq
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.ui.screens

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import org.lineageos.settings.R
import org.lineageos.settings.fastcharge.FastChargeUtils
import org.lineageos.settings.ui.components.ChipGroupCard
import org.lineageos.settings.ui.components.Group
import org.lineageos.settings.ui.components.NoteText
import org.lineageos.settings.ui.components.PartsPage
import org.lineageos.settings.ui.components.SliderCard
import org.lineageos.settings.ui.components.SwitchRow
import org.lineageos.settings.zram.ZramUtils

@Composable
fun FastChargeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val utils = remember { FastChargeUtils(context) }
    val modeSupported = remember { FastChargeUtils.isModeSupported() }
    val usbSupported = remember { FastChargeUtils.isUsbSupported() }
    var mode by remember { mutableIntStateOf(utils.mode.toIntOrNull() ?: FastChargeUtils.MODE_DEFAULT.toInt()) }
    var usb by remember { mutableStateOf(usbSupported && utils.isUsbEnabled) }
    val modes = stringArrayResource(R.array.fastcharge_mode_entries).toList()
    PartsPage(stringResource(R.string.fastcharge_title), onBack) {
        item {
            ChipGroupCard(stringResource(R.string.fastcharge_mode_title), modes, mode, enabled = modeSupported) {
                if (utils.setMode(it.toString())) mode = it
                else Toast.makeText(context, R.string.write_failed, Toast.LENGTH_SHORT).show()
            }
        }
        item {
            NoteText(stringResource(if (modeSupported) R.string.fastcharge_mode_note else R.string.fastcharge_mode_unavailable))
        }
        item {
            Group(
                { shape ->
                    SwitchRow(
                        shape = shape,
                        title = stringResource(R.string.fastcharge_usb_title),
                        summary = stringResource(if (usbSupported) R.string.fastcharge_usb_summary else R.string.fastcharge_usb_unavailable),
                        checked = usb,
                        enabled = usbSupported,
                    ) {
                        if (utils.setUsbEnabled(it)) usb = it
                        else Toast.makeText(context, R.string.write_failed, Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }
    }
}

private val ZramSizes = intArrayOf(0, 2, 4, 8, -1)
private val ZramAlgorithms = arrayOf("lz4", "lzo", "lzo-rle", "zstd")
private const val SWAPPINESS_DEFAULT = 100

@Composable
fun ZramScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val utils = remember { ZramUtils(context) }
    var size by remember {
        mutableIntStateOf(ZramSizes.indexOf(utils.currentZramSize).takeIf { it >= 0 } ?: ZramSizes.lastIndex)
    }
    var algorithm by remember {
        mutableIntStateOf(ZramAlgorithms.indexOf(utils.currentCompression).takeIf { it >= 0 } ?: ZramAlgorithms.lastIndex)
    }
    var swappiness by remember { mutableIntStateOf(utils.currentSwappiness) }
    var showRebootDialog by remember { mutableStateOf(false) }

    PartsPage(stringResource(R.string.zram_title), onBack) {
        item {
            ChipGroupCard(stringResource(R.string.zram_size_title), stringArrayResource(R.array.zram_size_entries).toList(), size) {
                if (it != size) {
                    utils.setZramSize(ZramSizes[it])
                    size = it
                    showRebootDialog = true
                }
            }
        }
        item {
            ChipGroupCard(stringResource(R.string.zram_compression_title), ZramAlgorithms.toList(), algorithm) {
                if (it != algorithm) {
                    utils.setCompressionAlgorithm(ZramAlgorithms[it])
                    algorithm = it
                    showRebootDialog = true
                }
            }
        }
        item {
            SliderCard(
                title = stringResource(R.string.swappiness_title),
                value = swappiness,
                range = 0..100,
                defaultValue = SWAPPINESS_DEFAULT,
                onValueChange = { swappiness = it },
                // Applied on release: the setter writes /proc and shows a toast each time.
                onValueChangeFinished = { utils.setSwappiness(it) },
            )
        }
        item { NoteText(stringResource(R.string.zram_note)) }
    }

    if (showRebootDialog) {
        AlertDialog(
            onDismissRequest = { showRebootDialog = false },
            confirmButton = {
                TextButton(onClick = { showRebootDialog = false }) { Text(stringResource(android.R.string.ok)) }
            },
            title = { Text(stringResource(R.string.zram_title)) },
            text = { Text(stringResource(R.string.zram_reboot_recommended), style = MaterialTheme.typography.bodyMedium) },
        )
    }
}
