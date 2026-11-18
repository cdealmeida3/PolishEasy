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
import com.polisheasy.app.ui.components.LottoCard
import com.polisheasy.app.ui.components.LottoNumberView
import com.polisheasy.app.ui.components.LottoState
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.LottoRound
import com.polisheasy.engine.Round

/**
 * One call of Lotto, chapter 7's Game. Heard, the number is said when it is called and again from
 * the play button; read, its Polish words are written. The card is below: the numbers called
 * before stay ticked, and once answered the number called is marked, with the wrong one tapped.
 */
@Composable
internal fun LottoStep(round: LottoRound, sayAll: (List<String>) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    LaunchedEffect(round.step) { if (round.listen) sayAll(question.words) }
    Answered(given, next)
    val ticked = stringResource(R.string.lotto_ticked)
    val numbers = round.card.map { number ->
        val state = when {
            number in round.ticked -> LottoState.Ticked
            given != null && number == question.number -> LottoState.Right
            given != null && !given.correct && given.text == "$number" -> LottoState.Wrong
            else -> LottoState.Open
        }
        LottoNumberView("$number", state, ticked.takeIf { state == LottoState.Ticked })
    }
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = { LottoCard(numbers, onTick = { onMove(round.tick(round.card[it])) }, enabled = given == null) },
    ) {
        Instruction(stringResource(if (round.listen) R.string.lotto_listen_instruction else R.string.lotto_read_instruction))
        Spacer(Modifier.height(Space.l))
        if (round.listen) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                PlayButton(stringResource(R.string.audio_play), onClick = { sayAll(question.words) })
            }
        } else {
            Text(
                question.polish,
                style = MaterialTheme.typography.headlineMedium.inPolish(),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (given != null) {
            Spacer(Modifier.height(Space.m))
            RoundNote(
                if (given.correct) Mark.Right else Mark.Missed,
                if (round.listen) stringResource(R.string.numbers_answer, "${question.number}", question.polish) else "${question.number}",
                polish = round.listen,
            )
        }
    }
}
