package com.polisheasy.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.CardNote
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.ScreenFrame
import com.polisheasy.app.ui.components.StreakWeekCard
import com.polisheasy.app.ui.components.Teaches
import com.polisheasy.app.ui.components.TodayCard
import com.polisheasy.app.ui.components.UnitProgress
import com.polisheasy.app.ui.components.WeekDay
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.DayView
import com.polisheasy.engine.NextDailySetView
import com.polisheasy.engine.Today
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/**
 * Home: a greeting, the Streak with the last seven days, and what today holds. [next] is where the
 * Learner is on the Path, or null once every Daily Set released so far is completed. Each thing
 * [today] can be has its own card: ready, in progress, done for today, and the Review-only ones.
 */
@Composable
internal fun HomeScreen(
    today: Today,
    streak: Int,
    week: List<DayView>,
    next: NextDailySetView?,
    onStart: () -> Unit,
    onContinue: () -> Unit,
) {
    val done = today is Today.DoneForToday
    ScreenFrame {
        // Scrolls at a large font size; at the usual one everything fits.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(Space.xs))
            Text(
                stringResource(if (done) R.string.home_greeting_done else R.string.home_greeting),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(
                    when (today) {
                        Today.DailySetAvailable -> R.string.home_ready
                        Today.ReviewOnlyAvailable -> R.string.home_review_only_ready
                        is Today.InProgress -> R.string.home_in_progress_headline
                        is Today.DoneForToday -> R.string.home_done_headline
                        Today.AllCompleted -> R.string.home_all_completed_headline
                    },
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.l))
            Streak(streak, week, today)
            Spacer(Modifier.height(Space.m))
            when (today) {
                Today.DailySetAvailable -> NextDailySet(next, R.string.home_today, highlighted = true) {
                    Spacer(Modifier.height(Space.l))
                    PrimaryButton(stringResource(R.string.home_start), onStart)
                }
                is Today.InProgress -> {
                    val progress = today.progress
                    if (today.reviewOnly) {
                        ReviewOnly {
                            CardNote(stringResource(R.string.home_review_only_in_progress, progress.correct, progress.total))
                            Spacer(Modifier.height(Space.l))
                            PrimaryButton(stringResource(R.string.home_continue), onContinue)
                        }
                    } else {
                        NextDailySet(next, R.string.home_today, highlighted = true) {
                            Spacer(Modifier.height(Space.xs))
                            CardNote(stringResource(R.string.home_in_progress, progress.correct, progress.total))
                            Spacer(Modifier.height(Space.l))
                            PrimaryButton(stringResource(R.string.home_continue), onContinue)
                        }
                    }
                }
                Today.ReviewOnlyAvailable -> ReviewOnly {
                    CardNote(stringResource(R.string.home_review_only_note))
                    Spacer(Modifier.height(Space.l))
                    PrimaryButton(stringResource(R.string.home_start), onStart)
                }
                is Today.DoneForToday -> {
                    val unlockTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(today.nextUnlock)
                    val message = if (today.reviewOnlyNext) R.string.home_done_review_only_next else R.string.home_done
                    TodayCard(stringResource(R.string.home_done_title), done = true) {
                        CardNote(stringResource(message, unlockTime))
                    }
                    if (next != null) {
                        Spacer(Modifier.height(Space.m))
                        NextDailySet(next, R.string.home_up_next, highlighted = false)
                    }
                }
                Today.AllCompleted -> TodayCard(stringResource(R.string.home_all_completed_title), done = true) {
                    CardNote(stringResource(R.string.home_all_completed))
                }
            }
        }
    }
}

/** The Streak and its week. The note says what today's Daily Set would do for it. */
@Composable
private fun Streak(streak: Int, week: List<DayView>, today: Today) {
    val note = when (today) {
        Today.DailySetAvailable, Today.ReviewOnlyAvailable, is Today.InProgress ->
            if (streak == 0) R.string.home_streak_note_start else R.string.home_streak_note_keep
        is Today.DoneForToday -> R.string.home_streak_note_done
        Today.AllCompleted -> null
    }
    val completed = week.count { it.completed }
    StreakWeekCard(
        streak = streak,
        title = stringResource(R.string.streak_title),
        note = note?.let { stringResource(it) },
        days = week.mapIndexed { index, day ->
            WeekDay(
                label = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                completed = day.completed,
                today = index == week.lastIndex,
            )
        },
        description = pluralStringResource(R.plurals.home_streak, streak, streak) + ". " +
            pluralStringResource(R.plurals.home_week, completed, completed, week.size),
    )
}

/** The Daily Set the Learner is at: its number in its Unit, how far through the Unit that is, and what it teaches. */
@Composable
private fun NextDailySet(
    next: NextDailySetView?,
    label: Int,
    highlighted: Boolean,
    action: @Composable () -> Unit = {},
) {
    // Today offers a Daily Set only while the Path has one, so there is always a next one to show.
    if (next == null) return
    val completed = next.numberInUnit - 1
    TodayCard(
        title = stringResource(R.string.home_daily_set, next.numberInUnit, next.dailySetsInUnit),
        label = stringResource(label),
        highlighted = highlighted,
    ) {
        Spacer(Modifier.height(Space.xxs))
        UnitProgress(
            unit = stringResource(R.string.path_unit, next.unitNumber, next.unitTitle),
            completed = completed,
            dailySets = next.dailySetsInUnit,
            description = stringResource(R.string.home_unit_progress, completed, next.dailySetsInUnit),
        )
        if (next.teaches.isNotEmpty()) {
            Spacer(Modifier.height(Space.m))
            Teaches(stringResource(R.string.home_teaches), next.teaches)
        }
        action()
    }
}

@Composable
private fun ReviewOnly(content: @Composable () -> Unit) {
    TodayCard(
        title = stringResource(R.string.home_review_only_title),
        label = stringResource(R.string.home_today),
        highlighted = true,
    ) { content() }
}
