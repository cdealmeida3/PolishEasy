package com.polisheasy.app.ui.screens.practice

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.polisheasy.app.R
import com.polisheasy.engine.Game
import com.polisheasy.engine.GameGroup
import com.polisheasy.engine.MemoryRound
import com.polisheasy.engine.OptionView
import com.polisheasy.engine.Round

/** What the screens call a Game: its name, one line of what it is, what opens it, and its icon. */
internal class GameWords(
    @StringRes val name: Int,
    @StringRes val note: Int,
    /** What the Learner has to learn before the Game can be played. */
    @StringRes val needs: Int,
    @DrawableRes val icon: Int,
)

internal fun Game.words(): GameWords = when (this) {
    Game.PICTURE_CARDS ->
        GameWords(R.string.game_picture_cards, R.string.game_picture_cards_note, R.string.game_picture_cards_needs, R.drawable.game_picture_cards)
    Game.MEMORY_PAIRS ->
        GameWords(R.string.game_memory_pairs, R.string.game_memory_pairs_note, R.string.game_memory_pairs_needs, R.drawable.game_memory_pairs)
    Game.PHRASE_CHAINS ->
        GameWords(R.string.game_phrase_chains, R.string.game_phrase_chains_note, R.string.game_phrase_chains_needs, R.drawable.game_phrase_chains)
    Game.LISTEN_AND_TAP ->
        GameWords(R.string.game_listen_and_tap, R.string.game_listen_and_tap_note, R.string.game_listen_and_tap_needs, R.drawable.game_listen_and_tap)
    Game.SOUND_GAPS ->
        GameWords(R.string.game_sound_gaps, R.string.game_sound_gaps_note, R.string.game_sound_gaps_needs, R.drawable.game_sound_gaps)
    Game.VERB_CHALLENGE ->
        GameWords(R.string.game_verb_challenge, R.string.game_verb_challenge_note, R.string.game_verb_challenge_needs, R.drawable.game_verb_challenge)
}

@StringRes
internal fun GameGroup.title(): Int = when (this) {
    GameGroup.WORDS -> R.string.practice_group_words
    GameGroup.PHRASES -> R.string.practice_group_phrases
    GameGroup.VERBS -> R.string.practice_group_verbs
    GameGroup.SOUNDS -> R.string.practice_group_sounds
    GameGroup.NUMBERS -> R.string.practice_group_numbers
}

/** A Best in words, as the result of [game] is counted: "14 right". */
@Composable
internal fun bestValue(game: Game, value: Int): String = when (game) {
    Game.VERB_CHALLENGE, Game.LISTEN_AND_TAP, Game.SOUND_GAPS -> pluralStringResource(R.plurals.practice_right, value, value)
    Game.PHRASE_CHAINS -> pluralStringResource(R.plurals.practice_slips, value, value)
    Game.MEMORY_PAIRS -> pluralStringResource(R.plurals.practice_turns, value, value)
    Game.PICTURE_CARDS -> error("Picture cards has no Best")
}

/** What one thing to choose before a Round is called: "Verb", "Level". */
@Composable
internal fun choiceLabel(id: String): String = stringResource(
    when (id) {
        "verb" -> R.string.choice_verb
        "level" -> R.string.choice_level
        "deck" -> R.string.choice_deck
        "cards" -> R.string.choice_cards
        "pairs" -> R.string.choice_pairs
        "sounds" -> R.string.choice_sounds
        "clue" -> R.string.choice_clue
        "length" -> R.string.choice_length
        "tap" -> R.string.choice_tap
        "size" -> R.string.choice_size
        else -> error("No name for the choice $id")
    },
)

/** What a way to play is called: its own name where the content gives one, such as a verb. */
@Composable
internal fun optionLabel(option: OptionView): String = option.label ?: option.id.pairs()?.let { stringResource(R.string.option_pairs, it) } ?: stringResource(
    when (option.id) {
        "picture-word" -> R.string.option_picture_word
        "listen" -> R.string.option_listen
        "read" -> R.string.option_read
        "short" -> R.string.option_short
        "medium" -> R.string.option_medium
        "long" -> R.string.option_long
        "picture" -> R.string.option_picture
        "meaning" -> R.string.option_meaning
        "word-meaning" -> R.string.option_word_meaning
        "sound-word" -> R.string.option_sound_word
        "mixed" -> R.string.option_mixed
        "all" -> R.string.option_all
        "pictures" -> R.string.option_pictures
        "words" -> R.string.option_words
        "latest" -> R.string.option_latest
        "tricky" -> R.string.option_tricky
        "choose" -> R.string.option_choose
        "type" -> R.string.option_type
        else -> error("No name for the option ${option.id}")
    },
)

/** The number of pairs an option for the size of a board stands for, or null if it is another kind of option. */
private fun String.pairs(): Int? = if (startsWith(PAIRS)) removePrefix(PAIRS).toIntOrNull() else null

private const val PAIRS = "pairs-"

/** How far a Round has come, for a screen reader: "3 of 14 answered", or on a board "2 of 6 pairs found". */
@Composable
internal fun progressWords(round: Round): String =
    stringResource(if (round is MemoryRound) R.string.memory_progress else R.string.round_progress, round.done, round.total)
