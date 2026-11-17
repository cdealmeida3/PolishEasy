package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.AnswerField
import com.polisheasy.app.ui.components.EmptyTileSlot
import com.polisheasy.app.ui.components.GapSentenceText
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionCard
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PolishKeyRow
import com.polisheasy.app.ui.components.PolishPrompt
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.PromptCard
import com.polisheasy.app.ui.components.PromptChip
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.components.SentenceField
import com.polisheasy.app.ui.components.TileBank
import com.polisheasy.app.ui.components.VerbTable
import com.polisheasy.app.ui.components.VerbTableRowView
import com.polisheasy.app.ui.components.WordTile
import com.polisheasy.app.ui.components.inserting
import com.polisheasy.app.ui.screens.personLabel
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.Given
import com.polisheasy.engine.Round
import com.polisheasy.engine.VerbQuestion
import com.polisheasy.engine.VerbRound
import com.polisheasy.engine.VerbRowState
import com.polisheasy.engine.VerbStage

/**
 * One question of the Verb challenge. First the verb and a person, and the form is picked or
 * typed; then the verb's empty table, filled a row at a time from tiles; then a sentence with a
 * gap. [say] says Polish aloud where the Learner can listen.
 */
@Composable
internal fun VerbChallengeStep(round: VerbRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    var typed by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val keyboard = LocalSoftwareKeyboardController.current
    val next = { onMove(round.next()) }
    // The right form is heard as the answer is given; a sentence is heard whole.
    // Each question is felt for itself: the table stays on screen while several are answered.
    key(round.done - if (given == null) 0 else 1) {
        Answered(given, next) {
            val form = question.answers.first()
            say(question.sentence?.let { "${it.before} $form ${it.after}".trim() } ?: form)
        }
    }
    val submitTyped = {
        if (given == null && typed.text.isNotBlank()) {
            // Hidden so "Continue" is not behind the keyboard.
            keyboard?.hide()
            onMove(round.type(typed.text))
        }
    }
    val typing = round.typing
    RoundStep(
        action = when {
            given != null -> {
                { ContinueButton(given, next) }
            }
            typing -> {
                { PrimaryButton(stringResource(R.string.action_check), submitTyped, enabled = typed.text.isNotBlank()) }
            }
            else -> null
        },
        keyRow = if (typing && given == null) {
            {
                PolishKeyRow(
                    onKey = { letter -> typed = typed.inserting(letter) },
                    keyDescription = { letter -> stringResource(R.string.polish_key_description, letter) },
                )
            }
        } else {
            null
        },
        answer = when {
            question.stage == VerbStage.TABLE -> {
                { Tiles(question.options, round.tilesUsed, enabled = given == null, onChoose = { onMove(round.choose(it)) }) }
            }
            // With a sentence the Learner types into the sentence itself, up in the prompt.
            typing && question.stage == VerbStage.SENTENCE -> null
            typing -> {
                {
                    AnswerField(typed, { typed = it }, stringResource(R.string.type_answer_field_label), enabled = given == null, onDone = submitTyped)
                }
            }
            else -> {
                { Forms(question, given, onChoose = { onMove(round.choose(it)) }) }
            }
        },
    ) {
        when (question.stage) {
            VerbStage.PICK -> {
                Instruction(stringResource(if (typing) R.string.verb_type_instruction else R.string.verb_pick_instruction))
                Spacer(Modifier.height(Space.s))
                PromptChip(stringResource(R.string.verb_title, question.verb, question.meaning))
                Spacer(Modifier.height(Space.s))
                PromptCard {
                    Column {
                        PolishPrompt(question.person.pronouns)
                        Text(
                            personLabel(question.person),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (typing && given != null) {
                    Spacer(Modifier.height(Space.s))
                    TypedNote(question, given)
                }
            }
            VerbStage.TABLE -> {
                Instruction(stringResource(R.string.verb_table_instruction))
                Spacer(Modifier.height(Space.s))
                VerbTable(
                    title = stringResource(R.string.verb_title, question.verb, question.meaning),
                    rows = round.table.map { row ->
                        VerbTableRowView(
                            pronouns = row.person.pronouns,
                            form = row.form,
                            asked = row.state == VerbRowState.ASKED,
                            mark = when (row.state) {
                                VerbRowState.RIGHT -> Mark.Right
                                VerbRowState.MISSED -> Mark.Missed
                                else -> null
                            },
                            description = when (row.state) {
                                VerbRowState.RIGHT -> stringResource(R.string.verb_row_filled, row.person.pronouns, row.form.orEmpty())
                                VerbRowState.MISSED -> stringResource(R.string.verb_row_missed, row.person.pronouns, row.form.orEmpty())
                                VerbRowState.ASKED -> stringResource(R.string.verb_row_asked, row.person.pronouns)
                                VerbRowState.EMPTY -> stringResource(R.string.verb_row_empty, row.person.pronouns)
                            },
                        )
                    },
                )
            }
            VerbStage.SENTENCE -> {
                val sentence = checkNotNull(question.sentence)
                Instruction(stringResource(R.string.verb_sentence_instruction))
                Spacer(Modifier.height(Space.s))
                // The person is always shown: another form could make a sentence too.
                PromptChip(stringResource(R.string.conjugation_chip, question.verb, personLabel(question.person), question.person.pronouns))
                Spacer(Modifier.height(Space.l))
                if (typing && given == null) {
                    SentenceField(
                        before = sentence.before,
                        after = sentence.after,
                        typed = typed,
                        onTypedChange = { typed = it },
                        label = stringResource(R.string.type_answer_field_label),
                        enabled = true,
                        onDone = submitTyped,
                    )
                } else {
                    GapSentenceText(
                        sentence.before,
                        sentence.after,
                        filled = given?.let { question.answers.first() },
                        mark = given?.let { if (it.correct) Mark.Right else Mark.Missed },
                    )
                }
                Spacer(Modifier.height(Space.xs))
                Text(sentence.meaning, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (typing && given != null && !given.correct) {
                    Spacer(Modifier.height(Space.s))
                    TypedNote(question, given)
                }
            }
        }
    }
}

/** What became of a typed form: the form itself when it was right, or the right one when it was not. */
@Composable
private fun TypedNote(question: VerbQuestion, given: Given) {
    val form = question.answers.first()
    if (given.correct) {
        RoundNote(Mark.Right, form, polish = true)
    } else {
        RoundNote(Mark.Missed, stringResource(R.string.feedback_correct_answer, form))
    }
}

/** The forms to choose from. Once answered, the right one is marked, and the one tapped if it was wrong. */
@Composable
private fun Forms(question: VerbQuestion, given: Given?, onChoose: (Int) -> Unit) {
    val right = question.answers.first()
    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
        question.options.forEachIndexed { index, option ->
            val state = when {
                given == null -> OptionState.Idle
                option == right -> OptionState.Right
                option == given.text -> OptionState.Wrong
                else -> OptionState.Faded
            }
            OptionCard(option, state, onClick = { onChoose(index) }, polish = true)
        }
    }
}

/** The forms of the table as tiles. One whose form is in its row leaves an empty slot behind. */
@Composable
private fun Tiles(forms: List<String>, used: List<Boolean>, enabled: Boolean, onChoose: (Int) -> Unit) {
    TileBank {
        forms.forEachIndexed { index, form ->
            if (used[index]) EmptyTileSlot(form) else WordTile(form, enabled = enabled, onClick = { onChoose(index) })
        }
    }
}
