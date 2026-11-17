package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Keypad
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.NumberDisplay
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.NumberKind
import com.polisheasy.engine.NumberRound
import com.polisheasy.engine.Round

/**
 * One question of Numbers. Heard, the number, the run or the sum is said when the question appears
 * and again from the play button, and nothing of it is written until it is answered; read, its
 * Polish words are written. Below are what has been typed and the keypad, whose last key checks
 * the answer. Answered, the question is written out in numerals.
 */
@Composable
internal fun NumbersStep(round: NumberRound, sayAll: (List<String>) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    LaunchedEffect(Unit) { if (round.listen) sayAll(question.words) }
    Answered(given, next)
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
            Keypad(
                onDigit = { onMove(round.press(it)) },
                onErase = { onMove(round.erase()) },
                onCheck = { onMove(round.check()) },
                eraseLabel = stringResource(R.string.keypad_erase),
                checkLabel = stringResource(R.string.action_check),
                enabled = given == null,
                canCheck = round.canCheck,
            )
        },
    ) {
        Instruction(
            stringResource(
                when (question.kind) {
                    NumberKind.SINGLE -> if (round.listen) R.string.numbers_listen_instruction else R.string.numbers_read_instruction
                    NumberKind.RUN -> if (round.listen) R.string.numbers_run_listen_instruction else R.string.numbers_run_read_instruction
                    NumberKind.SUM -> if (round.listen) R.string.numbers_sum_listen_instruction else R.string.numbers_sum_read_instruction
                },
            ),
        )
        Spacer(Modifier.height(Space.l))
        if (round.listen) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                PlayButton(stringResource(R.string.audio_play), onClick = { sayAll(question.words) })
            }
        } else {
            Text(
                question.polish,
                style = MaterialTheme.typography.headlineLarge.inPolish(),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(Space.l))
        NumberDisplay(
            typed = round.typed,
            description = if (round.typed.isEmpty()) stringResource(R.string.numbers_typed_nothing) else stringResource(R.string.numbers_typed, round.typed),
            mark = given?.let { if (it.correct) Mark.Right else Mark.Missed },
        )
        // What was asked is written out once it is answered: in numerals, and in words if it was only heard.
        if (given != null) {
            Spacer(Modifier.height(Space.m))
            RoundNote(
                if (given.correct) Mark.Right else Mark.Missed,
                if (round.listen) stringResource(R.string.numbers_answer, question.numerals, question.polish) else question.numerals,
                polish = round.listen,
            )
        }
    }
}
