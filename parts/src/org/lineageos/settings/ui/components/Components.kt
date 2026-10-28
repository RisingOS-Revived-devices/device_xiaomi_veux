/*
 * Copyright (C) 2026 acerhizmq
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lineageos.settings.R
import org.lineageos.settings.ui.theme.SectionTitleStyle
import kotlin.math.roundToInt

val ScreenPadding = 18.dp
private val CardShape = RoundedCornerShape(26.dp)
private val OuterRadius = 22.dp
private val InnerRadius = 6.dp
private const val DisabledAlpha = 0.45f

/** Shape of row [index] in a group of [count]: rounded outer corners, tight inner ones. */
fun groupShape(index: Int, count: Int): Shape = when {
    count == 1 -> RoundedCornerShape(OuterRadius)
    index == 0 -> RoundedCornerShape(OuterRadius, OuterRadius, InnerRadius, InnerRadius)
    index == count - 1 -> RoundedCornerShape(InnerRadius, InnerRadius, OuterRadius, OuterRadius)
    else -> RoundedCornerShape(InnerRadius)
}

/** Full-screen page with the round back button and large title. */
@Composable
fun PartsPage(title: String, onBack: () -> Unit, content: LazyListScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = ScreenPadding),
        ) {
            item {
                Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
                ) {
                    RoundIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.navigate_up),
                        onClick = onBack,
                        size = 48.dp,
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            content()
            item {
                Spacer(Modifier.height(24.dp))
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}

@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    size: Dp = 40.dp,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Icon(icon, contentDescription = contentDescription, tint = content, modifier = Modifier.size(size * 0.46f))
    }
}

@Composable
fun IconTile(icon: ImageVector, size: Dp = 46.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.34f))
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = SectionTitleStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 6.dp, top = 22.dp, bottom = 10.dp),
    )
}

/** Plain rounded card on the container surface. */
@Composable
fun PartsCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(CardShape)
            .background(color)
            .then(clickModifier)
            .padding(contentPadding),
        content = content,
    )
}

/** Dashboard feature card: icon tile, title, live summary and arrow. */
@Composable
fun FeatureCard(
    icon: ImageVector,
    title: String,
    summary: String,
    tall: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
    ) {
        IconTile(icon)
        Spacer(Modifier.height(if (tall) 28.dp else 16.dp))
        Text(
            text = title,
            style = if (tall) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (tall) {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** Dashboard quick toggle; fills with the accent color when on. */
@Composable
fun RowScope.QuickToggle(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val container by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
        label = "quickToggle",
    )
    val contentColor = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(22.dp))
            .background(container)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onToggle)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .padding(vertical = 14.dp, horizontal = 4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (checked) contentColor else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Large on/off card at the top of a feature page; turns accent-colored when on. */
@Composable
fun MainSwitchCard(title: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    val container by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "mainSwitch",
    )
    val contentColor = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(CardShape)
            .background(container)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), color = contentColor, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.onPrimary,
                checkedBorderColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
    }
}

/** Rows stacked with small gaps; outer corners rounded. */
@Composable
fun Group(vararg rows: @Composable (Shape) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(bottom = 12.dp)) {
        rows.forEachIndexed { index, row -> row(groupShape(index, rows.size)) }
    }
}

