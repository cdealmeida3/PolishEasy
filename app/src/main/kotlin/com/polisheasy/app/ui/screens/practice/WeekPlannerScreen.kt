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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.Diary
import com.polisheasy.app.ui.components.DiaryDayState
import com.polisheasy.app.ui.components.DiaryDayView
import com.polisheasy.app.ui.components.DiaryEntry
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionCard
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.PromptCard
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.PlannerRound
import com.polisheasy.engine.Round

/**
 * One plan of Week planner, chapter 12's second Game. Heard, the plan is said when it comes and
 * again from the play button; read, it is written. The week's diary is below, the plans put in so
 * far written in their days. The Learner taps the day, then the time from four; the plan is
 * written in its day, the day is marked, and a wrong answer says when it was.
 */
@Composable
internal fun WeekPlannerStep(round: PlannerRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    LaunchedEffect(round.step) { if (round.listen) say(question.text) }
    Answered(given, next) { if (!round.listen) say(question.text) }
    val picked = stringResource(R.string.planner_picked)
    val days = round.days.mapIndexed { index, name ->
        val state = when {
            given != null && index == question.day -> DiaryDayState.Right
            given != null && index == round.day -> DiaryDayState.Wrong
            given == null && index == round.day -> DiaryDayState.Picked
            else -> DiaryDayState.Open
        }
        val entries = round.written.filter { it.day == index }.map { DiaryEntry(it.time, it.what) }
        DiaryDayView(name, entries, state, picked.takeIf { state == DiaryDayState.Picked })
    }
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = if (round.day == null) null else {
            {
                // Two across, so a time keeps its room beside the tick or the cross.
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    question.times.chunked(ACROSS).forEachIndexed { line, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            row.forEachIndexed { place, time ->
                                OptionCard(
                                    text = time,
                                    state = when {
                                        given == null -> OptionState.Idle
                                        time == question.time -> OptionState.Right
                                        given.text.endsWith(" $time") -> OptionState.Wrong
                                        else -> OptionState.Faded
                                    },
                                    onClick = { onMove(round.pickTime(line * ACROSS + place)) },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                )
                            }
                        }
                    }
                }
            }
        },
    ) {
        Instruction(stringResource(if (round.day == null) R.string.planner_day_instruction else R.string.planner_time_instruction))
        Spacer(Modifier.height(Space.s))
        if (round.listen && given == null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                PlayButton(stringResource(R.string.audio_play), onClick = { say(question.text) })
            }
        } else {
            PromptCard {
                Text(
                    question.text,
                    style = MaterialTheme.typography.titleLarge.inPolish(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(Space.s))
        Diary(days, onTap = if (given == null) { index -> onMove(round.pickDay(index)) } else null)
        if (given != null) {
            Spacer(Modifier.height(Space.s))
            RoundNote(
                if (given.correct) Mark.Right else Mark.Missed,
                stringResource(R.string.planner_note, round.days[question.day], question.time, question.meaning),
                polish = true,
            )
        }
    }
}

private const val ACROSS = 2
