package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.MoneyCounter
import com.polisheasy.app.ui.components.MoneyKeys
import com.polisheasy.app.ui.components.MoneyView
import com.polisheasy.app.ui.components.NumberDisplay
import com.polisheasy.app.ui.components.PictureFace
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.Round
import com.polisheasy.engine.TillRound

/**
 * One price of Kiosk till, chapter 7's Game. The good is on the counter in its picture; heard, its
 * price is said when it appears and again from the play button, and read, it is written. Below are
 * what is on the counter and its sum, then the coins and notes to put there; "Pay" checks it.
 * Answered, the price is written as a till shows it, with its words if it was only heard.
 */
@Composable
internal fun KioskTillStep(round: TillRound, sayAll: (List<String>) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    LaunchedEffect(round.step) { if (round.listen) sayAll(question.words) }
    Answered(given, next)
    val money = round.money.map { it.money() }
    RoundStep(
        action = if (given != null) {
            { ContinueButton(given, next) }
        } else {
            { PrimaryButton(stringResource(R.string.till_pay), { onMove(round.pay()) }, enabled = round.canPay) }
        },
        answer = { MoneyKeys(money, onPick = { onMove(round.add(round.money[it])) }, enabled = given == null) },
    ) {
        Instruction(stringResource(if (round.listen) R.string.till_listen_instruction else R.string.till_read_instruction))
        Spacer(Modifier.height(Space.m))
        artOf(question.picture)?.let { art ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                PictureFace(art, Modifier.size(Size.pictureOption))
            }
        }
        Spacer(Modifier.height(Space.m))
        if (round.listen) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                PlayButton(stringResource(R.string.audio_play), onClick = { sayAll(question.words) })
            }
        } else {
            Text(
                question.polish,
                style = MaterialTheme.typography.titleLarge.inPolish(),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(Space.l))
        val sum = round.counter
        NumberDisplay(
            typed = sum,
            description = if (sum.isEmpty()) stringResource(R.string.till_paid_nothing) else stringResource(R.string.till_paid, sum),
            mark = given?.let { if (it.correct) Mark.Right else Mark.Missed },
        )
        Spacer(Modifier.height(Space.s))
        MoneyCounter(
            paid = round.paid.map { it.money() },
            empty = stringResource(R.string.till_counter_empty),
            takeBackLabel = stringResource(R.string.till_take_back),
            onTakeBack = { onMove(round.takeBack(it)) },
            enabled = given == null,
        )
        if (given != null) {
            Spacer(Modifier.height(Space.m))
            RoundNote(
                if (given.correct) Mark.Right else Mark.Missed,
                if (round.listen) stringResource(R.string.till_answer, question.numerals, question.polish) else question.numerals,
                polish = round.listen,
            )
        }
    }
}

/** A coin or note in grosze, as it is written on it: "50 gr", "2 zł". */
@Composable
private fun Int.money(): MoneyView =
    if (this < 100) MoneyView(stringResource(R.string.till_grosze, this), note = false)
    else MoneyView(stringResource(R.string.till_zloty, this / 100), note = this >= 1000)
