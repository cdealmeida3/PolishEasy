package com.polisheasy.engine

import java.time.Clock
import java.time.LocalDate
import kotlin.random.Random

/** The kinds of Game, as the Practice screen groups them. */
enum class GameGroup { WORDS, PHRASES, VERBS, SOUNDS, NUMBERS }

/**
 * A Game of Practice. [id] is stable: Bests are saved under it. With [fewestWins] the Best is the
 * lowest result, such as the fewest turns; otherwise the highest, such as the most right. A Game
 * without [rounds] is opened and looked at, not played to a result.
 */
/**
 * A Game. [chapter] is the book's chapter whose own Game it is, listed on that chapter's Lesson
 * screen; null for the Games of every chapter.
 */
enum class Game(val id: String, val group: GameGroup, val fewestWins: Boolean = false, val rounds: Boolean = true, val chapter: Int? = null) {
    /** Nothing is asked: the cards are looked at and listened to. It has no Rounds and no Best. */
    PICTURE_CARDS("picture-cards", GameGroup.WORDS, rounds = false),
    MEMORY_PAIRS("memory-pairs", GameGroup.WORDS, fewestWins = true),
    /** A conversation with a person, replied to by choosing, typing or saying. */
    CHAT("chat", GameGroup.PHRASES),
    PHRASE_CHAINS("phrase-chains", GameGroup.PHRASES, fewestWins = true),
    /** Phrases sorted against the clock by who they are said to: formal, informal or to anyone. */
    REGISTER_SORT("register-sort", GameGroup.PHRASES, chapter = 1),
    /** People introduce themselves, heard or read, and the Learner fills in a form for each. */
    ANKIETA("ankieta", GameGroup.PHRASES, chapter = 2),
    VERB_CHALLENGE("verb-challenge", GameGroup.VERBS),
    /** Chapter 6's Game: a verb is sorted into its conjugation: -m, -sz; -ę, -esz; or -ę, -isz / -ysz. */
    VERB_GROUPS("verb-groups", GameGroup.VERBS, chapter = 6),
    LISTEN_AND_TAP("listen-and-tap", GameGroup.SOUNDS),
    SOUND_GAPS("sound-gaps", GameGroup.SOUNDS),
    SPELL_IT("spell-it", GameGroup.WORDS),
    QUICK_ROUND("quick-round", GameGroup.WORDS),
    MOJ_MOJA_MOJE("moj-moja-moje", GameGroup.WORDS),
    /** Chapter 3's Game: nouns sorted under ten, ta or to. */
    TEN_TA_TO("ten-ta-to", GameGroup.WORDS, chapter = 3),
    /** Chapter 3's Game: a board of pictures labelled one at a time, by choosing or typing. */
    LABEL_IT("label-the-picture", GameGroup.WORDS, chapter = 3),
    /** Chapter 4's Game: two adjectives side by side, against the clock: opposites or not? */
    OPPOSITES_SNAP("opposites-snap", GameGroup.WORDS, chapter = 4),
    /** Chapter 5's Game: a job is shown, and the Learner says who the person is: on jest lekarzem. */
    WHOS_WHO("whos-who", GameGroup.WORDS, chapter = 5),
    /** Chapter 6's Game: a hobby is acted out in a picture, and the Learner names it: jeździć na rowerze. */
    HOBBY_CHARADES("hobby-charades", GameGroup.WORDS, chapter = 6),
    NUMBERS("numbers", GameGroup.NUMBERS),
    /** Chapter 7's Game: a price is said, and the Learner pays it exactly with coins and notes. */
    KIOSK_TILL("kiosk-till", GameGroup.NUMBERS, chapter = 7),
    /** Chapter 7's Game: numbers up to a thousand are called, and the Learner ticks them on a card. */
    LOTTO("lotto", GameGroup.NUMBERS, chapter = 7),
    /** Chapter 8's Game: a dish is ordered, chleb z masłem, and the Learner puts its foods on a plate. */
    PLATE_BUILDER("plate-builder", GameGroup.WORDS, chapter = 8),
    /** Chapter 9's Game: a shopping list is said, dwie gruszki i trzy banany, and the Learner fills the basket. */
    MARKET_LIST("market-list", GameGroup.NUMBERS, chapter = 9),
    /** Chapter 9's Game: products sorted into the supermarket's aisles against the clock: nabiał, pieczywo, słodycze. */
    AISLE_SORT("aisle-sort", GameGroup.WORDS, chapter = 9),
    /** Chapter 10's Game: a table orders, dla mnie żurek, a dla mnie to samo, and the Learner, the waiter, writes each guest's dish. */
    TAKE_THE_ORDER("take-the-order", GameGroup.PHRASES, chapter = 10),
    /** Chapter 10's Game: a sentence negated against the clock, the object in the genitive: Lubię kawę, Nie lubię kawy. */
    NEGATION_FLIP("negation-flip", GameGroup.PHRASES, chapter = 10),
    /** Chapter 11's Game: a family tree seen from the Learner, each relative named or found: mój wujek, moja ciocia. */
    FAMILY_TREE("family-tree", GameGroup.WORDS, chapter = 11),
    /** Chapter 11's Game: a time on the clock, and when in the day it is, against the clock: 7:30 is rano, 23:00 wieczorem. */
    TIMES_OF_DAY("times-of-day", GameGroup.WORDS, chapter = 11),
    /** Chapter 12's Game: a time on a clock face said in Polish, or found from it: pięć po ósmej, wpół do dziewiątej. */
    TELL_THE_TIME("tell-the-time", GameGroup.NUMBERS, chapter = 12),
    /** Chapter 12's Game: a week's plans heard or read, W piątek o dwudziestej: impreza, and put in the diary at their day and time. */
    WEEK_PLANNER("week-planner", GameGroup.PHRASES, chapter = 12),
    /** Chapter 13's Game: a short story in the past heard or read, Najpierw wstałam o siódmej. Potem umyłam zęby., and its pictures put in order. */
    STORY_STRIP("story-strip", GameGroup.PHRASES, chapter = 13),
    /** Chapter 13's Game: a sentence in the past heard or read, and who said it, from the verb's ending: byłem, byłam, byliśmy, byłyśmy. */
    WHO_SAID_IT("who-said-it", GameGroup.VERBS, chapter = 13),
    /** Chapter 14's Game: a route heard or read, Proszę iść prosto, potem skręcić w lewo., and the place on a small map it leads to. */
    DIRECTIONS("directions", GameGroup.PHRASES, chapter = 14),
    /** Chapter 14's Game: a sentence with its noun marked, Kocham tatę., and the pronoun that says it shorter: go, ją, je, ich, or after na, niego, nią, nich. */
    PRONOUN_SWAP("pronoun-swap", GameGroup.WORDS, chapter = 14),
    /** Chapter 15's Game: what is in a container, ___ coli, and the container it comes in: puszka, słoik, kostka. */
    PACKAGING("packaging", GameGroup.WORDS, chapter = 15),
    /** Chapter 15's Game: a picture shown two to ten times and the phrase that counts them: pięć butelek, not pięć butelki. */
    HOW_MANY("how-many", GameGroup.WORDS, chapter = 15),
    /** Chapter 16's Game: tomorrow's forecast heard or read, and its card made: the weather signs it names and its temperature. */
    FORECASTER("forecaster", GameGroup.PHRASES, chapter = 16),
    /** Chapter 16's Game: learned words hidden in a grid of letters, found by what they mean. */
    WORD_SEARCH("word-search", GameGroup.WORDS, chapter = 16),
    /** Chapter 17's Game: the table of the pronouns filled a square at a time, *ze mną*, *o nim*, *komu? jej*. */
    PRONOUN_GRID("pronoun-grid", GameGroup.WORDS, chapter = 17),
}

