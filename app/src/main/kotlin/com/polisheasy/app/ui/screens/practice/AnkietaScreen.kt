package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.ChatBubble
import com.polisheasy.app.ui.components.FieldState
import com.polisheasy.app.ui.components.FieldView
import com.polisheasy.app.ui.components.FormCard
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.OptionGrid
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.components.SpeakerButton
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.AnkietaLineOfForm
import com.polisheasy.engine.AnkietaRound
import com.polisheasy.engine.Round

/**
 * One person of Ankieta. Heard, what they say is played when they appear and again from the play
 * button, and written only once their form is filled in; read, it is written in one chat bubble
 * from the start, its meaning added once the form is filled in. Under it is the form, its fields
 * filled one at a time: the one being asked is marked, a right answer is written in green and one
 * put right in amber. The options for the field are at the bottom, two across while they fit.
 */
@Composable
internal fun AnkietaStep(round: AnkietaRound, sound: Boolean, say: (String) -> Unit, sayAll: (List<String>) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val person = round.person ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    val lines = person.says.map { it.text }
    LaunchedEffect(person.id) { if (round.listen) sayAll(lines) }
    Answered(given, next)
    val form = round.form
    val filled = form.all { it.filled != null }
    fun stateOf(option: Int): OptionState = when {
        given == null -> OptionState.Idle
        option == question.answer -> OptionState.Right
        question.options[option] == given.text -> OptionState.Wrong
        else -> OptionState.Faded
    }
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
            OptionGrid(question.options, ::stateOf, onClick = { option -> onMove(round.choose(option)) })
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Instruction(
                stringResource(if (round.listen) R.string.ankieta_listen_instruction else R.string.ankieta_read_instruction),
                Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.ankieta_person, question.person + 1, round.people.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Space.s))
        if (round.listen && !filled) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                PlayButton(stringResource(R.string.audio_play_again), onClick = { sayAll(lines) })
            }
        } else {
            // All they say in one bubble, as one message.
            ChatBubble(
                lines.joinToString(" "),
                if (filled) person.says.joinToString(" ") { it.meaning } else "",
                mine = false,
                speaker = if (sound) {
                    { SpeakerButton(stringResource(R.string.audio_play), onClick = { sayAll(lines) }) }
                } else {
                    null
                },
            )
        }
        Spacer(Modifier.height(Space.m))
        FormCard(stringResource(R.string.ankieta_form), form.map { fieldView(it) })
    }
}

/** A field of the form as the [FormCard] shows it, with its words for a screen reader. */
@Composable
private fun fieldView(field: AnkietaLineOfForm): FieldView {
    val filled = field.filled
    return when {
        filled == null -> FieldView(
            field.label,
            field.meaning,
            null,
            if (field.current) FieldState.Asking else FieldState.Blank,
            stringResource(R.string.ankieta_field_blank, field.label),
        )
        field.right == true -> FieldView(field.label, field.meaning, filled, FieldState.Right, stringResource(R.string.ankieta_field_right, field.label, filled))
        else -> FieldView(field.label, field.meaning, filled, FieldState.Missed, stringResource(R.string.ankieta_field_missed, field.label, filled))
    }
}
