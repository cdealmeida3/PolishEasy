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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.polisheasy.app.R
import com.polisheasy.app.ui.appVersion
import com.polisheasy.app.ui.components.LinkSetting
import com.polisheasy.app.ui.components.QuietButton
import com.polisheasy.app.ui.components.ScreenFrame
import com.polisheasy.app.ui.components.SettingDivider
import com.polisheasy.app.ui.components.SettingNotice
import com.polisheasy.app.ui.components.SettingsCard
import com.polisheasy.app.ui.components.SettingsFootnote
import com.polisheasy.app.ui.components.SwitchSetting
import com.polisheasy.app.ui.components.TopBar
import com.polisheasy.app.ui.components.ValueSetting
import com.polisheasy.app.ui.theme.Space
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Settings: a card for the Reminder's switch and time, one for the Sound switch, and one for "Report a mistake". Turning the Reminder on is the
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
            SettingsCard {
                SwitchSetting(
                    icon = R.drawable.ic_notifications,
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
                    SettingNotice(
                        text = stringResource(R.string.settings_notifications_off),
                        action = stringResource(R.string.settings_open_android_settings),
                        onAction = { openNotificationSettings(context) },
                    )
                }
                if (reminderEnabled) {
                    SettingDivider()
                    ValueSetting(
                        icon = R.drawable.ic_schedule,
                        label = stringResource(R.string.settings_reminder_time),
                        value = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(reminderTime),
                        onClick = { pickingTime = true },
                    )
                }
            }
            Spacer(Modifier.height(Space.m))
            SettingsCard {
                SwitchSetting(
                    icon = R.drawable.ic_volume_up,
                    label = stringResource(R.string.settings_sound),
                    note = stringResource(R.string.settings_sound_note),
                    checked = soundOn,
                    onCheckedChange = onSoundOn,
                )
            }
            Spacer(Modifier.height(Space.m))
            SettingsCard {
                LinkSetting(
                    icon = R.drawable.ic_flag,
                    label = stringResource(R.string.report_mistake),
                    note = stringResource(R.string.settings_report_note),
                    onClick = onReport,
                )
            }
            Spacer(Modifier.height(Space.l))
            SettingsFootnote(stringResource(R.string.settings_version, appVersion(context)))
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
