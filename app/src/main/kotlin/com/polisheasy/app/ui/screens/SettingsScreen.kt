package com.polisheasy.app.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.polisheasy.app.R
import com.polisheasy.app.ui.appVersion
import com.polisheasy.app.ui.components.QuietButton
import com.polisheasy.app.ui.components.ScreenFrame
import com.polisheasy.app.ui.components.SecondaryButton
import com.polisheasy.app.ui.components.TopBar
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Settings: the Reminder's switch and time, the Sound switch, and "Report a mistake". Turning the Reminder on is the
 * only moment the app asks for permission to show notifications.
 */
@Composable
internal fun SettingsScreen(
    reminderEnabled: Boolean,
    reminderTime: LocalTime,
    notificationsAllowed: Boolean,
    soundOn: Boolean,
    onReminderEnabled: (Boolean) -> Unit,
    onReminderTime: (LocalTime) -> Unit,
    onSoundOn: (Boolean) -> Unit,
    onReport: () -> Unit,
) {
    val context = LocalContext.current
    // Whether the Learner refused the permission here, so the screen can say why the switch stayed off.
    var refused by rememberSaveable { mutableStateOf(false) }
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refused = !granted
        if (granted) onReminderEnabled(true)
    }
    ScreenFrame {
        TopBar(stringResource(R.string.settings_title))
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            SwitchRow(
                label = stringResource(R.string.settings_reminder),
                note = stringResource(R.string.settings_reminder_note),
                checked = reminderEnabled,
                onCheckedChange = { on ->
                    val mustAsk = on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    if (mustAsk) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else onReminderEnabled(on)
                },
            )
            if (!notificationsAllowed && (refused || reminderEnabled)) {
                Spacer(Modifier.height(Space.m))
                Text(
                    stringResource(R.string.settings_notifications_off),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(Space.xs))
                SecondaryButton(
                    stringResource(R.string.settings_open_android_settings),
                    onClick = { openNotificationSettings(context) },
                )
            }
            if (reminderEnabled) {
                Spacer(Modifier.height(Space.xs))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pickingTime = true }
                        .heightIn(min = Size.control),
                ) {
                    Text(
                        stringResource(R.string.settings_reminder_time),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(reminderTime),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(Space.m))
            SwitchRow(
                label = stringResource(R.string.settings_sound),
                note = stringResource(R.string.settings_sound_note),
                checked = soundOn,
                onCheckedChange = onSoundOn,
            )
            Spacer(Modifier.height(Space.m))
            HorizontalDivider()
            Spacer(Modifier.height(Space.m))
            SecondaryButton(stringResource(R.string.report_mistake), onReport)
            Spacer(Modifier.height(Space.l))
            Text(
                stringResource(R.string.settings_version, appVersion(context)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (pickingTime) {
        ReminderTimeDialog(
            reminderTime,
            onPicked = {
                pickingTime = false
                onReminderTime(it)
            },
            onDismiss = { pickingTime = false },
        )
    }
}

/** A setting that is on or off, with a line under it saying what it does. */
@Composable
private fun SwitchRow(label: String, note: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
    Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(time: LocalTime, onPicked: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(
        initialHour = time.hour,
        initialMinute = time.minute,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            QuietButton(stringResource(R.string.action_ok), onClick = { onPicked(LocalTime.of(state.hour, state.minute)) })
        },
        dismissButton = { QuietButton(stringResource(R.string.action_cancel), onDismiss) },
        title = { Text(stringResource(R.string.settings_reminder_time)) },
        text = { TimePicker(state) },
    )
}

/** Android's own notification settings for this app, where a refused permission can be given. */
private fun openNotificationSettings(context: Context) {
    val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // A phone without that page: nothing to open.
    }
}
