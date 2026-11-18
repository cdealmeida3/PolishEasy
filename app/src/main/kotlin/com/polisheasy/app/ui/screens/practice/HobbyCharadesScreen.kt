package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.AnswerField
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionCard
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PictureFace
import com.polisheasy.app.ui.components.PolishKeyRow
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.components.inserting
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.HobbyRound
import com.polisheasy.engine.Round

/**
 * One question of Hobby charades, chapter 6's Game. A hobby is acted out by its picture, and the
 * Learner names it with one of four cards or, typed, in a field. Once answered, the right hobby is
 * marked, and the one tapped if it was wrong, and *Lubię …* is written with it; the hobby is said
 * where there is a Clip of it.
 */
@Composable
internal fun HobbyCharadesStep(round: HobbyRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    var typed by rememberSaveable(question.key, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val keyboard = LocalSoftwareKeyboardController.current
    val next = { onMove(round.next()) }
    Answered(given, next) { say(question.answer) }
    val submitTyped = {
        if (given == null && typed.text.isNotBlank()) {
            // Hidden so "Continue" is not behind the keyboard.
            keyboard?.hide()
            onMove(round.type(typed.text))
        }
    }
    RoundStep(
        action = when {
            given != null -> {
                { ContinueButton(given, next) }
            }
            round.typed -> {
                { PrimaryButton(stringResource(R.string.action_check), submitTyped, enabled = typed.text.isNotBlank()) }
            }
            else -> null
        },
        keyRow = if (round.typed && given == null) {
            {
                PolishKeyRow(
                    onKey = { letter -> typed = typed.inserting(letter) },
                    keyDescription = { letter -> stringResource(R.string.polish_key_description, letter) },
                )
            }
        } else {
            null
        },
        answer = if (round.typed) {
            { AnswerField(typed, { typed = it }, stringResource(R.string.hobby_type_field), enabled = given == null, onDone = submitTyped) }
        } else {
            {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    question.options.forEachIndexed { index, option ->
                        val state = when {
                            given == null -> OptionState.Idle
                            option == question.answer -> OptionState.Right
                            option == given.text -> OptionState.Wrong
                            else -> OptionState.Faded
                        }
                        OptionCard(option, state, onClick = { onMove(round.choose(index)) }, polish = true)
                    }
                }
            }
        },
    ) {
        Instruction(stringResource(R.string.hobby_instruction))
        Spacer(Modifier.height(Space.l))
        artOf(question.picture)?.let { art ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                PictureFace(art, Modifier.size(Size.pictureTile))
            }
        }
        if (given != null) {
            Spacer(Modifier.height(Space.m))
            RoundNote(if (given.correct) Mark.Right else Mark.Missed, stringResource(R.string.hobby_lubie, question.answer), polish = true)
        }
    }
}