@Composable
private fun RowText(title: String, summary: String?, modifier: Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (!summary.isNullOrEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SwitchRow(
    shape: Shape,
    title: String,
    summary: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .heightIn(min = 64.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        RowText(title, summary, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
fun InfoRow(shape: Shape, title: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .heightIn(min = 60.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ValuePill(text: String, @DrawableRes icon: Int? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        if (icon != null) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Themed inline picker: the row shows the current value as a pill; tapping it expands the
 * options right below in the theme colors. The platform dropdown is never used.
 */
@Composable
fun ChoiceRow(
    shape: Shape,
    title: String,
    options: List<String>,
    selected: Int,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    showChevron: Boolean = true,
    optionIcons: List<Int>? = null,
    onSelect: (Int) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .alpha(if (enabled) 1f else DisabledAlpha),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, role = Role.DropdownList) { expanded = !expanded }
                .heightIn(min = 64.dp)
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(14.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            ValuePill(options.getOrElse(selected) { options.firstOrNull().orEmpty() }, optionIcons?.getOrNull(selected))
            if (showChevron) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(rotation),
                )
            }
        }
        AnimatedVisibility(
            visible = expanded && enabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 12.dp),
            ) {
                options.forEachIndexed { index, option ->
                    val isSelected = index == selected
                    val optionShape = when {
                        options.size == 1 -> RoundedCornerShape(16.dp)
                        index == 0 -> RoundedCornerShape(16.dp, 16.dp, 6.dp, 6.dp)
                        index == options.size - 1 -> RoundedCornerShape(6.dp, 6.dp, 16.dp, 16.dp)
                        else -> RoundedCornerShape(6.dp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(optionShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            .selectable(selected = isSelected, role = Role.RadioButton) {
                                onSelect(index)
                                expanded = false
                            }
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        optionIcons?.getOrNull(index)?.let { icon ->
                            Icon(
                                painterResource(icon),
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionRow(shape: Shape, title: String, summary: String? = null, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        RowText(title, summary, Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Single-choice chips for short option lists. */
@Composable
fun ChipGroupCard(
    title: String,
    options: List<String>,
    selected: Int,
    enabled: Boolean = true,
    onSelect: (Int) -> Unit,
) {
    PartsCard(modifier = Modifier.alpha(if (enabled) 1f else DisabledAlpha)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selected
                val bg by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    label = "chip",
                )
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(bg)
                        .selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton) { onSelect(index) }
                        .heightIn(min = 40.dp)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }
}

/** Themed slider with live value, -/+ steps and reset to default. */
@Composable
fun SliderCard(
    title: String,
    value: Int,
    range: IntRange,
    step: Int = 1,
    defaultValue: Int? = null,
    enabled: Boolean = true,
    format: (Int) -> String = { it.toString() },
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: (Int) -> Unit = {},
) {
    fun clamp(v: Int) = v.coerceIn(range.first, range.last)
    PartsCard(modifier = Modifier.alpha(if (enabled) 1f else DisabledAlpha)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(format(value), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(
                icon = Icons.Rounded.Remove,
                contentDescription = stringResource(R.string.slider_decrease),
                onClick = { if (enabled) clamp(value - step).let { onValueChange(it); onValueChangeFinished(it) } },
                size = 36.dp,
                container = MaterialTheme.colorScheme.surfaceContainerHighest,
                content = MaterialTheme.colorScheme.onSurface,
            )
            val steps = ((range.last - range.first) / step - 1).coerceAtLeast(0)
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(clamp(((it - range.first) / step).roundToInt() * step + range.first)) },
                onValueChangeFinished = { onValueChangeFinished(value) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = if (steps <= 200) steps else 0,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            RoundIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.slider_increase),
                onClick = { if (enabled) clamp(value + step).let { onValueChange(it); onValueChangeFinished(it) } },
                size = 36.dp,
                container = MaterialTheme.colorScheme.surfaceContainerHighest,
                content = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (defaultValue != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = enabled && value != defaultValue, role = Role.Button) {
                        onValueChange(defaultValue)
                        onValueChangeFinished(defaultValue)
                    }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
            ) {
                Icon(Icons.Rounded.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.slider_reset, format(defaultValue)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun NoteText(text: String) {
    Row(modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 14.dp)) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun WarningCard(text: String) {
    PartsCard(color = MaterialTheme.colorScheme.primaryContainer) {
        Row {
            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Apply (accent) and Reset buttons used by the kernel and GPU managers. */
@Composable
fun ApplyResetButtons(onApply: () -> Unit, onReset: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)) {
        PillButton(stringResource(R.string.action_apply), primary = true, onClick = onApply, modifier = Modifier.weight(1f))
        PillButton(stringResource(R.string.action_reset), primary = false, onClick = onReset, modifier = Modifier.weight(1f))
    }
}

@Composable
fun PillButton(text: String, primary: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        val color = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, color = color)
    }
}
