package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.BestLine
import com.polisheasy.app.ui.components.ChoiceChips
import com.polisheasy.app.ui.components.DetailFrame
import com.polisheasy.app.ui.components.GameBadge
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.GameSetup

/**
 * A Game before a Round: what it is, the ways to play it as chips to choose from, the Best of the
 * way chosen now, and "Play". [onChoose] is given the position of the choice and the id of the
 * option tapped.
 */
@Composable
internal fun GameSetupScreen(setup: GameSetup, onChoose: (choice: Int, option: String) -> Unit, onPlay: () -> Unit, onBack: () -> Unit) {
    val words = setup.game.words()
    DetailFrame(
        backLabel = stringResource(R.string.action_back),
        onBack = onBack,
        action = { PrimaryButton(stringResource(R.string.setup_play), onPlay) },
    ) {
        GameBadge(words.icon, size = Size.gameIconHero)
        Spacer(Modifier.height(Space.m))
        Text(
            stringResource(words.name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(words.note), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        setup.choices.forEachIndexed { index, choice ->
            Spacer(Modifier.height(Space.l))
            ChoiceChips(
                label = choiceLabel(choice.id),
                options = choice.options.map { optionLabel(it) },
                selected = choice.options.indexOfFirst { it.id == choice.selected },
                onSelect = { option -> onChoose(index, choice.options[option].id) },
                // An option the content names, such as a verb, is Polish.
                polish = choice.options.indices.filter { choice.options[it].label != null }.toSet(),
            )
        }
        Spacer(Modifier.height(Space.l))
        val best = setup.best
        if (best == null) {
            Instruction(stringResource(R.string.setup_no_best))
        } else {
            BestLine(stringResource(R.string.practice_best, bestValue(setup.game, best)))
        }
    }
}
