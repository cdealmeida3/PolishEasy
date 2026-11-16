package com.polisheasy.app

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlin.concurrent.thread

/**
 * Receives the Reminder's daily alarm, and the moments Android drops or shifts it: a restart, an
 * app update, a change of clock or time zone. Every one of them sets the next alarm; only the
 * alarm itself can show the notification, and only if the course engine says a Reminder is due.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // First, so that nothing going wrong below can stop tomorrow's Reminder.
        Reminder.arm(context)
        if (intent.action != Reminder.ACTION_REMIND) return
        if (!Settings(context).reminderEnabled || MainActivity.visible) return
        if (!context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()) return
        val application = context.applicationContext
        val pending = goAsync()
        // Loading the course reads files, so it stays off the main thread.
        thread {
            try {
                if (courseEngine(application).reminderDue()) Reminder.notify(application)
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Could not decide whether a Reminder is due", e)
            } finally {
                pending.finish()
            }
        }
    }
}