/** Whether a Game can be played now. */
enum class GameState {
    READY,

    /** The Learner has not yet learned enough for it. */
    NEEDS_MORE,

    /** It is only listening, and sound is off. */
    NEEDS_SOUND,
}

/**
 * Practice as its screen shows it. [open] once today's Daily Set is completed. [roundsToday] is
 * how many Rounds were finished today; it resets each day and nothing is bought with it.
 */
data class PracticeView(val open: Boolean, val roundsToday: Int, val games: List<GameView>)

/** A Game on the Practice screen: whether it can be played, and the Best of the way it was [lastPlayed]. */
data class GameView(
    val game: Game,
    val state: GameState,
    val best: Int? = null,
    val lastPlayed: List<OptionView> = emptyList(),
)

/**
 * One way to play, such as a verb or a level. [id] is stable. [label] is set when the name comes
 * from the content, such as a verb or a Unit's title; otherwise the app names it.
 */
data class OptionView(val id: String, val label: String? = null)

/** One thing to choose before a Round, such as the verb: its [options] and the one [selected]. */
data class ChoiceView(val id: String, val options: List<OptionView>, val selected: String)

/** A Game before a Round: what there is to choose, and the Best of what is chosen now. */
data class GameSetup(val game: Game, val choices: List<ChoiceView>, val best: Int? = null) {
    /** The id of the option selected for each choice, in order: the way the Round will be played. */
    val selected: List<String> get() = choices.map { it.selected }
}

