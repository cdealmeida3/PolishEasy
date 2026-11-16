package com.polisheasy.engine

import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * The next moment strictly after [now] at which the clock in [now]'s zone reads [at]. A time the
 * clocks skip when they go forward is moved past the change, as java.time does.
 */
fun nextReminderTime(now: ZonedDateTime, at: LocalTime): ZonedDateTime {
    val today = ZonedDateTime.of(now.toLocalDate(), at, now.zone)
    return if (today.isAfter(now)) today else ZonedDateTime.of(now.toLocalDate().plusDays(1), at, now.zone)
}
