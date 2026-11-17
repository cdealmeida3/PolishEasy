package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import com.polisheasy.app.LocalSpeech
import com.polisheasy.app.R
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.components.AnswerField
import com.polisheasy.app.ui.components.ChatBubble
import com.polisheasy.app.ui.components.ChatHeader
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.MicButton
import com.polisheasy.app.ui.components.OptionCard
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PolishKeyRow
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.QuietButton
import com.polisheasy.app.ui.components.RegisterChip
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.SpeakerButton
import com.polisheasy.app.ui.components.inserting
import com.polisheasy.app.ui.screens.lesson.registerWords
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.withPolish
import com.polisheasy.engine.ChatGiven
import com.polisheasy.engine.ChatRound
import com.polisheasy.engine.ChatWay
import com.polisheasy.engine.Given
import com.polisheasy.engine.Register
import com.polisheasy.engine.Round
import kotlinx.coroutines.delay

/**
 * A Round of Chat, as a messenger shows a conversation: who it is with and where, then the bubbles,
 * newest at the bottom, and under them the Learner's reply: replies to choose, a field to type in,
 * or the microphone. The person's lines arrive one at a time, "typing" first, and are said as they
 * arrive. A reply is answered once: a right one moves on by itself, a wrong one shows the right
 * one and why, and waits. Once the person's last line is read, [onFinish] gives the result.
 */
@Composable
internal fun ChatStep(round: ChatRound, sound: Boolean, say: (String) -> Unit, onMove: (Round) -> Unit, onFinish: () -> Unit) {
    val reduced = Motion.reduced()
    val speak by rememberUpdatedState(say)
    // The person's newest line is "typed" first: these are the lines shown so far.
    var shown by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(round.lines.size) {
        if (shown > round.lines.size) shown = 0
        while (shown < round.lines.size) {
            val line = round.lines[shown]
            if (!line.mine && !reduced) delay(Motion.TYPING_MILLIS)
            shown++
            if (!line.mine) speak(line.text)
        }
    }
    val typing = shown < round.lines.size
    LaunchedEffect(round.finished, typing) {
        if (round.finished && !typing) {
            delay(Motion.CHAT_DONE_MILLIS)
            onFinish()
        }
    }
    val given = round.given
    val next = { onMove(round.next()) }
    // The reply the chat goes on with is said as it is given, right or put right.
    // Keyed by the lines, so each reply is felt once: a chat's step never changes.
    key(round.lines.size) {
        Answered(given?.let { Given(it.text, it.correct) }, next) { given?.let { reply -> speak(reply.reply.text) } }
    }

    var typed by rememberSaveable(round.done, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var heardNothing by rememberSaveable(round.done) { mutableStateOf(false) }
    val speech = LocalSpeech.current
    val choosing = round.way == ChatWay.CHOOSE || (round.way == ChatWay.SAY && (round.choosing || !speech.available))
    val typingReply = round.way == ChatWay.TYPE && round.replying && !typing
    val submit = { if (typed.text.isNotBlank()) onMove(round.type(typed.text)) }

    val scroll = rememberScrollState()
    LaunchedEffect(shown, given, typing) { scroll.animateScrollTo(scroll.maxValue) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)),
    ) {
        BoxWithConstraints(modifier = Modifier.weight(1f).widthIn(max = Size.contentMax)) {
            val most = maxHeight
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = Space.screenSide)) {
                // Who and where scroll with the chat, so at a large font size the bubbles keep their room.
                Column(
                    verticalArrangement = Arrangement.spacedBy(Space.xs),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                        .padding(bottom = Space.s),
                ) {
                    Spacer(Modifier.height(Space.m))
                    ChatHeader(
                        round.person,
                        round.scene,
                        register = round.register?.let { register -> { Register(register) } },
                    )
                    Spacer(Modifier.height(Space.s))
                    round.lines.take(shown).forEach { line ->
                        ChatBubble(
                            line.text,
                            line.meaning,
                            mine = line.mine,
                            missed = line.missed,
                            speaker = if (!line.mine && sound) {
                                { SpeakerButton(stringResource(R.string.audio_play), onClick = { say(line.text) }) }
                            } else {
                                null
                            },
                        )
                    }
                    if (typing && !round.lines[shown].mine) {
                        ChatBubble("", "", mine = false, typing = true, typingLabel = stringResource(R.string.chat_typing, round.person))
                    }
                    given?.let { reply ->
                        Spacer(Modifier.height(Space.xs))
                        Verdict(reply, chosen = choosing, said = round.way == ChatWay.SAY && !round.choosing)
                    }
                }
                // The reply, under the chat: never more than half the screen, so the chat stays in sight.
                Column(
                    modifier = Modifier
                        .heightIn(max = most / 2)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = Space.s),
                ) {
                    when {
                        typing || round.finished -> Unit
                        choosing -> Replies(round, given, onMove)
                        round.way == ChatWay.TYPE && given == null -> {
                            AnswerField(typed, { typed = it }, stringResource(R.string.chat_type_label), enabled = true, onDone = submit)
                        }
                        round.way == ChatWay.SAY && given == null -> Saying(
                            heardNothing = heardNothing,
                            onHeard = { heard ->
                                val after = round.say(heard)
                                heardNothing = after === round
                                if (after !== round) onMove(after)
                            },
                            onChoose = { onMove(round.chooseInstead()) },
                        )
                    }
                }
            }
        }
        Box(modifier = Modifier.widthIn(max = Size.contentMax).padding(start = Space.screenSide, end = Space.screenSide, bottom = if (typingReply) Space.xs else Space.screenBottom)) {
            when {
                given != null -> ContinueButton(Given(given.text, given.correct), next)
                typingReply -> PrimaryButton(stringResource(R.string.action_check), submit, enabled = typed.text.isNotBlank())
            }
        }
        if (typingReply) {
            PolishKeyRow(
                onKey = { letter -> typed = typed.inserting(letter) },
                keyDescription = { letter -> stringResource(R.string.polish_key_description, letter) },
            )
        }
    }
}