/** What a Round came to: [value] is what the Best is kept of, out of [of] where that means something. */
data class Score(val value: Int, val of: Int? = null)

/**
 * One play of a Game from start to result. Each Game has its own, an immutable state with moves
 * that return the next state. [played] is the way it is being played, as option ids.
 */
interface Round {
    val game: Game
    val played: List<String>

    /** How far through the Round the Learner is: [done] of [total]. */
    val done: Int
    val total: Int
    val finished: Boolean

    /**
     * Which of its steps the Round is showing. It changes when what is shown is replaced, which the
     * screen slides; it stays the same while the Learner answers, and while one thing is filled in bit by bit.
     */
    val step: Int

    /** What the Round came to. Read it once the Round is [finished]. */
    val score: Score

    /** What this Round asks, by stable keys, so the next Round can ask something else first. */
    val asked: List<String>
}

/**
 * A finished Round with the Best it leaves. [newBest] when it beat the Best there was;
 * [first] when there was none, so this result is now the Best.
 */
data class RoundResult(
    val game: Game,
    val played: List<String>,
    val score: Score,
    val best: Int,
    val newBest: Boolean = false,
    val first: Boolean = false,
)

/** What was answered to a question: [text] as given, and whether it was right. */
data class Given(val text: String, val correct: Boolean)

/**
 * A run of questions, each answered once. A right answer and a wrong one both move on; only the
 * first try counts.
 */
data class Run<Q>(
    val questions: List<Q>,
    val index: Int = 0,
    /** Whether each question answered so far was right. */
    val results: List<Boolean> = emptyList(),
    /** The answer to the question being shown, until the Learner moves on. */
    val given: Given? = null,
) {
    val current: Q? get() = questions.getOrNull(index)
    val finished: Boolean get() = index >= questions.size
    val firstTry: Int get() = results.count { it }

    /** Answers the question being shown. No change if it is already answered or none is left. */
    fun answer(text: String, correct: Boolean): Run<Q> =
        if (given != null || finished) this else copy(given = Given(text, correct), results = results + correct)

    /** Moves on from an answered question. No change while it is unanswered. */
    fun next(): Run<Q> = if (given == null) this else copy(index = index + 1, given = null)
}

