package com.polisheasy.app

import android.content.Context
import java.time.LocalTime

/** The Learner's settings, kept in SharedPreferences so Android's backup carries them. */
class Settings(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Whether the Reminder is on. It is off until the Learner turns it on. */
    var reminderEnabled: Boolean
        get() = preferences.getBoolean(REMINDER_ENABLED, false)
        set(value) = preferences.edit().putBoolean(REMINDER_ENABLED, value).apply()

    /** The time of day of the Reminder, stored as minutes after midnight. */
    var reminderTime: LocalTime
        get() = LocalTime.ofSecondOfDay(
            preferences.getInt(REMINDER_MINUTES, DEFAULT_REMINDER_MINUTES).coerceIn(0, MINUTES_IN_A_DAY - 1) * 60L,
        )
        set(value) = preferences.edit().putInt(REMINDER_MINUTES, value.hour * 60 + value.minute).apply()

    /** Whether the app may play sound. On until the Learner turns it off. */
    var soundOn: Boolean
        get() = preferences.getBoolean(SOUND_ON, true)
        set(value) = preferences.edit().putBoolean(SOUND_ON, value).apply()

    /**
     * One a day: whether at most one new Daily Set opens per calendar day. Off until the Learner
     * turns it on, so they go at their own pace.
     */
    var oneADay: Boolean
        get() = preferences.getBoolean(ONE_A_DAY, false)
        set(value) = preferences.edit().putBoolean(ONE_A_DAY, value).apply()

    /** Whether the Learner has been through the welcome, which is shown once, before the first Daily Set. */
    var welcomed: Boolean
        get() = preferences.getBoolean(WELCOMED, false)
        set(value) = preferences.edit().putBoolean(WELCOMED, value).apply()

    /** Whether a Summary has offered the Reminder. It offers it once and never again. */
    var reminderOffered: Boolean
        get() = preferences.getBoolean(REMINDER_OFFERED, false)
        set(value) = preferences.edit().putBoolean(REMINDER_OFFERED, value).apply()

    /**
     * Whether the Learner refused the microphone when it was first asked for. Then no microphone
     * is offered, until Android's settings give the app the permission after all.
     */
    var microphoneRefused: Boolean
        get() = preferences.getBoolean(MICROPHONE_REFUSED, false)
        set(value) = preferences.edit().putBoolean(MICROPHONE_REFUSED, value).apply()

    private companion object {
        const val MICROPHONE_REFUSED = "microphone_refused"
        const val WELCOMED = "welcomed"
        const val REMINDER_OFFERED = "reminder_offered"
        const val SOUND_ON = "sound_on"
        const val ONE_A_DAY = "one_a_day"
        const val REMINDER_ENABLED = "reminder_enabled"
        const val REMINDER_MINUTES = "reminder_minutes"
        const val DEFAULT_REMINDER_MINUTES = 19 * 60
        const val MINUTES_IN_A_DAY = 24 * 60
    }
}
