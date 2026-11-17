package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.CardNote
import com.polisheasy.app.ui.components.GameCard
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.PromptChip
import com.polisheasy.app.ui.components.ScreenFrame
import com.polisheasy.app.ui.components.SectionHeading
import com.polisheasy.app.ui.components.TodayCard
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.Game
import com.polisheasy.engine.GameState
import com.polisheasy.engine.GameView
import com.polisheasy.engine.PracticeView
import com.polisheasy.engine.Today

/**
 * Practice: a greeting, how many Rounds were played today, and the Games in named groups, each
 * with its Best. Until today's Daily Set is completed it is locked: the Games are greyed, with a
 * padlock each, and a card at the top says what opens them and starts or continues today's Daily
 * Set, which [today] names.
 */
@Composable
internal fun PracticeScreen(
    view: PracticeView,
    today: Today,
    onOpen: (Game) -> Unit,
    onStart: () -> Unit,
    onContinue: () -> Unit,
) {
    ScreenFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(Space.xs))
            Text(
                stringResource(R.string.practice_greeting),
                style = MaterialTheme.typography.headlineLarge.inPolish(),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(if (view.open) R.string.practice_open_note else R.string.practice_locked_note),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (view.open && view.roundsToday > 0) {
                Spacer(Modifier.height(Space.s))
                PromptChip(pluralStringResource(R.plurals.practice_rounds_today, view.roundsToday, view.roundsToday))
            }
            Spacer(Modifier.height(Space.l))
            if (!view.open) {
                Locked(today, onStart, onContinue)
                Spacer(Modifier.height(Space.l))
            }
            view.games.groupBy { it.game.group }.forEach { (group, games) ->
                SectionHeading(stringResource(group.title()))
                games.forEach { game ->
                    Spacer(Modifier.height(Space.xs))
                    Game(game, open = view.open, onOpen)
                }
                Spacer(Modifier.height(Space.l))
            }
        }
    }
}

/** What opens Practice, with the button that starts or continues today's Daily Set. */
@Composable
private fun Locked(today: Today, onStart: () -> Unit, onContinue: () -> Unit) {
    val review = today == Today.ReviewOnlyAvailable || (today is Today.InProgress && today.reviewOnly)
    TodayCard(
        title = stringResource(if (review) R.string.practice_locked_title_review else R.string.practice_locked_title),
        locked = true,
    ) {
        CardNote(stringResource(R.string.practice_locked_body))
        Spacer(Modifier.height(Space.l))
        if (today is Today.InProgress) {
            PrimaryButton(stringResource(R.string.home_continue), onContinue)
        } else {
            PrimaryButton(stringResource(R.string.home_start), onStart)
        }
    }
}

@Composable
private fun Game(view: GameView, open: Boolean, onOpen: (Game) -> Unit) {
    val words = view.game.words()
    val playable = open && view.state == GameState.READY
    val best = view.best?.let { best ->
        val played = view.lastPlayed.map { optionLabel(it) }.joinToString(", ")
        stringResource(R.string.practice_best_played, bestValue(view.game, best), played)
    }
    GameCard(
        name = stringResource(words.name),
        description = stringResource(words.note),
        icon = words.icon,
        onClick = { onOpen(view.game) },
        best = best,
        locked = !playable,
        lockedLabel = stringResource(R.string.path_locked),
        // While all of Practice is locked the card at the top says why; a Game alone says what opens it.
        reason = when {
            !open -> null
            view.state == GameState.NEEDS_MORE -> stringResource(words.needs)
            view.state == GameState.NEEDS_SOUND -> stringResource(R.string.practice_needs_sound)
            else -> null
        },
    )
}