/** What the Learner has learned: the completed Daily Sets in Path order, and what they hold. */
internal class Learned(
    val course: Course,
    progress: LearnerProgress,
    val language: String,
) {
    private val completed: Set<String> = progress.completedDailySets().keys

    val dailySets: List<DailySet> = course.path.filter { it.id in completed }

    /** The Items each completed Daily Set introduced, by its id. */
    private val introduced: Map<String, List<Item>> = buildMap {
        val met = mutableSetOf<String>()
        course.path.forEach { dailySet ->
            val new = dailySet.itemIds().filter(met::add).mapNotNull(course.itemsById::get)
            if (dailySet.id in completed) put(dailySet.id, new)
        }
    }

    /** The Items of completed Daily Sets, once each, in Path order. */
    val items: List<Item> = dailySets.flatMap { it.itemIds() }.distinct().mapNotNull(course.itemsById::get)

    /** The learned Items that have a picture. */
    val pictured: List<Item> = items.filter { it.id in course.pictures }

    /** Each Unit the Learner has completed a Daily Set of, with the Items those Daily Sets introduced. */
    val units: List<Pair<CourseUnit, List<Item>>> = course.levels.flatMap { it.units }.mapNotNull { unit ->
        val done = unit.dailySets.filter { it.id in completed }
        if (done.isEmpty()) null else unit to introducedBy(done)
    }

    val itemProgress: Map<String, ItemProgress> = progress.itemProgress()

    /** The Items [dailySets] introduced, in order. */
    fun introducedBy(dailySets: List<DailySet>): List<Item> = dailySets.flatMap { introduced[it.id].orEmpty() }

    fun localize(text: Localized): String = text.getValue(language)
}

/** The rules of one Game: when it can be played, the ways to play it, and how a Round is made. */
internal interface GameRules {
    val game: Game

    fun state(learned: Learned, sound: Boolean): GameState

    /**
     * What there is to choose, each choice with [wanted]'s option selected where it is offered, or
     * else its first. Without [sound], a way to play that is only listening is not offered. Nothing,
     * for a Game with nothing to choose.
     */
    fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> = emptyList()

    /** As [choices], where [speech] says whether the Learner can be heard: a way to play that is only speaking is offered only with it. */
    fun choices(learned: Learned, wanted: List<String>, sound: Boolean, speech: Boolean): List<ChoiceView> = choices(learned, wanted, sound)

    /**
     * A new Round played the [selected] way. [seen] is what this Game has already asked today, to
     * ask something else where it can, and [avoidFirst] what the last Round opened with.
     */
    fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round =
        error("$game has no Rounds")

