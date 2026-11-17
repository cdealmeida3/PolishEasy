package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionCard
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PictureFace
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.Gender
import com.polisheasy.engine.JobClue
import com.polisheasy.engine.Round
import com.polisheasy.engine.WhoRound

/**
 * One question of Who's who, chapter 5's Game. A job is shown by its picture, or written with its
 * meaning, under *Kim on jest?* or *Kim ona jest?*, and the Learner finishes *On jest …* with one
 * of four words. Once answered, the right one is marked, and the one tapped if it was wrong; the
 * whole sentence is written, and said where there is a Clip of it. Choosing the job as it is
 * written alone gets a word on the instrumental.
 */
@Composable
internal fun WhosWhoStep(round: WhoRound, sayFirst: (List<String>) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    val man = question.gender == Gender.MASCULINE
    val sentence = stringResource(if (man) R.string.whos_who_he_is else R.string.whos_who_she_is, question.answer)
    Answered(given, next) { sayFirst(listOf(sentence)) }
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
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
        },
    ) {
        Instruction(stringResource(R.string.whos_who_instruction))
        Spacer(Modifier.height(Space.l))
        val art = question.picture?.takeIf { round.clue == JobClue.PICTURE }?.let { artOf(it) }
        if (art != null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                PictureFace(art, Modifier.size(Size.pictureOption))
            }
        } else {
            Text(
                question.word,
                style = MaterialTheme.typography.headlineLarge.inPolish(),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                question.meaning,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(Space.m))
        Text(
            stringResource(if (man) R.string.whos_who_he else R.string.whos_who_she),
            style = MaterialTheme.typography.titleLarge.inPolish(),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (given != null) {
            Spacer(Modifier.height(Space.m))
            RoundNote(if (given.correct) Mark.Right else Mark.Missed, sentence, polish = true)
            if (given.text == question.word) {
                Spacer(Modifier.height(Space.xs))
                Text(
                    stringResource(R.string.whos_who_after_jest, question.answer, question.word),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
