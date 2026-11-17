package com.polisheasy.app.ui.screens.lesson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.CardNote
import com.polisheasy.app.ui.components.DetailFrame
import com.polisheasy.app.ui.components.Flashcard
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.RegisterChip
import com.polisheasy.app.ui.components.SecondaryButton
import com.polisheasy.app.ui.components.SpeakerButton
import com.polisheasy.app.ui.components.TopBar
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.ClipView
import com.polisheasy.engine.FlashcardView
import com.polisheasy.engine.LessonView

/**
 * A chapter's Fiszki, one card at a time: the Polish on the chapter's colour, turned with a tap to
 * show what it means. Once turned, the Learner says whether they knew it, and the word is
 * rescheduled in Review as an answer would reschedule it. Words due for Review come first. At the
 * end, how many they knew, and "Go again".
 */
@Composable
internal fun FlashcardsScreen(
    lesson: LessonView,
    cards: List<FlashcardView>,
    index: Int,
    knew: Int,
    shown: Int,
    sound: Boolean,
    onAnswer: (knew: Boolean) -> Unit,
    onAgain: () -> Unit,
    onPlay: (ClipView) -> Unit,
    onBack: () -> Unit,
) {
    val card = cards.getOrNull(index)
    // Each card starts face up, and stays turned if the screen is rebuilt.
    var turned by rememberSaveable(shown) { mutableStateOf(false) }
    DetailFrame(
        backLabel = stringResource(R.string.dictionary_back),
        onBack = onBack,
        action = when {
            card == null && cards.isNotEmpty() -> {
                {
                    PrimaryButton(stringResource(R.string.flashcards_again), onAgain)
                    Spacer(Modifier.height(Space.xs))
                    SecondaryButton(stringResource(R.string.dictionary_back), onBack)
                }
            }
            card != null && turned -> {
                {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.fillMaxWidth()) {
                        SecondaryButton(stringResource(R.string.flashcards_not_yet), { onAnswer(false) }, modifier = Modifier.weight(1f))
                        PrimaryButton(stringResource(R.string.flashcards_knew), { onAnswer(true) }, modifier = Modifier.weight(1f))
                    }
                }
            }
            else -> null
        },
    ) {
        TopBar(
            stringResource(R.string.flashcards_title),
            note = if (card != null) {
                stringResource(R.string.flashcards_count, index + 1, cards.size)
            } else {
                "${lessonName(lesson)} · ${lesson.title}"
            },
        )
        when {
            cards.isEmpty() -> CardNote(stringResource(R.string.flashcards_empty))
            card == null -> Finished(knew, cards.size)
            else -> {
                Instruction(
                    stringResource(if (card.due) R.string.flashcards_due else if (card.met) R.string.lesson_flashcards_note else R.string.flashcards_new),
                    highlighted = card.due,
                )
                Spacer(Modifier.height(Space.s))
                val word = card.word
                Flashcard(
                    front = word.text,
                    back = word.meaning,
                    flipped = turned,
                    onFlip = { turned = !turned },
                    flipLabel = stringResource(R.string.flashcards_turn),
                    accent = lesson.accent,
                    register = word.register?.let { register ->
                        { RegisterChip(register.mark, description = registerWords(register), label = registerWords(register)) }
                    },
                    speaker = word.audio?.takeIf { sound }?.let { clip ->
                        { SpeakerButton(stringResource(R.string.audio_play), onClick = { onPlay(clip) }) }
                    },
                )
                if (!turned) {
                    Spacer(Modifier.height(Space.m))
                    Instruction(stringResource(R.string.flashcards_turn_hint))
                }
            }
        }
    }
}

/** The end of the cards: a cheer in Polish and how many the Learner knew. */
@Composable
private fun Finished(knew: Int, cards: Int) {
    Spacer(Modifier.height(Space.xl))
    Text(
        stringResource(R.string.flashcards_done_title),
        style = MaterialTheme.typography.displaySmall.inPolish(),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().semantics { heading() },
    )
    Text(
        stringResource(R.string.flashcards_done_meaning),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(Space.l))
    Text(
        stringResource(R.string.flashcards_done_note, knew, cards),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
