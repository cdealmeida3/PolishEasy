package com.polisheasy.app.ui.screens.lesson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.ChipRow
import com.polisheasy.app.ui.components.DetailFrame
import com.polisheasy.app.ui.components.LessonBanner
import com.polisheasy.app.ui.components.Point
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.QuietButton
import com.polisheasy.app.ui.components.StopState
import com.polisheasy.app.ui.components.StrandChip
import com.polisheasy.app.ui.components.Teaches
import com.polisheasy.app.ui.components.ToolTile
import com.polisheasy.app.ui.components.TopBar
import com.polisheasy.app.ui.components.TrailMilestone
import com.polisheasy.app.ui.components.TrailStop
import com.polisheasy.app.ui.components.WordChip
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.ClipView
import com.polisheasy.engine.DailySetState
import com.polisheasy.engine.LessonView
import com.polisheasy.engine.NextDailySetView
import com.polisheasy.engine.PathView
import com.polisheasy.engine.SectionView
import com.polisheasy.engine.Strand
import com.polisheasy.engine.Register

/**
 * A chapter's Lesson screen, laid out like the book's opening page: the banner in the chapter's
 * colour with its number and title, what it teaches as K, L and G (each filling as its sections
 * are completed), "Sprawdź w słowniku" with its look-up words, its sections as a trail, and its
 * Powtórzenie: the Słownik, the Fiszki and the Test. A chapter still being written shows what it
 * will teach and says it is on its way.
 *
 * The sections follow the same rules as on Home: today's is started or continued, a completed one
 * replayed, and at the Learner's own pace any other one started, after asking first when another
 * Daily Set is in progress ([inProgress]), since its progress would be lost. With [oneADay] the
 * ones ahead are locked and the next one unlocks tomorrow.
 */