/** The replies to tap. Once one is given, the one the chat goes on with is marked, and the one tapped if it was wrong. */
@Composable
private fun Replies(round: ChatRound, given: ChatGiven?, onMove: (Round) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
        Instruction(stringResource(R.string.chat_choose_instruction))
        round.options.forEachIndexed { index, option ->
            val state = when {
                given == null -> OptionState.Idle
                option.text == given.reply.text -> OptionState.Right
                option.text == given.text -> OptionState.Wrong
                else -> OptionState.Faded
            }
            OptionCard(option.text, state, onClick = { round.choose(index).takeIf { it !== round }?.let(onMove) }, polish = true)
        }
    }
}

/** What became of a reply: the tick and the reply when it was typed or said right, or the cross, the right reply, what was heard if it was [said], and why the one given did not fit. */
@Composable
private fun Verdict(given: ChatGiven, chosen: Boolean, said: Boolean) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Space.xs),
        modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        when {
            given.correct && given.accentWarning -> RoundNote(Mark.Right, stringResource(R.string.feedback_accent_warning, given.reply.text))
            // A tapped reply is marked on its card; a typed or said one is written here.
            given.correct && !chosen -> RoundNote(Mark.Right, given.reply.text, polish = true)
            given.correct -> Unit
            else -> RoundNote(Mark.Missed, stringResource(R.string.feedback_correct_answer, given.reply.text))
        }
        // What the phone heard, so a Learner who said the right thing can tell it was misheard.
        if (said && !given.correct) {
            Text(stringResource(R.string.speak_heard, given.text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Space.xs))
        }
        if (!given.correct && (given.note != null || given.register != null)) {
            val colors = PolishEasyTheme.colors
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(horizontal = Space.xs)) {
                given.register?.let { register ->
                    Register(register)
                    Spacer(Modifier.width(Space.s))
                }
                given.note?.let { note ->
                    Text(withPolish(note.text, note.polish), style = MaterialTheme.typography.bodyLarge, color = colors.caution, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** The microphone to say the reply with, and the way to choose it instead: speaking never blocks. */
@Composable
private fun Saying(heardNothing: Boolean, onHeard: (List<String>) -> Unit, onChoose: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Instruction(stringResource(R.string.chat_say_instruction))
        Spacer(Modifier.height(Space.s))
        MicButton(stringResource(R.string.speak_mic), stringResource(R.string.speak_listening), onHeard, large = true)
        if (heardNothing) {
            Spacer(Modifier.height(Space.xs))
            Text(stringResource(R.string.speak_heard_nothing), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(Space.xs))
        QuietButton(stringResource(R.string.chat_choose_instead), onChoose, subdued = true)
    }
}

/** The book's register mark, with its word. */
@Composable
private fun Register(register: Register) {
    val words = registerWords(register)
    RegisterChip(register.mark, description = stringResource(R.string.register_description, register.mark, words), label = words)
}
