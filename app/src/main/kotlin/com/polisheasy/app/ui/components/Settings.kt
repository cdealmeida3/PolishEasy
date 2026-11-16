package com.polisheasy.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.polisheasy.app.R
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.Scaled

private val Badge = 40.dp

/** A group of settings that belong together: a card whose rows reach its edges. Put a [SettingDivider] between rows. */
@Composable
fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    ContentCard(modifier = modifier, padded = false, content = content)
}

/** The line between two rows of a [SettingsCard], starting where their words start. */
@Composable
fun SettingDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = modifier.padding(start = Space.m + Badge + Space.m),
    )
}

/**
 * A setting that is on or off: its [icon], its [label], a [note] saying what it does, and a switch.
 * The whole row is the switch, for a finger and for a screen reader, which hears the label, the
 * note and whether it is on as one thing.
 */
@Composable
fun SwitchSetting(
    @DrawableRes icon: Int,
    label: String,
    note: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val colors = PolishEasyTheme.colors
    SettingRow(icon, modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)) {
        // On is the brand's colour in both themes, like the main button. The row handles the tap.
        val switch = @Composable {
            Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colors.brand,
                checkedThumbColor = colors.onBrand,
                uncheckedTrackColor = scheme.surfaceContainerHighest,
                uncheckedThumbColor = scheme.outline,
                uncheckedBorderColor = scheme.outline,
            ),
        )
        }
        if (Scaled.roomy()) {
            // At a large font size the switch goes under the words, which then have the row's width.
            Column(modifier = Modifier.weight(1f)) {
                SettingWords(label, note, Modifier)
                Spacer(Modifier.height(Space.xs))
                switch()
            }
        } else {
            SettingWords(label, note, Modifier.weight(1f))
            Spacer(Modifier.width(Space.m))
            switch()
        }
    }
}

/** A setting with a [value] to change, such as a time: tapping the row opens whatever changes it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ValueSetting(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    SettingRow(icon, modifier.clickable(role = Role.Button, onClick = onClick)) {
        // Side by side while both fit; at a large font size the value goes under the label.
        FlowRow(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(Space.xs),
            itemVerticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSecondaryContainer,
                modifier = Modifier
                    .background(scheme.secondaryContainer, MaterialTheme.shapes.small)
                    .padding(horizontal = Space.s, vertical = Space.xs),
            )
        }
    }
}

/** A row that leads somewhere else, such as the email app: its [icon], [label] and [note], and an arrow. */
@Composable
fun LinkSetting(
    @DrawableRes icon: Int,
    label: String,
    note: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(icon, modifier.clickable(role = Role.Button, onClick = onClick)) {
        SettingWords(label, note, Modifier.weight(1f))
        Spacer(Modifier.width(Space.xs))
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Scaled.icon()),
        )
    }
}

/**
 * Something in the way of a setting, inside its [SettingsCard], with the one [action] that fixes
 * it. It is a caution, not an error: nothing broke.
 */
@Composable
fun SettingNotice(text: String, action: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PolishEasyTheme.colors
    Column(
        modifier = modifier
            .padding(start = Space.m, end = Space.m, bottom = Space.m)
            .fillMaxWidth()
            .background(colors.cautionContainer, MaterialTheme.shapes.medium)
            .padding(Space.m),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = colors.onCautionContainer)
        Spacer(Modifier.height(Space.s))
        SecondaryButton(action, onAction)
    }
}

/** A quiet line under the settings, such as the app's version. */
@Composable
fun SettingsFootnote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

/** What every row of a [SettingsCard] shares: its height, its space, and its icon on a round badge. */
@Composable
private fun SettingRow(@DrawableRes icon: Int, modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Size.control)
            .padding(horizontal = Space.m, vertical = Space.s),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(Badge).background(scheme.primaryContainer, CircleShape)) {
            Icon(painterResource(icon), contentDescription = null, tint = scheme.primary)
        }
        Spacer(Modifier.width(Space.m))
        content()
    }
}

@Composable
private fun SettingWords(label: String, note: String?, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
        if (note != null) Text(note, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
    }
}
