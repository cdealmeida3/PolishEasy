package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.BasketView
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PictureOption
import com.polisheasy.app.ui.components.PlateFoodView
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.MarketRound
import com.polisheasy.engine.Round

/**
 * One shopping list of Market list, chapter 9's Game. Heard, the list is said when it appears and
 * again from the play button; read, it is written. Below is the basket, with what is in it, and
 * the stall of goods, three across: a tap on the stall puts one more of a good in the basket, a
 * tap in the basket takes one out, and "That's all" pays. Once paid, a good the basket holds as
 * many of as the list asks is marked right; one it holds another number of, or any of one the list
 * does not ask for, a cross; and the rest fade. The list is written below.
 */
@Composable
internal fun MarketListStep(round: MarketRound, sayAll: (List<String>) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    LaunchedEffect(round.step) { if (round.listen) sayAll(question.words) }
    Answered(given, next)
    // Once paid, a good is right where the basket holds as many as the list asks, and wrong where it holds another number of one the list asks for or any of one it does not.
    fun stateOf(id: String): OptionState = when {
        given == null -> if (id in round.basket) OptionState.Chosen else OptionState.Idle
        id in question.wanted -> if (round.counts[id] == question.wanted[id]) OptionState.Right else OptionState.Wrong
        id in round.basket -> OptionState.Wrong
        else -> OptionState.Faded
    }
    RoundStep(
        action = if (given != null) {
            { ContinueButton(given, next) }
        } else {
            { PrimaryButton(stringResource(R.string.market_pay), { onMove(round.pay()) }, enabled = round.canPay) }
        },
        answer = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                question.stall.chunked(ACROSS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        row.forEach { good ->
                            val count = round.counts[good.id] ?: 0
                            PictureOption(
                                art = artOf(good.picture),
                                description = if (count == 0) good.means else pluralStringResource(R.plurals.market_in_basket, count, good.means, count),
                                state = stateOf(good.id),
                                onClick = { onMove(round.add(good.id)) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                        repeat(ACROSS - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        },
    ) {
        Instruction(stringResource(if (round.listen) R.string.market_listen_instruction else R.string.market_read_instruction))
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
        Spacer(Modifier.height(Space.m))
        val byId = question.stall.associateBy { it.id }
        val inBasket = round.inBasket
        BasketView(
            goods = inBasket.mapNotNull { byId[it] }.map { PlateFoodView(artOf(it.picture), it.means) },
            empty = stringResource(R.string.market_empty),
            takeOutLabel = stringResource(R.string.market_take_out),
            onTakeOut = { onMove(round.takeOut(inBasket[it])) },
            enabled = given == null,
        )
        if (given != null) {
            Spacer(Modifier.height(Space.m))
            RoundNote(if (given.correct) Mark.Right else Mark.Missed, question.polish, polish = true)
        }
    }
}

private const val ACROSS = 3
