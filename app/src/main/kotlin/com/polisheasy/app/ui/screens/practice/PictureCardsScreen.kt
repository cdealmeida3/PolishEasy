package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.ChoiceChips
import com.polisheasy.app.ui.components.DetailFrame
import com.polisheasy.app.ui.components.PictureTile
import com.polisheasy.app.ui.components.SectionHeading
import com.polisheasy.app.ui.components.SpeakerButton
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.ClipView
import com.polisheasy.engine.PictureCard
import com.polisheasy.engine.PictureCardsView

/**
 * Picture cards: the learned words that have a picture, or every learned word, two across, under
 * the Unit that taught them. A card turns over when tapped to show the word and what it means, and says it as it turns; its
 * speaker says it again. Nothing is asked and nothing is counted. With [sound] off the cards are
 * silent and have no speaker.
 */
@Composable
internal fun PictureCardsScreen(
    view: PictureCardsView,
    sound: Boolean,
    clipOf: (String) -> ClipView?,
    onPlay: (ClipView) -> Unit,
    onKind: (String) -> Unit,
    onDeck: (String) -> Unit,
    onBack: () -> Unit,
) {
    // The cards showing their words, by Item. A card stays turned until it is tapped again.
    var turned by rememberSaveable { mutableStateOf(listOf<String>()) }
    DetailFrame(backLabel = stringResource(R.string.action_back), onBack = onBack) {
        Text(
            stringResource(R.string.game_picture_cards),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.game_picture_cards_note),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // What the cards show, then which words: the second only when there is more than one deck.
        listOf(view.kinds to onKind, view.decks to onDeck).filter { (choice, _) -> choice.options.size > 1 }.forEach { (choice, onChoose) ->
            Spacer(Modifier.height(Space.m))
            ChoiceChips(
                label = choiceLabel(choice.id),
                options = choice.options.map { optionLabel(it) },
                selected = choice.options.indexOfFirst { it.id == choice.selected },
                onSelect = { onChoose(choice.options[it].id) },
            )
        }
        view.groups.forEach { group ->
            Spacer(Modifier.height(Space.l))
            SectionHeading(group.title)
            group.cards.chunked(ACROSS).forEach { row ->
                Spacer(Modifier.height(Space.xs))
                // The cards of a row are as tall as the tallest of them.
                Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    row.forEach { card ->
                        val clip = if (sound) clipOf(card.polish) else null
                        Card(
                            card,
                            turned = card.itemId in turned,
                            clip = clip,
                            onPlay = onPlay,
                            onTurn = {
                                if (card.itemId in turned) {
                                    turned = turned - card.itemId
                                } else {
                                    turned = turned + card.itemId
                                    clip?.let(onPlay)
                                }
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    // A last card alone keeps its half of the row.
                    repeat(ACROSS - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private const val ACROSS = 2

@Composable
private fun Card(card: PictureCard, turned: Boolean, clip: ClipView?, onPlay: (ClipView) -> Unit, onTurn: () -> Unit, modifier: Modifier) {
    // A card whose image cannot be read shows its meaning in words, as one with no picture does.
    val art = card.picture?.let { artOf(it) }
    PictureTile(
        art = art,
        polish = card.polish,
        meaning = card.meaning,
        turned = turned,
        onTurn = onTurn,
        turnLabel = stringResource(R.string.picture_card_turn),
        modifier = modifier,
        speaker = clip?.let { { SpeakerButton(stringResource(R.string.audio_play), onClick = { onPlay(it) }) } },
    )
}
