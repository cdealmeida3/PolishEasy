package com.polisheasy.engine

import kotlin.random.Random

/**
 * One forecast of Forecaster: its [text] (*Jutro będzie padać śnieg. Będą dwa stopnie poniżej
 * zera.*), what it means, the [weather] it names as Items with a Picture (*pada-snieg*), the
 * temperature it gives in [degrees] Celsius, and the [items] it uses.
 */
data class Forecast(val text: String, val meaning: Localized, val weather: List<String>, val degrees: Int, val items: List<String>)

/** A weather sign on Forecaster's board: its Item [id], its [picture], and what it [means], for a screen reader. */
data class WeatherSign(val id: String, val picture: Picture, val means: String)

/**
 * One forecast of a Forecaster Round: its [text] and [meaning], the signs [wanted] on the card, as
 * Item ids, the [board] they are picked from, the [temperatures] offered, and the right one, [degrees].
 */
data class ForecastQuestion(
    val key: String,
    val text: String,
    val meaning: String,
    val wanted: Set<String>,
    val board: List<WeatherSign>,
    val temperatures: List<Int>,
    val degrees: Int,
)

/**
 * A Round of Forecaster, chapter 16's Game: tomorrow's forecast is said, or with [listen] off
 * written, and the Learner makes its card: the weather signs it names, from the board, and its
 * temperature, from those offered. [placed] is what is on the card, in the order it was put
 * there, and [temperature] the one picked. Sent, the card is right if it holds exactly the signs
 * the forecast names and its temperature. The result is the cards right first try.
 */
data class ForecasterRound(
    override val played: List<String>,
    val listen: Boolean,
    val run: Run<ForecastQuestion>,
    val placed: List<String> = emptyList(),
    val temperature: Int? = null,
) : Round {
    override val game: Game get() = Game.FORECASTER
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.key }

    /** The forecast being shown, or null once the Round is finished. */
    val question: ForecastQuestion? get() = run.current

    /** The answer to the forecast being shown, until the Learner moves on: the Item ids on the card joined by commas, then the temperature. */
    val given: Given? get() = run.given

    /** Whether the card has a sign and a temperature, to send. */
    val canSend: Boolean get() = given == null && question != null && placed.isNotEmpty() && temperature != null

    /** Puts the sign [id] of the board on the card, or takes it off if it is there. No change if it is not on the board or the card is sent. */
    fun place(id: String): ForecasterRound {
        val question = question ?: return this
        if (given != null || question.board.none { it.id == id }) return this
        return copy(placed = if (id in placed) placed - id else placed + id)
    }

    /** Picks the temperature [degrees] of those offered. No change for one not offered, or once the card is sent. */
    fun pick(degrees: Int): ForecasterRound {
        val question = question ?: return this
        if (given != null || degrees !in question.temperatures) return this
        return copy(temperature = degrees)
    }

    /** Sends the card: right if it holds exactly the signs the forecast names and its temperature. */
    fun send(): ForecasterRound {
        val question = question ?: return this
        if (!canSend) return this
        val correct = placed.toSet() == question.wanted && temperature == question.degrees
        return copy(run = run.answer(placed.joinToString(",") + " $temperature", correct))
    }

    /** Moves on from a sent card, with the next card empty. */
    fun next(): ForecasterRound = if (given == null) this else copy(run = run.next(), placed = emptyList(), temperature = null)
}

/**
 * Forecaster: the forecasts of `forecasts` in practice.json. It opens once [FEWEST] forecasts have
 * all their Items learned and [BOARD] weather signs are learned with their Pictures.
 */
internal object Forecaster : GameRules {
    const val CLUE = "forecast"
    const val LISTEN = "listen"
    const val READ = "read"

    /** How many forecasts a Round asks, at most, and how many must be learned for it to open. */
    const val QUESTIONS = 6
    const val FEWEST = 4

    /** How many signs the board holds, and how many temperatures are offered. */
    const val BOARD = 6
    const val TEMPERATURES = 4

    /** Temperatures to offer beside the right one, so there are always enough. */
    private val OTHERS = listOf(-10, -5, -2, 0, 3, 5, 10, 15, 20, 25, 30)

    override val game = Game.FORECASTER

    /** The weather signs of the forecasts, learned and with a Picture, once each and each Picture once. */
    private fun signs(learned: Learned): List<WeatherSign> {
        val known = learned.items.associateBy { it.id }
        return learned.course.forecasts.flatMap { it.weather }.distinct().mapNotNull { id ->
            val item = known[id] ?: return@mapNotNull null
            val picture = learned.course.pictures[id] ?: return@mapNotNull null
            WeatherSign(id, picture, learned.localize(item.meaning))
        }.distinctBy { it.picture }
    }

    /** The forecasts whose Items and signs are all learned; heard, only those said by a Clip. */
    private fun forecasts(learned: Learned, listen: Boolean): List<Forecast> {
        val known = learned.items.map { it.id }.toSet()
        val signs = signs(learned).map { it.id }.toSet()
        return learned.course.forecasts.filter { forecast ->
            known.containsAll(forecast.items) && signs.containsAll(forecast.weather) && (!listen || learned.course.clipFor(forecast.text) != null)
        }
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (forecasts(learned, listen = false).size >= FEWEST && signs(learned).size >= BOARD) GameState.READY else GameState.NEEDS_MORE

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        val clues = listOf(LISTEN, READ).filter { it == READ || (sound && forecasts(learned, listen = true).size >= FEWEST) }
        return listOf(choice(CLUE, clues.map { OptionView(it) }, wanted.getOrNull(0)))
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val listen = selected.getOrNull(0) == LISTEN
        val signs = signs(learned)
        val questions = random.draw(forecasts(learned, listen), QUESTIONS, seen, avoidFirst) { it.text }.map { forecast ->
            val wanted = signs.filter { it.id in forecast.weather }
            val others = signs.filter { it.id !in forecast.weather }.shuffled(random).take(BOARD - wanted.size)
            ForecastQuestion(
                key = forecast.text,
                text = forecast.text,
                meaning = learned.localize(forecast.meaning),
                wanted = wanted.map { it.id }.toSet(),
                board = (wanted + others).shuffled(random),
                temperatures = temperatures(forecast.degrees, random),
                degrees = forecast.degrees,
            )
        }
        return ForecasterRound(selected, listen, Run(questions))
    }

    /** [TEMPERATURES] temperatures from coldest to warmest: [degrees], the same below or above zero, and others. */
    private fun temperatures(degrees: Int, random: Random): List<Int> {
        val mirrored = listOf(-degrees).filter { it != degrees }
        val others = OTHERS.filter { it != degrees && it !in mirrored }.shuffled(random)
        return (listOf(degrees) + mirrored + others).take(TEMPERATURES).sorted()
    }
}
