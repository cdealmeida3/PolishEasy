# The Reminder uses an inexact alarm

The Reminder is one alarm, set with `AlarmManager.setAndAllowWhileIdle` for the next time the clock reads the Learner's chosen time. When it goes off, the app asks the course engine whether a Reminder is due, shows the notification if so, and sets the alarm for the next day. The same alarm is set again when a setting changes, when the app opens, and after a restart, an app update or a change of clock or time zone.

The alarm is inexact on purpose. An exact alarm needs `SCHEDULE_EXACT_ALARM`, which Android 14 denies by default and the Learner would have to grant on a special settings page, or `USE_EXACT_ALARM`, which Google Play allows only for alarm clock and calendar apps. An inexact alarm needs no permission, and for a daily nudge a late delivery does no harm.

We rejected WorkManager as well: it is a new dependency whose periodic work drifts by hours, for something one alarm does.

## Consequences

- The Reminder can arrive late, usually by minutes and by up to about an hour. `AllowWhileIdle` means a phone lying still is still reached.
- No special permission and no Google Play declaration. The only permissions are for showing notifications and for hearing that the phone restarted.
- Whether to notify is decided when the alarm goes off, not when it is set, so a Daily Set completed a minute before the Reminder time silences it with nothing to cancel.
- An app the Learner force-stopped gets no Reminder until it is opened again. That is an Android rule.
- Moving to an exact alarm later is a change to one call plus a permission.
