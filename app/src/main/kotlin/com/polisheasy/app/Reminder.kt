package com.polisheasy.app

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.polisheasy.engine.nextReminderTime
import java.time.ZonedDateTime

/**
 * The Reminder's alarm and notification. Whether a Reminder is due is the course engine's rule;
 * this only sets the daily alarm and shows or removes the one notification.
 */
object Reminder {
    /** The alarm going off. */
    const val ACTION_REMIND = "com.polisheasy.app.REMIND"

    /** The notification being tapped. */
    const val ACTION_OPEN_HOME = "com.polisheasy.app.OPEN_HOME"

    private const val CHANNEL = "reminder"
    private const val NOTIFICATION = 1

    /**
     * Sets the alarm for the next time the clock reads the Learner's Reminder time, or removes it
     * and the notification when the Reminder is off. Safe to call at any moment: there is only
     * ever one alarm, replaced each time.
     *
     * The alarm is inexact on purpose (ADR-0006): Android may deliver it late, and it needs no
     * permission.
     */
    fun arm(context: Context) {
        val settings = Settings(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val alarm = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (!settings.reminderEnabled) {
            alarmManager.cancel(alarm)
            dismiss(context)
            return
        }
        val next = nextReminderTime(ZonedDateTime.now(), settings.reminderTime)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), alarm)
    }

    /** Shows the Reminder notification, replacing the one already there. Tapping it opens Home. */
    fun notify(context: Context) {
        val notifications = context.getSystemService(NotificationManager::class.java)
        // Creating a channel that exists changes nothing.
        notifications.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val openHome = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_OPEN_HOME)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
                ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        notifications.notify(
            NOTIFICATION,
            Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.reminder_title))
                .setContentText(context.getString(R.string.reminder_text))
                .setCategory(Notification.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(openHome)
                .build(),
        )
    }

    /** Removes the Reminder notification, if it is showing. */
    fun dismiss(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION)
    }
}
