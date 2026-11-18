package com.polisheasy.app.ui.screens.practice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.ButtonTone
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.rememberAnswerHaptics
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.engine.ClipView
import com.polisheasy.engine.ChainRound
import com.polisheasy.engine.AnkietaRound
import com.polisheasy.engine.ChatRound
import com.polisheasy.engine.GapRound
import com.polisheasy.engine.GenderRound
import com.polisheasy.engine.LabelRound
import com.polisheasy.engine.NumberRound
import com.polisheasy.engine.OppositesRound
import com.polisheasy.engine.QuickRound
import com.polisheasy.engine.RegisterRound
import com.polisheasy.engine.SpellRound
import com.polisheasy.engine.Given
import com.polisheasy.engine.ListenRound
import com.polisheasy.engine.MemoryRound
import com.polisheasy.engine.Round
import com.polisheasy.engine.VerbRound
import com.polisheasy.engine.WhoRound
import com.polisheasy.engine.VerbGroupRound
import com.polisheasy.engine.HobbyRound
import com.polisheasy.engine.LottoRound
import com.polisheasy.engine.TillRound
import kotlinx.coroutines.delay

/**
 * The step of a Round being shown, whichever Game it is. A move the Learner makes is handed to
 * [onMove] as the Round it leaves; the rules of every move are the engine's. With [sound] the
 * Polish of an answer is said as it is given, where there is a Clip for it; [onPlayAll] plays
 * several Clips one after the other. A Game whose last
 * move should be seen before the result calls [onFinish] when it has been.
 */
@Composable
internal fun RoundScreen(
    round: Round,
    sound: Boolean,
    clipOf: (String) -> ClipView?,
    onPlay: (ClipView) -> Unit,
    onPlayAll: (List<ClipView>) -> Unit,
    onMove: (Round) -> Unit,
    onFinish: () -> Unit,
) {
    val say: (String) -> Unit = { polish -> if (sound) clipOf(polish)?.let(onPlay) }
    // Several things in a row are said only when every one of them has a Clip.
    val sayAll: (List<String>) -> Unit = { polish ->
        if (sound) polish.map(clipOf).takeIf { clips -> clips.none { it == null } }?.let { onPlayAll(it.filterNotNull()) }
    }
    // The first of several ways to say a thing that has a Clip.
    val sayFirst: (List<String>) -> Unit = { polish -> if (sound) polish.firstNotNullOfOrNull(clipOf)?.let(onPlay) }
    when (round) {
        is NumberRound -> NumbersStep(round, sayAll, onMove)
        is SpellRound -> SpellItStep(round, say, sayAll, onMove)
        is QuickRound -> QuickRoundStep(round, onMove)
        is RegisterRound -> RegisterSortStep(round, say, onMove)
        is OppositesRound -> OppositesSnapStep(round, onMove)
        is GenderRound -> MojMojaMojeStep(round, say, sayFirst, onMove)
        is GapRound -> SoundGapsStep(round, say, onMove)
        is ChainRound -> PhraseChainsStep(round, say, onMove)
        is ListenRound -> ListenAndTapStep(round, say, onMove)
        is MemoryRound -> MemoryPairsBoard(round, sound, say, onMove, onFinish)
        is VerbRound -> VerbChallengeStep(round, say, onMove)
        is ChatRound -> ChatStep(round, sound, say, onMove, onFinish)
        is AnkietaRound -> AnkietaStep(round, sound, say, sayAll, onMove)
        is LabelRound -> LabelItStep(round, say, onMove)
        is WhoRound -> WhosWhoStep(round, sayFirst, onMove)
        is VerbGroupRound -> VerbGroupsStep(round, say, onMove)
        is HobbyRound -> HobbyCharadesStep(round, say, onMove)
        is TillRound -> KioskTillStep(round, sayAll, onMove)
        is LottoRound -> LottoStep(round, sayAll, onMove)
        else -> error("No screen for a Round of ${round.game}")
    }
}

/**
 * What follows an answer in every Game: it is felt, right or wrong, once; a right one moves on by
 * itself after a moment, and a wrong one waits for "Continue", so the right answer can be read.
 */
@Composable
internal fun Answered(given: Given?, onNext: () -> Unit, onFirst: (Given) -> Unit = {}) {
    // Felt once, when the answer is given, and not again if the screen is rebuilt.
    var felt by rememberSaveable { mutableStateOf(given != null) }
    val haptics = rememberAnswerHaptics()
    LaunchedEffect(given) {
        if (given == null) return@LaunchedEffect
        if (!felt) {
            felt = true
            if (given.correct) haptics.right() else haptics.wrong()
            onFirst(given)
        }
        if (given.correct) {
            delay(Motion.MOVE_ON_MILLIS)
            onNext()
        }
    }
}

/** "Continue" once a question is answered, in the colour of how it went. Tapping it does not wait for the moment to pass. */
@Composable
internal fun ContinueButton(given: Given, onNext: () -> Unit) {
    PrimaryButton(
        stringResource(R.string.action_continue),
        onNext,
        tone = if (given.correct) ButtonTone.Success else ButtonTone.Caution,
    )
}
