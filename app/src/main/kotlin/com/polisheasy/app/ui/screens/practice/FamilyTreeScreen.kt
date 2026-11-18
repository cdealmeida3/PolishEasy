package com.polisheasy.app.ui.screens.practice

import androidx.annotation.DrawableRes
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.FamilyTreeBoard
import com.polisheasy.app.ui.components.FieldState
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionCard
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PictureArt
import com.polisheasy.app.ui.components.PromptCard
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.components.SpeakerButton
import com.polisheasy.app.ui.components.TreeLink
import com.polisheasy.app.ui.components.TreeSpot
import com.polisheasy.app.ui.theme.Scaled
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.Relative
import com.polisheasy.engine.Round
import com.polisheasy.engine.TreeRound

/**
 * One question of Family tree, chapter 11's Game. The tree shows the grandparents, their three
 * children and the mother, and the five grandchildren, the Learner (*ja*) among them; the people
 * named so far have their word under them. Named: one person is marked "?" and the Learner picks
 * who it is, *mój wujek*, from four. Found: *mój wujek* is written, and said with sound on, and
 * the Learner taps him on the tree. Once answered, the person is written on the tree, green if
 * right and amber if put right, the phrase is said, and a wrong answer says who it was.
 */
@Composable
internal fun FamilyTreeStep(round: TreeRound, sound: Boolean, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val person = question.person
    val given = round.given
    val next = { onMove(round.next()) }
    LaunchedEffect(round.step) { if (round.find) say(person.phrase) }
    Answered(given, next) { say(person.phrase) }
    val arts = Relative.entries.associateWith { relative -> ImageBitmap.imageResource(pictureOf(relative)) }
    val me = ImageBitmap.imageResource(R.drawable.family_me)
    val tapped = given?.takeIf { !it.correct && round.find }?.let { wrong -> round.people.firstOrNull { it.phrase == wrong.text }?.relative }
    val unknown = stringResource(R.string.tree_unknown)
    val spots = remember(round, arts) {
        val asked = round.run.questions.map { it.person.relative }
        round.people.map { someone ->
            val at = asked.indexOf(someone.relative)
            val named = someone.relative in round.named
            val state = when {
                someone.relative == person.relative && given == null -> if (round.find) FieldState.Blank else FieldState.Asking
                someone.relative == tapped -> FieldState.Missed
                // Found wrong, the one asked is marked where they are, apart from the one tapped.
                someone.relative == person.relative && tapped != null -> FieldState.Right
                named -> if (round.run.results.getOrNull(at) == true) FieldState.Right else FieldState.Missed
                else -> FieldState.Blank
            }
            val label = if (named || someone.relative == tapped) someone.word else null
            val (column, row) = PLACES.getValue(someone.relative)
            TreeSpot(someone.relative.id, column, row, PictureArt.Image(arts.getValue(someone.relative)), label, state, me = false, description = label ?: unknown)
        } + TreeSpot(ME, ME_PLACE.first, ME_PLACE.second, PictureArt.Image(me), round.me, FieldState.Blank, me = true, description = round.me)
    }
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = if (round.find) null else {
            {
                // One across when the text is large, so no option wraps.
                val across = if (Scaled.roomy()) 1 else ACROSS
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    question.options.chunked(across).forEachIndexed { line, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            row.forEachIndexed { place, option ->
                                OptionCard(
                                    text = option,
                                    state = when {
                                        given == null -> OptionState.Idle
                                        option == person.phrase -> OptionState.Right
                                        option == given.text -> OptionState.Wrong
                                        else -> OptionState.Faded
                                    },
                                    onClick = { onMove(round.choose(line * across + place)) },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    polish = true,
                                )
                            }
                        }
                    }
                }
            }
        },
    ) {
        Instruction(stringResource(if (round.find) R.string.tree_find_instruction else R.string.tree_name_instruction))
        Spacer(Modifier.height(Space.s))
        if (round.find) {
            PromptCard(speaker = if (sound) ({ SpeakerButton(stringResource(R.string.audio_play), onClick = { say(person.phrase) }) }) else null) {
                Text(
                    person.phrase,
                    style = MaterialTheme.typography.headlineMedium.inPolish(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(Space.m))
        }
        FamilyTreeBoard(
            spots = spots,
            links = LINKS,
            columns = COLUMNS,
            onTap = if (round.find && given == null) { key -> Relative.of(key)?.let { onMove(round.tap(it)) } } else null,
        )
        if (given != null) {
            Spacer(Modifier.height(Space.m))
            val note = stringResource(R.string.tree_note, person.phrase, person.meaning)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                RoundNote(if (given.correct) Mark.Right else Mark.Missed, note, polish = true)
            }
        }
    }
}

@DrawableRes
private fun pictureOf(relative: Relative): Int = when (relative) {
    Relative.GRANDFATHER -> R.drawable.family_old_man
    Relative.GRANDMOTHER -> R.drawable.family_old_woman
    Relative.UNCLE, Relative.FATHER -> R.drawable.family_man
    Relative.AUNT, Relative.MOTHER -> R.drawable.family_woman
    Relative.COUSIN, Relative.BROTHER -> R.drawable.family_boy
    Relative.COUSIN_GIRL, Relative.SISTER -> R.drawable.family_girl
}

/** Where each person stands: the column, half steps allowed, and the row. */
private val PLACES = mapOf(
    Relative.GRANDFATHER to (0f to 0), Relative.GRANDMOTHER to (2f to 0),
    Relative.UNCLE to (0f to 1), Relative.AUNT to (1f to 1), Relative.FATHER to (2f to 1), Relative.MOTHER to (3f to 1),
    Relative.COUSIN to (0f to 2), Relative.COUSIN_GIRL to (1f to 2), Relative.BROTHER to (2f to 2), Relative.SISTER to (4f to 2),
)
private const val ME = "me"
private val ME_PLACE = 3f to 2

private val LINKS = listOf(
    TreeLink(listOf(Relative.GRANDFATHER.id, Relative.GRANDMOTHER.id), listOf(Relative.UNCLE.id, Relative.AUNT.id, Relative.FATHER.id)),
    TreeLink(listOf(Relative.FATHER.id, Relative.MOTHER.id), listOf(Relative.BROTHER.id, ME, Relative.SISTER.id)),
    TreeLink(listOf(Relative.UNCLE.id), listOf(Relative.COUSIN.id)),
    TreeLink(listOf(Relative.AUNT.id), listOf(Relative.COUSIN_GIRL.id)),
)
private const val COLUMNS = 5
private const val ACROSS = 2
