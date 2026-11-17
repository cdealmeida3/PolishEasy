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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.BucketRow
import com.polisheasy.app.ui.components.BucketView
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PictureFace
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.Game
import com.polisheasy.engine.GenderRound
import com.polisheasy.engine.NounClue
import com.polisheasy.engine.Round

/**
 * One question of Mój, moja, moje, or of Ten, ta, to. A noun is shown, with its picture when it
 * has one: written, said, or given only by what it means. Below are the words for "my", or for
 * "this", each counting the nouns that belong under it so far. The one tapped is marked and the right one with it, and the two
 * words are written and said together.
 */
@Composable
internal fun MojMojaMojeStep(round: GenderRound, say: (String) -> Unit, sayFirst: (List<String>) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    LaunchedEffect(Unit) { if (round.clue == NounClue.LISTEN) say(question.polish) }
    // The two words together are heard, where there is a Clip of them, and if not the noun.
    Answered(given, next) { sayFirst(listOf(question.phrase, question.polish)) }
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
            BucketRow(
                round.buckets.map { bucket ->
                    val holds = round.sorted(bucket.gender)
                    BucketView(
                        word = bucket.word,
                        holds = holds.toString(),
                        holdsDescription = pluralStringResource(R.plurals.bucket_holds, holds, holds),
                        state = when {
                            given == null -> OptionState.Idle
                            bucket.gender == question.gender -> OptionState.Right
                            bucket.word == given.text -> OptionState.Wrong
                            else -> OptionState.Faded
                        },
                    )
                },
                onClick = { onMove(round.choose(it)) },
            )
        },
    ) {
        Instruction(stringResource(if (round.game == Game.TEN_TA_TO) R.string.ten_instruction else R.string.moj_instruction))
        Spacer(Modifier.height(Space.l))
        question.picture?.let { picture ->
            artOf(picture)?.let { art ->
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    PictureFace(art, Modifier.size(Size.pictureOption))
                }
                Spacer(Modifier.height(Space.m))
            }
        }
        val written = round.clue == NounClue.WORD
        if (round.clue == NounClue.LISTEN) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                PlayButton(stringResource(R.string.audio_play), onClick = { say(question.polish) })
            }
        } else {
            Text(
                if (written) question.polish else question.meaning,
                style = if (written) MaterialTheme.typography.headlineLarge.inPolish() else MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // The meaning goes with the written word from the start, and with a word that was heard once it is answered.
        if (written || (given != null && round.clue == NounClue.LISTEN)) {
            Spacer(Modifier.height(Space.xs))
            Text(
                question.meaning,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (given != null) {
            Spacer(Modifier.height(Space.m))
            RoundNote(if (given.correct) Mark.Right else Mark.Missed, question.phrase, polish = true)
        }
    }
}
