package com.polisheasy.app.ui.screens.practice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.BestLine
import com.polisheasy.app.ui.components.GameBadge
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.QuietButton
import com.polisheasy.app.ui.components.ScreenFrame
import com.polisheasy.app.ui.components.StatRow
import com.polisheasy.app.ui.components.StatTile
import com.polisheasy.app.ui.components.rememberAnswerHaptics
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.Game
import com.polisheasy.engine.RoundResult
import com.polisheasy.engine.Score

/**
 * The end of a Round: a cheer in Polish, what the Round came to and the Best as two small facts,
 * and "New best" when the Best was beaten. "Play again" starts another Round the same way; "Done"
 * goes back to Practice. No confetti: that is for a Daily Set, which counts.
 */
@Composable
internal fun RoundResultScreen(result: RoundResult, onPlayAgain: () -> Unit, onDone: () -> Unit) {
    BackHandler(onBack = onDone)
    val haptics = rememberAnswerHaptics()
    LaunchedEffect(Unit) { haptics.finished() }
    val cheers = stringArrayResource(R.array.summary_cheers)
    val meanings = stringArrayResource(R.array.summary_cheer_meanings)
    val cheer = result.score.value.mod(cheers.size)
    val words = result.game.words()
    ScreenFrame(horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(Space.l))
            GameBadge(words.icon, size = Size.gameIconHero)
            Spacer(Modifier.height(Space.m))
            Text(
                cheers[cheer],
                style = MaterialTheme.typography.displaySmall.inPolish(),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                meanings[cheer],
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                stringResource(R.string.result_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(words.name),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (result.newBest) {
                Spacer(Modifier.height(Space.l))
                BestLine(stringResource(R.string.result_new_best), fresh = true)
            }
            Spacer(Modifier.height(Space.l))
            val score = scoreWords(result.game, result.score)
            StatRow(
                tiles = listOf(
                    { modifier -> StatTile(score.value, score.label, score.sentence, modifier) },
                    { modifier ->
                        StatTile(
                            value = result.best.toString(),
                            label = stringResource(if (result.first) R.string.result_first_best else R.string.result_best),
                            description = stringResource(R.string.result_best_sentence, bestValue(result.game, result.best)),
                            modifier = modifier,
                        )
                    },
                ),
            )
            Spacer(Modifier.height(Space.m))
        }
        Spacer(Modifier.height(Space.m))
        PrimaryButton(stringResource(R.string.result_play_again), onPlayAgain)
        Spacer(Modifier.height(Space.xs))
        QuietButton(stringResource(R.string.result_done), onDone)
    }
}

/** What a Round came to, as a [StatTile] shows it: the number, what it counts, and the fact as a sentence. */
internal class ScoreWords(val value: String, val label: String, val sentence: String)

@Composable
internal fun scoreWords(game: Game, score: Score): ScoreWords = when (game) {
    Game.PICTURE_CARDS -> error("Picture cards has no result")
    Game.MEMORY_PAIRS -> ScoreWords(
        value = score.value.toString(),
        label = pluralStringResource(R.plurals.result_turns, score.value),
        sentence = pluralStringResource(R.plurals.result_turns_sentence, score.value, score.value),
    )
    Game.QUICK_ROUND, Game.REGISTER_SORT -> ScoreWords(
        value = score.value.toString(),
        label = pluralStringResource(R.plurals.result_right, score.value),
        sentence = pluralStringResource(R.plurals.result_right_sentence, score.value, score.value),
    )
    Game.PHRASE_CHAINS -> ScoreWords(
        value = score.value.toString(),
        label = pluralStringResource(R.plurals.result_slips, score.value),
        sentence = pluralStringResource(R.plurals.result_slips_sentence, score.value, score.value),
    )
    Game.VERB_CHALLENGE, Game.LISTEN_AND_TAP, Game.SOUND_GAPS, Game.MOJ_MOJA_MOJE, Game.TEN_TA_TO, Game.LABEL_IT, Game.NUMBERS, Game.SPELL_IT, Game.CHAT, Game.ANKIETA -> ScoreWords(
        value = stringResource(R.string.summary_first_try_value, score.value, score.of ?: score.value),
        label = stringResource(R.string.summary_first_try),
        sentence = stringResource(R.string.summary_accuracy, score.value, score.of ?: score.value),
    )
}