@Composable
internal fun LessonScreen(
    lesson: LessonView,
    path: PathView,
    onSection: (String) -> Unit,
    onDictionary: () -> Unit,
    onFlashcards: () -> Unit,
    onTest: () -> Unit,
    onBack: () -> Unit,
    onPlay: (ClipView) -> Unit,
    today: NextDailySetView? = null,
    started: Boolean = false,
    oneADay: Boolean = true,
    inProgress: String? = null,
    sound: Boolean = true,
) {
    // The section chosen while another Daily Set is in progress, waiting for the Learner to say it may replace it.
    var replacing by rememberSaveable { mutableStateOf<String?>(null) }
    val stops = path.levels.flatMap { level -> level.units.flatMap { it.dailySets } }
    // The stop the Learner is at on the whole Path: today's, or with none today the first not completed.
    val at = stops.firstOrNull { it.state == DailySetState.TODAY } ?: stops.firstOrNull { it.state != DailySetState.COMPLETED }
    val choose: (SectionView) -> Unit = { section ->
        if (section.state == DailySetState.OPEN && started && inProgress != section.dailySetId) replacing = section.dailySetId else onSection(section.dailySetId)
    }
    DetailFrame(backLabel = stringResource(R.string.lesson_back), onBack = onBack) {
        val number = lesson.number
        LessonBanner(
            label = if (number != null) stringResource(R.string.lesson_label, number) else stringResource(R.string.path_unit_number, lesson.place),
            number = (number ?: lesson.place).toString(),
            title = lesson.title,
            subtitle = lesson.subtitle,
            accent = lesson.accent,
        )
        lesson.strands.forEach { strand ->
            Spacer(Modifier.height(Space.xs))
            val name = stringResource(
                when (strand.strand) {
                    Strand.COMMUNICATION -> R.string.lesson_strand_k
                    Strand.VOCABULARY -> R.string.lesson_strand_l
                    Strand.GRAMMAR -> R.string.lesson_strand_g
                },
            )
            val topics = strand.topics.joinToString(", ")
            StrandChip(
                mark = strand.strand.mark,
                name = name,
                topics = topics,
                done = strand.done,
                total = strand.total,
                accent = lesson.accent,
                description = stringResource(R.string.lesson_strand_description, name, topics, strand.done, strand.total),
            )
        }
        if (lesson.lookUp.isNotEmpty()) {
            Spacer(Modifier.height(Space.l))
            TopBar(stringResource(R.string.lesson_look_up), note = stringResource(R.string.lesson_look_up_note))
            var open by rememberSaveable { mutableStateOf(setOf<String>()) }
            ChipRow {
                lesson.lookUp.forEach { word ->
                    WordChip(
                        text = word.text,
                        meaning = word.meaning,
                        open = word.itemId in open,
                        onClick = {
                            open = open + word.itemId
                            if (sound) word.audio?.let(onPlay)
                        },
                        onClickLabel = stringResource(R.string.lesson_word_open),
                        accent = lesson.accent,
                    )
                }
            }
        }
        Spacer(Modifier.height(Space.l))
        if (lesson.coming) {
            Point(R.drawable.ic_menu_book, stringResource(R.string.lesson_coming_title), stringResource(R.string.lesson_coming_note))
            return@DetailFrame
        }
        TopBar(stringResource(R.string.lesson_sections))
        lesson.sections.forEachIndexed { index, section ->
            SectionStop(lesson, section, index, at?.id, today, started, oneADay, onSection = { choose(section) })
        }
        val completed = lesson.sections.count { it.state == DailySetState.COMPLETED }
        TrailMilestone(
            title = if (number != null) {
                stringResource(if (completed == lesson.sections.size) R.string.lesson_complete else R.string.lesson_end, lessonName(lesson))
            } else {
                stringResource(if (completed == lesson.sections.size) R.string.path_unit_complete else R.string.path_unit_end, lesson.place)
            },
            reached = completed == lesson.sections.size,
        )
        Spacer(Modifier.height(Space.l))
        TopBar(stringResource(R.string.lesson_review), note = stringResource(R.string.lesson_review_note))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            ToolTile(
                R.drawable.ic_menu_book,
                stringResource(R.string.lesson_dictionary),
                pluralStringResource(R.plurals.lesson_dictionary_note, lesson.words, lesson.words),
                accent = lesson.accent,
                onClick = onDictionary,
                onClickLabel = stringResource(R.string.lesson_dictionary_open),
                modifier = Modifier.weight(1f),
            )
            ToolTile(
                R.drawable.ic_style,
                stringResource(R.string.lesson_flashcards),
                stringResource(R.string.lesson_flashcards_note),
                accent = lesson.accent,
                onClick = onFlashcards,
                onClickLabel = stringResource(R.string.lesson_flashcards_open),
                modifier = Modifier.weight(1f),
            )
            val best = lesson.checkpoint.best
            ToolTile(
                R.drawable.ic_quiz,
                stringResource(R.string.lesson_test),
                if (best != null) {
                    stringResource(R.string.lesson_test_best, best)
                } else {
                    pluralStringResource(R.plurals.lesson_test_note, lesson.checkpoint.questions, lesson.checkpoint.questions)
                },
                accent = lesson.accent,
                onClick = onTest,
                onClickLabel = stringResource(R.string.lesson_test_open),
                modifier = Modifier.weight(1f),
                sealed = lesson.checkpoint.sealed,
                sealedLabel = stringResource(R.string.lesson_sealed),
            )
        }
    }
    replacing?.let { id ->
        val name = lesson.sections.firstOrNull { it.dailySetId == inProgress }?.let { it.heading ?: stringResource(R.string.path_daily_set, lesson.sections.indexOf(it) + 1) }
            ?: inProgress?.let { progressing -> stops.firstOrNull { it.id == progressing }?.title }
            ?: stringResource(R.string.path_daily_set, 1)
        AlertDialog(
            onDismissRequest = { replacing = null },
            confirmButton = {
                QuietButton(stringResource(R.string.path_replace_start), onClick = {
                    replacing = null
                    onSection(id)
                })
            },
            dismissButton = { QuietButton(stringResource(R.string.action_cancel), { replacing = null }) },
            title = { Text(stringResource(R.string.path_replace_title)) },
            text = { Text(stringResource(R.string.path_replace_note, name)) },
        )
    }
}

/**
 * One section on the chapter's trail. [at] is the Daily Set the Learner is at on the whole Path.
 * Today's stop has the room: what else it teaches, and the button that starts it, or continues it
 * once [started].
 */
