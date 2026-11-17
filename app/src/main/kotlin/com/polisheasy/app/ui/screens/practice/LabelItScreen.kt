package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.AnswerField
import com.polisheasy.app.ui.components.EmptyTileSlot
import com.polisheasy.app.ui.components.FieldState
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.LabelBoard
import com.polisheasy.app.ui.components.LabelView
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.PolishKeyRow
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.components.TileBank
import com.polisheasy.app.ui.components.WordTile
import com.polisheasy.app.ui.components.inserting
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.LabelOnPicture
import com.polisheasy.engine.LabelRound
import com.polisheasy.engine.Round

/**
 * Label the picture. The board of pictures is at the top, the one asked marked, with its meaning
 * under the board. The labels lie below as tiles, or, typed, there is a field to type into. A
 * right label sticks in green and is said; a wrong one puts the right label on in amber, says it
 * and waits for "Continue". The board stays while its pictures are labelled one by one.
 */
@Composable
internal fun LabelItStep(round: LabelRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val spot = round.spot ?: return
    val given = round.given
    var typed by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val keyboard = LocalSoftwareKeyboardController.current
    val next = {
        typed = TextFieldValue("")
        onMove(round.next())
    }
    // Each picture is felt for itself: the board stays on screen while all of them are labelled.
    key(round.done - if (given == null) 0 else 1) {
        Answered(given, next) { say(spot.polish) }
    }
    val submitTyped = {
        if (given == null && typed.text.isNotBlank()) {
            // Hidden so "Continue" is not behind the keyboard.
            keyboard?.hide()
            onMove(round.type(typed.text))
        }
    }
    val typing = round.typed
    RoundStep(
        action = when {
            given != null -> {
                { ContinueButton(given, next) }
            }
            typing -> {
                { PrimaryButton(stringResource(R.string.action_check), submitTyped, enabled = typed.text.isNotBlank()) }
            }
            else -> null
        },
        keyRow = if (typing && given == null) {
            {
                PolishKeyRow(
                    onKey = { letter -> typed = typed.inserting(letter) },
                    keyDescription = { letter -> stringResource(R.string.polish_key_description, letter) },
                )
            }
        } else {
            null
        },
        answer = if (typing) {
            { AnswerField(typed, { typed = it }, stringResource(R.string.label_type_field), enabled = given == null, onDone = submitTyped) }
        } else {
            {
                TileBank {
                    round.labels.forEachIndexed { label, belongs ->
                        val word = round.spots[belongs].polish
                        if (round.placed(label)) {
                            EmptyTileSlot(word)
                        } else {
                            WordTile(word, enabled = given == null, onClick = { onMove(round.choose(label)) })
                        }
                    }
                }
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Instruction(stringResource(if (typing) R.string.label_type_instruction else R.string.label_choose_instruction), Modifier.weight(1f))
            Text(
                stringResource(R.string.label_progress, round.done + if (given == null) 1 else 0, round.total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Space.s))
        LabelBoard(round.board.map { labelView(it) })
        Spacer(Modifier.height(Space.s))
        if (given != null && !given.correct) {
            RoundNote(Mark.Missed, spot.polish, polish = true)
        } else {
            // What the picture asked means, so a picture that could be two things is not a guess.
            Text(
                spot.meaning,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** A picture of the board as the [LabelBoard] shows it, with its words for a screen reader. */
@Composable
private fun labelView(picture: LabelOnPicture): LabelView {
    val art = artOf(picture.spot.picture)
    return when {
        !picture.labelled -> LabelView(
            art,
            null,
            if (picture.current) FieldState.Asking else FieldState.Blank,
            stringResource(if (picture.current) R.string.label_picture_asked else R.string.label_picture_blank, picture.spot.meaning),
        )
        picture.right == true -> LabelView(art, picture.spot.polish, FieldState.Right, stringResource(R.string.label_picture_right, picture.spot.meaning, picture.spot.polish))
        else -> LabelView(art, picture.spot.polish, FieldState.Missed, stringResource(R.string.label_picture_missed, picture.spot.meaning, picture.spot.polish))
    }
}
