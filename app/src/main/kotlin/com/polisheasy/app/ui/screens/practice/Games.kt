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
import com.polisheasy.engine.TimedRound

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
    Game.CHAT ->
        GameWords(R.string.game_chat, R.string.game_chat_note, R.string.game_chat_needs, R.drawable.game_chat)
    Game.REGISTER_SORT ->
        GameWords(R.string.game_register_sort, R.string.game_register_sort_note, R.string.game_register_sort_needs, R.drawable.game_register_sort)
    Game.ANKIETA ->
        GameWords(R.string.game_ankieta, R.string.game_ankieta_note, R.string.game_ankieta_needs, R.drawable.game_ankieta)
    Game.PHRASE_CHAINS ->
        GameWords(R.string.game_phrase_chains, R.string.game_phrase_chains_note, R.string.game_phrase_chains_needs, R.drawable.game_phrase_chains)
    Game.LISTEN_AND_TAP ->
        GameWords(R.string.game_listen_and_tap, R.string.game_listen_and_tap_note, R.string.game_listen_and_tap_needs, R.drawable.game_listen_and_tap)
    Game.SOUND_GAPS ->
        GameWords(R.string.game_sound_gaps, R.string.game_sound_gaps_note, R.string.game_sound_gaps_needs, R.drawable.game_sound_gaps)
    Game.SPELL_IT ->
        GameWords(R.string.game_spell_it, R.string.game_spell_it_note, R.string.game_spell_it_needs, R.drawable.game_spell_it)
    Game.QUICK_ROUND ->
        GameWords(R.string.game_quick_round, R.string.game_quick_round_note, R.string.game_quick_round_needs, R.drawable.game_quick_round)
    Game.MOJ_MOJA_MOJE ->
        GameWords(R.string.game_moj_moja_moje, R.string.game_moj_moja_moje_note, R.string.game_moj_moja_moje_needs, R.drawable.game_moj_moja_moje)
    Game.TEN_TA_TO ->
        GameWords(R.string.game_ten_ta_to, R.string.game_ten_ta_to_note, R.string.game_ten_ta_to_needs, R.drawable.game_ten_ta_to)
    Game.LABEL_IT ->
        GameWords(R.string.game_label_it, R.string.game_label_it_note, R.string.game_label_it_needs, R.drawable.game_label_it)
    Game.OPPOSITES_SNAP ->
        GameWords(R.string.game_opposites_snap, R.string.game_opposites_snap_note, R.string.game_opposites_snap_needs, R.drawable.game_opposites_snap)
    Game.WHOS_WHO ->
        GameWords(R.string.game_whos_who, R.string.game_whos_who_note, R.string.game_whos_who_needs, R.drawable.game_whos_who)
    Game.NUMBERS ->
        GameWords(R.string.game_numbers, R.string.game_numbers_note, R.string.game_numbers_needs, R.drawable.game_numbers)
    Game.VERB_CHALLENGE ->
        GameWords(R.string.game_verb_challenge, R.string.game_verb_challenge_note, R.string.game_verb_challenge_needs, R.drawable.game_verb_challenge)
    Game.VERB_GROUPS ->
        GameWords(R.string.game_verb_groups, R.string.game_verb_groups_note, R.string.game_verb_groups_needs, R.drawable.game_verb_groups)
    Game.HOBBY_CHARADES ->
        GameWords(R.string.game_hobby_charades, R.string.game_hobby_charades_note, R.string.game_hobby_charades_needs, R.drawable.game_hobby_charades)
    Game.KIOSK_TILL ->
        GameWords(R.string.game_kiosk_till, R.string.game_kiosk_till_note, R.string.game_kiosk_till_needs, R.drawable.game_kiosk_till)
    Game.LOTTO ->
        GameWords(R.string.game_lotto, R.string.game_lotto_note, R.string.game_lotto_needs, R.drawable.game_lotto)
    Game.PLATE_BUILDER ->
        GameWords(R.string.game_plate_builder, R.string.game_plate_builder_note, R.string.game_plate_builder_needs, R.drawable.game_plate_builder)
    Game.MARKET_LIST ->
        GameWords(R.string.game_market_list, R.string.game_market_list_note, R.string.game_market_list_needs, R.drawable.game_market_list)
    Game.AISLE_SORT ->
        GameWords(R.string.game_aisle_sort, R.string.game_aisle_sort_note, R.string.game_aisle_sort_needs, R.drawable.game_aisle_sort)
    Game.TAKE_THE_ORDER ->
        GameWords(R.string.game_take_the_order, R.string.game_take_the_order_note, R.string.game_take_the_order_needs, R.drawable.game_take_the_order)
    Game.NEGATION_FLIP ->
        GameWords(R.string.game_negation_flip, R.string.game_negation_flip_note, R.string.game_negation_flip_needs, R.drawable.game_negation_flip)
    Game.FAMILY_TREE ->
        GameWords(R.string.game_family_tree, R.string.game_family_tree_note, R.string.game_family_tree_needs, R.drawable.game_family_tree)
    Game.TIMES_OF_DAY ->
        GameWords(R.string.game_times_of_day, R.string.game_times_of_day_note, R.string.game_times_of_day_needs, R.drawable.game_times_of_day)
    Game.TELL_THE_TIME ->
        GameWords(R.string.game_tell_the_time, R.string.game_tell_the_time_note, R.string.game_tell_the_time_needs, R.drawable.game_tell_the_time)
    Game.WEEK_PLANNER ->
        GameWords(R.string.game_week_planner, R.string.game_week_planner_note, R.string.game_week_planner_needs, R.drawable.game_week_planner)
    Game.STORY_STRIP ->
        GameWords(R.string.game_story_strip, R.string.game_story_strip_note, R.string.game_story_strip_needs, R.drawable.game_story_strip)
    Game.WHO_SAID_IT ->
        GameWords(R.string.game_who_said_it, R.string.game_who_said_it_note, R.string.game_who_said_it_needs, R.drawable.game_who_said_it)
    Game.DIRECTIONS ->
        GameWords(R.string.game_directions, R.string.game_directions_note, R.string.game_directions_needs, R.drawable.game_directions)
    Game.PRONOUN_SWAP ->
        GameWords(R.string.game_pronoun_swap, R.string.game_pronoun_swap_note, R.string.game_pronoun_swap_needs, R.drawable.game_pronoun_swap)
    Game.PACKAGING ->
        GameWords(R.string.game_packaging, R.string.game_packaging_note, R.string.game_packaging_needs, R.drawable.game_packaging)
    Game.HOW_MANY ->
        GameWords(R.string.game_how_many, R.string.game_how_many_note, R.string.game_how_many_needs, R.drawable.game_how_many)
    Game.FORECASTER ->
        GameWords(R.string.game_forecaster, R.string.game_forecaster_note, R.string.game_forecaster_needs, R.drawable.game_forecaster)
    Game.WORD_SEARCH ->
        GameWords(R.string.game_word_search, R.string.game_word_search_note, R.string.game_word_search_needs, R.drawable.game_word_search)
    Game.PRONOUN_GRID ->
        GameWords(R.string.game_pronoun_grid, R.string.game_pronoun_grid_note, R.string.game_pronoun_grid_needs, R.drawable.game_pronoun_grid)
    Game.ROOM_SCENE ->
        GameWords(R.string.game_room_scene, R.string.game_room_scene_note, R.string.game_room_scene_needs, R.drawable.game_room_scene)
    Game.STOI_LEZY_WISI ->
        GameWords(R.string.game_stoi_lezy_wisi, R.string.game_stoi_lezy_wisi_note, R.string.game_stoi_lezy_wisi_needs, R.drawable.game_stoi_lezy_wisi)
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
    Game.VERB_CHALLENGE, Game.LISTEN_AND_TAP, Game.SOUND_GAPS, Game.MOJ_MOJA_MOJE, Game.TEN_TA_TO, Game.LABEL_IT, Game.NUMBERS, Game.SPELL_IT, Game.QUICK_ROUND, Game.REGISTER_SORT, Game.OPPOSITES_SNAP, Game.WHOS_WHO, Game.VERB_GROUPS, Game.HOBBY_CHARADES, Game.KIOSK_TILL, Game.LOTTO, Game.PLATE_BUILDER, Game.MARKET_LIST, Game.AISLE_SORT, Game.TAKE_THE_ORDER, Game.NEGATION_FLIP, Game.FAMILY_TREE, Game.TIMES_OF_DAY, Game.TELL_THE_TIME, Game.WEEK_PLANNER, Game.STORY_STRIP, Game.WHO_SAID_IT, Game.DIRECTIONS, Game.PRONOUN_SWAP, Game.PACKAGING, Game.HOW_MANY, Game.FORECASTER, Game.WORD_SEARCH, Game.CHAT, Game.ANKIETA, Game.PRONOUN_GRID, Game.ROOM_SCENE, Game.STOI_LEZY_WISI -> pluralStringResource(R.plurals.practice_right, value, value)
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
        "number" -> R.string.choice_number
        "from" -> R.string.choice_from
        "match" -> R.string.choice_match
        "time" -> R.string.choice_time
        "noun" -> R.string.choice_noun
        "chat" -> R.string.choice_chat
        "way" -> R.string.choice_way
        "intro" -> R.string.choice_intro
        "label" -> R.string.choice_label
        "job" -> R.string.choice_job
        "show" -> R.string.choice_show
        "answer" -> R.string.choice_answer
        "price" -> R.string.choice_price
        "order" -> R.string.choice_order
        "list" -> R.string.choice_list
        "table" -> R.string.choice_table
        "clock" -> R.string.choice_clock
        "plans" -> R.string.choice_plans
        "story" -> R.string.choice_story
        "lines" -> R.string.choice_lines
        "routes" -> R.string.choice_routes
        "sentences" -> R.string.choice_sentences
        "forecast" -> R.string.choice_forecast
        "pronouns" -> R.string.choice_pronouns
        "scenes" -> R.string.choice_scenes
        else -> error("No name for the choice $id")
    },
)

