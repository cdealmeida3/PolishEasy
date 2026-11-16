package com.polisheasy.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.StreakRaised
import com.polisheasy.app.ui.components.Confetti
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.ItemCard
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.ScreenFrame
import com.polisheasy.app.ui.components.SpeakerButton
import com.polisheasy.app.ui.components.StatRow
import com.polisheasy.app.ui.components.StatTile
import com.polisheasy.app.ui.components.StreakHero
import com.polisheasy.app.ui.components.fadingBottom
import com.polisheasy.app.ui.components.rememberAnswerHaptics
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.ClipView
import com.polisheasy.engine.DayView
import com.polisheasy.engine.ItemView
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.polisheasy.app.ReminderOffer
import com.polisheasy.app.ui.components.OfferCard
import com.polisheasy.engine.UnitCompletedView

/** The Streaks that get a bigger celebration, once each: a week, a month and a hundred days. */
private val MILESTONES = mapOf(
    7 to R.string.summary_streak_milestone_week,
    30 to R.string.summary_streak_milestone_month,
    100 to R.string.summary_streak_milestone_hundred,
)

/**
 * The end of a Daily Set, as a celebration and not a grade: a cheer in Polish, the Streak it
 * raised as the hero with [week]'s days, then the score and the new words as small facts, and what
 * was learned, each word with its speaker when [clipOf] has a Clip for it. A Replay raises no
 * Streak, lists no Items and gets no confetti. With [reminder] the Summary offers the Reminder,
 * here where the Learner has a reason to want one; [onRemind] takes it.
 */
@Composable
internal fun SummaryScreen(
    correctAnswers: Int,
    exercises: Int,
    itemsLearned: List<ItemView>,
    reviewOnly: Boolean,
    replay: Boolean,
    streakRaised: StreakRaised?,
    week: List<DayView>,
    clipOf: (String) -> ClipView?,
    onPlay: (ClipView) -> Unit,
    onDone: () -> Unit,
    reminder: ReminderOffer? = null,
    onRemind: () -> Unit = {},
    unitCompleted: UnitCompletedView? = null,
) {
    val context = LocalContext.current
    // Whether the Learner refused the permission here, so the offer can say where to turn it on later.
    var refused by rememberSaveable { mutableStateOf(false) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refused = !granted
        if (granted) onRemind()
    }
    val haptics = rememberAnswerHaptics()
    LaunchedEffect(Unit) { haptics.finished() }
    val milestone = streakRaised?.let { MILESTONES[it.to] }
    // The cheer changes from day to day, and each one is a word of Polish learned.
    val cheers = stringArrayResource(R.array.summary_cheers)
    val meanings = stringArrayResource(R.array.summary_cheer_meanings)
    val cheer = (exercises + correctAnswers + (streakRaised?.to ?: 0)).mod(cheers.size)
    val scroll = rememberScrollState()
    ScreenFrame(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Scrolls when the list of Items is long, and fades out above the button, which stays on screen.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .fadingBottom(more = scroll.canScrollForward)
                    .verticalScroll(scroll),
            ) {
                Spacer(Modifier.height(Space.m))
                Text(
                    cheers[cheer],
                    style = MaterialTheme.typography.displaySmall.inPolish(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    meanings[cheer],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    when {
                        replay -> stringResource(R.string.summary_replay_title)
                        reviewOnly -> stringResource(R.string.summary_review_only_title)
                        // The Daily Set that ends a Unit says so: finishing a Unit is the bigger thing.
                        unitCompleted != null -> stringResource(R.string.summary_unit_complete, unitCompleted.number)
                        else -> stringResource(R.string.summary_title)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                if (unitCompleted != null && !replay && !reviewOnly) {
                    Text(
                        unitCompleted.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                if (streakRaised != null) {
                    Spacer(Modifier.height(Space.l))
                    StreakHero(
                        from = streakRaised.from,
                        to = streakRaised.to,
                        title = stringResource(R.string.streak_title),
                        note = stringResource(
                            milestone ?: if (streakRaised.to == 1) R.string.summary_streak_note_first else R.string.summary_streak_note,
                        ),
                        days = weekDays(week),
                        description = pluralStringResource(R.plurals.home_streak, streakRaised.to, streakRaised.to),
                    )
                }
                Spacer(Modifier.height(Space.m))
                val firstTry = stringResource(R.string.summary_accuracy, correctAnswers, exercises)
                val words = pluralStringResource(R.plurals.summary_new_words, itemsLearned.size, itemsLearned.size)
                StatRow(
                    tiles = buildList<@Composable (Modifier) -> Unit> {
                        add { modifier ->
                            StatTile(
                                value = stringResource(R.string.summary_first_try_value, correctAnswers, exercises),
                                label = stringResource(R.string.summary_first_try),
                                description = firstTry,
                                modifier = modifier,
                            )
                        }
                        if (itemsLearned.isNotEmpty()) {
                            add { modifier ->
                                StatTile(
                                    value = itemsLearned.size.toString(),
                                    label = pluralStringResource(R.plurals.summary_new_words_label, itemsLearned.size),
                                    description = words,
                                    modifier = modifier,
                                )
                            }
                        }
                    },
                )
                if (reviewOnly) {
                    Spacer(Modifier.height(Space.l))
                    Text(
                        stringResource(R.string.summary_more_units),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                if (reminder != null) {
                    Spacer(Modifier.height(Space.m))
                    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(reminder.time)
                    when {
                        reminder.on -> OfferCard(
                            R.drawable.ic_notifications,
                            stringResource(R.string.summary_reminder_on_title, time),
                            stringResource(R.string.summary_reminder_on_note),
                        )
                        refused -> OfferCard(
                            R.drawable.ic_notifications,
                            stringResource(R.string.summary_reminder_refused_title),
                            stringResource(R.string.summary_reminder_refused_note),
                        )
                        else -> OfferCard(
                            R.drawable.ic_notifications,
                            stringResource(R.string.summary_reminder_title),
                            stringResource(R.string.summary_reminder_note, time),
                            action = stringResource(R.string.summary_reminder_action),
                            onAction = {
                                val mustAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                                    PackageManager.PERMISSION_GRANTED
                                if (mustAsk) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else onRemind()
                            },
                        )
                    }
                }
                if (itemsLearned.isNotEmpty()) {
                    Spacer(Modifier.height(Space.l))
                    Instruction(stringResource(R.string.summary_items_learned), modifier = Modifier.fillMaxWidth())
                    itemsLearned.forEach { item ->
                        Spacer(Modifier.height(Space.xs))
                        val clip = clipOf(item.text)
                        ItemCard(
                            item.text,
                            item.meaning,
                            speaker = clip?.let {
                                { SpeakerButton(stringResource(R.string.audio_play_word, item.text), onClick = { onPlay(it) }) }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(Space.m))
            }
            if (!replay) Confetti(Modifier.matchParentSize(), plenty = milestone != null || unitCompleted != null)
        }
        Spacer(Modifier.height(Space.m))
        PrimaryButton(stringResource(if (replay) R.string.summary_back_path else R.string.summary_back_home), onDone)
    }
}