@Composable
private fun SectionStop(
    lesson: LessonView,
    section: SectionView,
    index: Int,
    at: String?,
    today: NextDailySetView?,
    started: Boolean,
    oneADay: Boolean,
    onSection: () -> Unit,
) {
    val sections = lesson.sections
    val state = section.state
    // Its number in its chapter, the same one Home shows.
    val number = stringResource(R.string.path_stop_number, index + 1, sections.size)
    val heading = section.heading
    // The book's sections have a letter, A, B, C, which goes before the heading.
    val title = when {
        heading == null -> stringResource(R.string.path_daily_set, index + 1)
        section.letter.first().isLetter() -> stringResource(R.string.lesson_section_title, section.letter, heading)
        else -> heading
    }
    val next = at == section.dailySetId && state == DailySetState.LOCKED
    val says = when {
        state == DailySetState.COMPLETED -> stringResource(R.string.path_completed)
        state == DailySetState.TODAY -> null
        next -> stringResource(R.string.path_unlocks_tomorrow)
        // The padlock says it to the eye; a screen reader is told below.
        else -> null
    }
    // A Daily Set with no heading has no title of its own, so its number stands in for one.
    val first = if (heading == null) null else section.meaning ?: number
    TrailStop(
        state = when (state) {
            DailySetState.COMPLETED -> StopState.Completed
            DailySetState.TODAY -> StopState.Today
            DailySetState.OPEN -> StopState.Open
            DailySetState.LOCKED -> StopState.Locked
        },
        title = title,
        note = listOfNotNull(first, says).takeIf { it.isNotEmpty() }?.let { parts ->
            if (parts.size == 2) stringResource(R.string.path_stop_note, parts[0], parts[1]) else parts[0]
        },
        first = index == 0,
        // The trail runs on from the last stop to the chapter's milestone.
        last = false,
        // The trail is walked as far as today's stop, and no further.
        walkedAbove = state == DailySetState.COMPLETED || state == DailySetState.TODAY,
        walkedBelow = state == DailySetState.COMPLETED && sections.getOrNull(index + 1)?.state == DailySetState.TODAY ||
            sections.getOrNull(index + 1)?.state == DailySetState.COMPLETED ||
            index == sections.lastIndex && state == DailySetState.COMPLETED,
        label = when {
            // At the Learner's own pace the stop they are at is only the suggested next one, until it is started.
            state == DailySetState.TODAY && !oneADay && !started -> stringResource(R.string.home_up_next)
            state == DailySetState.TODAY -> stringResource(R.string.path_today)
            next -> stringResource(R.string.home_up_next)
            else -> null
        },
        action = if (state == DailySetState.COMPLETED) R.drawable.ic_replay else null,
        onClickLabel = when (state) {
            DailySetState.COMPLETED -> stringResource(R.string.path_replay)
            DailySetState.TODAY, DailySetState.OPEN -> stringResource(R.string.home_start)
            DailySetState.LOCKED -> null
        },
        onClick = if (state == DailySetState.LOCKED) null else onSection,
        compact = state == DailySetState.COMPLETED,
        said = when {
            state == DailySetState.LOCKED && !next -> stringResource(R.string.path_locked)
            // The plain node says it to the eye.
            state == DailySetState.OPEN -> stringResource(R.string.path_open)
            else -> null
        },
        detail = if (state == DailySetState.TODAY) {
            {
                val alsoTeaches = today?.teaches.orEmpty().drop(1)
                if (alsoTeaches.isNotEmpty()) {
                    Teaches(stringResource(R.string.home_teaches), alsoTeaches)
                    Spacer(Modifier.height(Space.m))
                }
                PrimaryButton(stringResource(if (started) R.string.home_continue else R.string.home_start), onSection)
                Spacer(Modifier.height(Space.xs))
            }
        } else {
            null
        },
    )
}

/** What the book's register mark means, for a screen reader and beside a phrase. */
@Composable
internal fun registerWords(register: Register): String = stringResource(
    when (register) {
        Register.FORMAL -> R.string.register_formal
        Register.INFORMAL -> R.string.register_informal
        Register.UNIVERSAL -> R.string.register_universal
    },
)

/** The heading of a screen opened from a Lesson screen: its tool's name and the chapter it is for. */
@Composable
internal fun lessonName(lesson: LessonView): String =
    lesson.number?.let { stringResource(R.string.lesson_label, it) } ?: stringResource(R.string.path_unit_number, lesson.place)