    /** As [start], where [speech] says whether the Learner can be heard. */
    fun start(learned: Learned, selected: List<String>, sound: Boolean, speech: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round =
        start(learned, selected, sound, random, seen, avoidFirst)
}

/** A choice with the option [wanted] selected if it is among [options], or else the first. */
internal fun choice(id: String, options: List<OptionView>, wanted: String?): ChoiceView =
    ChoiceView(id, options, options.firstOrNull { it.id == wanted }?.id ?: options.first().id)

/**
 * [count] of [from], in a random order, taking first those whose [key] is not in [seen]. The
 * first is never the one whose key is [avoidFirst] when there is another to put there.
 */
internal fun <T> Random.draw(from: List<T>, count: Int, seen: Set<String>, avoidFirst: String?, key: (T) -> String): List<T> {
    val picked = from.shuffled(this).sortedBy { key(it) in seen }.take(count).shuffled(this)
    return if (picked.size > 1 && key(picked.first()) == avoidFirst) picked.drop(1) + picked.first() else picked
}

/**
 * Practice: the Games outside the Path, open each day once today's Daily Set is completed. It
 * reads what the Learner has learned and never changes it: a Round saves only the Game's Best, in
 * [bests], so it cannot advance the Path, raise the Streak or reschedule an Item.
 */
class Practice internal constructor(
    private val course: Course,
    private val progress: LearnerProgress,
    private val bests: PracticeProgress,
    private val clock: Clock,
    private val baseLanguage: String,
    private val random: Random,
    private val isOpen: () -> Boolean,
    /** Whether every Game is open with the whole book, as if every Daily Set were completed. */
    private val everything: () -> Boolean = { false },
) {
    private val rules: Map<Game, GameRules> = listOf(ChatRules, PictureCards, MemoryPairs, PhraseChains, RegisterSort, Ankieta, VerbChallenge, VerbGroups, ListenAndTap, SoundGaps, SpellIt, QuickRoundRules, MojMojaMoje, TenTaTo, LabelIt, OppositesSnap, WhosWho, HobbyCharades, Numbers, KioskTill, Lotto, PlateBuilder, MarketList, AisleSort, TakeTheOrder, NegationFlip, FamilyTree, TimesOfDay, TellTheTime, WeekPlanner, StoryStrip, WhoSaidIt, Directions, PronounSwap, PackagingMatch, HowMany, Forecaster, WordSearch, PronounGrid).associateBy { it.game }

    /** What each Game asked today, so a new Round asks something else first, and what its last Round opened with. */
    private var seenOn: LocalDate? = null
    private val seen = mutableMapOf<Game, MutableSet<String>>()
    private val lastFirst = mutableMapOf<Game, String>()

    /**
     * Practice as its screen shows it. With [sound] false, a Game that is only listening cannot be
     * played. [speech] is whether the Learner can be heard, for the ways to play that are speaking.
     */
    fun view(sound: Boolean = true, speech: Boolean = false): PracticeView {
        val learned = learned()
        return PracticeView(
            open = isOpen(),
            roundsToday = bests.roundsOn(today()),
            games = Game.entries.map { game -> gameView(game, learned, sound, speech) },
        )
    }

    private fun gameView(game: Game, learned: Learned, sound: Boolean, speech: Boolean): GameView {
        val of = rules.getValue(game)
        val state = of.state(learned, sound)
        val last = bests.lastPlayed(game.id)
        if (state == GameState.NEEDS_MORE || last == null || !game.rounds) return GameView(game, state)
        // The way it was last played, if that can still be played; its Best is the one to beat.
        val choices = of.choices(learned, last, sound, speech)
        val played = choices.map { it.selected }
        return GameView(
            game,
            state,
            best = bests.best(key(game, played)),
            lastPlayed = choices.map { choice -> choice.options.first { it.id == choice.selected } },
        )
    }

    /**
     * [game] before a Round, with [wanted] selected: the id of an option for each choice, in
     * order. Without it, the way the Game was last played. An option that is not offered, or no
     * longer is, gives way to the first of its choice. Without [sound], a way to play that is only
     * listening is not offered, and without [speech] one that is speaking. Fails unless the Game
     * has enough to play with.
     */
    fun setup(game: Game, wanted: List<String>? = null, sound: Boolean = true, speech: Boolean = false): GameSetup {
        check(game.rounds) { "$game has no Rounds" }
        val of = rules.getValue(game)
        val learned = learned()
        check(of.state(learned, sound = true) != GameState.NEEDS_MORE) { "$game has nothing to play with yet" }
        val choices = of.choices(learned, wanted ?: bests.lastPlayed(game.id).orEmpty(), sound, speech)
        return GameSetup(game, choices, bests.best(key(game, choices.map { it.selected })))
    }

    /**
     * Starts a Round of [setup]'s Game, played the way it selects. Fails unless Practice is open
     * and the Game can be played. Nothing is saved until the Round is finished.
     */
    fun start(setup: GameSetup, sound: Boolean = true, speech: Boolean = false): Round {
        check(isOpen()) { "Practice is locked until today's Daily Set is completed" }
        val of = rules.getValue(setup.game)
        val learned = learned()
        check(of.state(learned, sound) == GameState.READY) { "${setup.game} cannot be played now" }
        if (seenOn != today()) {
            seen.clear()
            seenOn = today()
        }
        val asked = seen.getOrPut(setup.game) { mutableSetOf() }
        val selected = of.choices(learned, setup.selected, sound, speech).map { it.selected }
        return of.start(learned, selected, sound, speech, random, asked.toSet(), lastFirst[setup.game]).also { round ->
            asked += round.asked
            round.asked.firstOrNull()?.let { lastFirst[setup.game] = it }
        }
    }

    /**
     * Ends a finished [round]: it counts as one of today's Rounds, and its result becomes the Best
     * of the way it was played if it is the first or beats the one there was. Nothing else is saved.
     */
    fun finish(round: Round): RoundResult {
        check(round.finished) { "The Round of ${round.game} is not finished" }
        val key = key(round.game, round.played)
        val previous = bests.best(key)
        val value = round.score.value
        val better = previous == null || if (round.game.fewestWins) value < previous else value > previous
        bests.recordRound(round.game.id, round.played, key, best = value.takeIf { better }, on = today())
        return RoundResult(
            round.game,
            round.played,
            round.score,
            best = if (better) value else checkNotNull(previous),
            newBest = better && previous != null,
            first = previous == null,
        )
    }

    /**
     * Picture cards: the cards of the [deck] and [kind] wanted, "pictures" or "words". Without
     * them, or when they are not offered, every card that has a picture. Fails unless Practice is
     * open and there are enough pictures to show.
     */
    fun pictureCards(deck: String? = null, kind: String? = null): PictureCardsView {
        check(isOpen()) { "Practice is locked until today's Daily Set is completed" }
        val learned = learned()
        check(PictureCards.state(learned, sound = true) == GameState.READY) { "There are not enough pictures to show yet" }
        return PictureCards.view(learned, deck, kind)
    }

    /** The Clip that says [polish], whatever its capitals and spacing, or null if there is none. */
    fun clipOf(polish: String): ClipView? = course.clipFor(polish)

    /**
     * The Games of the chapter of Unit [unitId], as its Lesson screen lists them: its own Games,
     * then each conversation of the Chat it brings: the first chapter of the book by which every
     * Item of the conversation is taught, or for one the book does not teach, the first Unit.
     */
    fun chapter(unitId: String, sound: Boolean = true, speech: Boolean = false): ChapterGamesView {
        val units = course.levels.flatMap { it.units }
        val unit = units.firstOrNull { it.id == unitId } ?: return ChapterGamesView(emptyList(), emptyList())
        val learned = learned()
        val games = if (unit.number == null) emptyList() else Game.entries.filter { it.chapter == unit.number }.map { gameView(it, learned, sound, speech) }
        val known = learned.items.map { it.id }.toSet()
        val chapterOf = mutableMapOf<String, String>()
        listOf(units.filter { it.number != null }, units).forEach { among ->
            val taught = mutableSetOf<String>()
            among.forEach { book ->
                taught += book.dailySets.flatMap { it.itemIds() }
                course.chats.forEach { script -> if (script.id !in chapterOf && taught.containsAll(script.items)) chapterOf[script.id] = book.id }
            }
        }
        val chats = course.chats
            .filter { it.turns.isNotEmpty() && chapterOf[it.id] == unitId }
            .map { ChapterChatView(it.id, localize(it.title), ready = known.containsAll(it.items)) }
        return ChapterGamesView(games, chats)
    }

    /** The Chat before a Round, with the conversation [scriptId] chosen and the way it was last replied. Fails unless it can be played. */
    fun chatSetup(scriptId: String, sound: Boolean = true, speech: Boolean = false): GameSetup =
        setup(Game.CHAT, listOf(scriptId) + bests.lastPlayed(Game.CHAT.id).orEmpty().drop(1), sound, speech)

    private fun localize(text: Localized): String = text[baseLanguage] ?: text.values.first()

    private fun learned() = Learned(course, if (everything()) EverythingCompleted(course, progress) else progress, baseLanguage)

    private fun today(): LocalDate = LocalDate.now(clock)

    /** What a Best is saved under: the Game and the way it was played. */
    private fun key(game: Game, played: List<String>): String = (listOf(game.id) + played).joinToString(":")
}

/** A chapter's Games on its Lesson screen: its own [games] and the conversations of the Chat it brings. */
data class ChapterGamesView(val games: List<GameView>, val chats: List<ChapterChatView>)

/** A conversation of the Chat, [ready] once every Item of it is learned. */
data class ChapterChatView(val id: String, val title: String, val ready: Boolean)

/** [progress] as Practice reads it when every Game is open: every Daily Set on the Path counts as completed. */
private class EverythingCompleted(course: Course, private val progress: LearnerProgress) : LearnerProgress by progress {
    private val all: Map<String, LocalDate> = course.path.associate { it.id to LocalDate.MIN }

    override fun completedDailySets(): Map<String, LocalDate> = all + progress.completedDailySets()
}