/** What a way to play is called: its own name where the content gives one, such as a verb. */
@Composable
internal fun optionLabel(option: OptionView): String = option.label
    ?: option.id.count(PAIRS)?.let { stringResource(R.string.option_pairs, it) }
    ?: option.id.count(SECONDS)?.let { stringResource(R.string.option_seconds, it) }
    ?: stringResource(
    when (option.id) {
        "picture-word" -> R.string.option_picture_word
        "listen" -> R.string.option_listen
        "read" -> R.string.option_read
        "short" -> R.string.option_short
        "medium" -> R.string.option_medium
        "long" -> R.string.option_long
        "picture" -> R.string.option_picture
        "meaning" -> R.string.option_meaning
        "aloud" -> R.string.option_aloud
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
        "say" -> R.string.option_say
        "word" -> R.string.option_word
        "to-10" -> R.string.option_to_10
        "to-20" -> R.string.option_to_20
        "to-100" -> R.string.option_to_100
        "runs" -> R.string.option_runs
        "sums" -> R.string.option_sums
        "verb" -> R.string.option_verb_only
        "with-ja" -> R.string.option_with_ja
        "coins" -> R.string.option_coins
        "notes" -> R.string.option_notes
        "hundreds" -> R.string.option_hundreds
        "to-1000" -> R.string.option_to_1000
        "one-dish" -> R.string.option_one_dish
        "two-dishes" -> R.string.option_two_dishes
        "one-good" -> R.string.option_one_good
        "two-goods" -> R.string.option_two_goods
        "two-guests" -> R.string.option_two_guests
        "three-guests" -> R.string.option_three_guests
        "name" -> R.string.option_name
        "find" -> R.string.option_find
        "tell" -> R.string.option_tell
        "find-clock" -> R.string.option_find_clock
        else -> error("No name for the option ${option.id}")
    },
)

/** The number an option such as "pairs-6" or "seconds-60" stands for, or null if it is another kind of option. */
private fun String.count(prefix: String): Int? = if (startsWith(prefix)) removePrefix(prefix).toIntOrNull() else null

private const val PAIRS = "pairs-"
private const val SECONDS = "seconds-"

/** How far a Round has come, for a screen reader: "3 of 14 answered", on a board "2 of 6 pairs found", against the clock "42 seconds left". */
@Composable
internal fun progressWords(round: Round): String = when (round) {
    is TimedRound -> pluralStringResource(R.plurals.quick_left, round.left, round.left)
    is MemoryRound -> stringResource(R.string.memory_progress, round.done, round.total)
    else -> stringResource(R.string.round_progress, round.done, round.total)
}
