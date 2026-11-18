package com.polisheasy.engine

import kotlin.random.Random

/**
 * What Market list needs from the content, as Item ids: [and], which joins two goods (*i*), the
 * [numbers] one to four, each the word for it before a thing of each gender (*jeden, jedna,
 * jedno*; *dwa, dwie, dwa*), and the [goods] on the stall: nouns with a gender, a Picture and
 * their plural as their only form (*gruszka*, *gruszki*).
 */
data class Market(val and: String, val numbers: List<Map<Gender, String>>, val goods: List<String>)

/** A good on Market list's stall: its Item [id], its [picture], and what it [means], for a screen reader. */
data class StallGood(val id: String, val picture: Picture, val means: String)

/**
 * One shopping list of Market list: the words said (*dwie gruszki i trzy banany*), how many of
 * each good is [wanted], by Item id, and the [stall] they are picked from. [key] names the goods
 * and their numbers.
 */
data class MarketQuestion(
    val key: String,
    val words: List<String>,
    val wanted: Map<String, Int>,
    val stall: List<StallGood>,
) {
    /** The words as a sentence: a capital first and a full stop. */
    val polish: String get() = words.joinToString(" ").replaceFirstChar { it.uppercase() } + "."
}

/**
 * A Round of Market list, chapter 9's Game: a shopping list is said, or with [listen] off written,
 * and the Learner puts in the basket as many of each good as it asks, from the stall: *dwie
 * gruszki* is two pears. [basket] holds one entry for each good put in. Paid, the list is right if
 * the basket holds exactly what it asks. The result is the lists bought right first try.
 */
data class MarketRound(
    override val played: List<String>,
    val listen: Boolean,
    val run: Run<MarketQuestion>,
    val basket: List<String> = emptyList(),
) : Round {
    override val game: Game get() = Game.MARKET_LIST
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.key }

    /** The list being shown, or null once the Round is finished. */
    val question: MarketQuestion? get() = run.current

    /** The answer to the list being shown, until the Learner moves on. Its text is the basket's Item ids, joined by commas. */
    val given: Given? get() = run.given

    /** How many of each good is in the basket, by Item id. */
    val counts: Map<String, Int> get() = basket.groupingBy { it }.eachCount()

    /** The basket's goods with the same ones side by side, in the order each first went in, as the basket shows them. */
    val inBasket: List<String> get() = basket.distinct().flatMap { id -> List(counts.getValue(id)) { id } }

    /** Whether there is something in the basket to pay for. */
    val canPay: Boolean get() = given == null && basket.isNotEmpty() && question != null

    /** Puts one more of the good [id] of the stall in the basket, up to [MarketList.MOST]. No change if it is not on the stall or the list is answered. */
    fun add(id: String): MarketRound {
        val question = question ?: return this
        if (given != null || question.stall.none { it.id == id } || (counts[id] ?: 0) >= MarketList.MOST) return this
        return copy(basket = basket + id)
    }

    /** Takes one of the good [id] out of the basket. No change if there is none or the list is answered. */
    fun takeOut(id: String): MarketRound {
        if (given != null || id !in basket) return this
        val last = basket.lastIndexOf(id)
        return copy(basket = basket.filterIndexed { index, _ -> index != last })
    }

    /** Pays: right if the basket holds exactly as many of each good as the list asks. */
    fun pay(): MarketRound {
        val question = question ?: return this
        if (!canPay) return this
        return copy(run = run.answer(basket.joinToString(","), counts == question.wanted))
    }

    /** Moves on from a paid list, with the basket empty. */
    fun next(): MarketRound = if (given == null) this else copy(run = run.next(), basket = emptyList())
}

/**
 * Market list: once the words of the content's `market` numbers are all learned, in every gender,
 * and four of its goods (the noun, its gender, its plural and a Picture), lists of one good, or of two joined by *i*, each
 * one to four of it: *jedna gruszka*, *dwie gruszki*, *cztery banany*. The number agrees with the
 * good's gender and, from two on, the good is plural. The stall holds the goods wanted and others,
 * each Picture once.
 */
internal object MarketList : GameRules {
    const val CLUE = "list"
    const val LISTEN = "listen"
    const val READ = "read"
    const val LEVEL = "level"
    const val ONE = "one-good"
    const val TWO = "two-goods"

    /** The fewest goods the Game opens with, the most lists a Round asks, and the most of one good a list asks or the basket holds. */
    const val LEAST = 4
    const val QUESTIONS = 6
    const val MOST = 4

    /** How many goods the stall holds. */
    const val STALL = 6

    override val game = Game.MARKET_LIST

    /** One good of a list: how many, and the words that say it, *dwie gruszki*. */
    private class Want(val good: Item, val picture: Picture, val count: Int, val words: List<String>) {
        val key: String get() = "${good.id}:$count"
    }

    /**
     * Each learned good with a gender, a plural and a Picture, one to four of it; heard, only where
     * both words have Clips. None until every word of the numbers is learned, *jedna* and *dwie* too.
     */
    private fun wants(learned: Learned, listen: Boolean): List<Want> {
        val market = learned.course.market ?: return emptyList()
        val known = learned.items.associateBy { it.id }
        if (market.numbers.take(MOST).flatMap { it.values }.any { it !in known }) return emptyList()
        val pictures = learned.course.pictures
        return market.goods.distinct().flatMap { id ->
            val good = known[id] ?: return@flatMap emptyList()
            val gender = learned.course.genders[id] ?: return@flatMap emptyList()
            val picture = pictures[id] ?: return@flatMap emptyList()
            val plural = good.forms.singleOrNull() ?: return@flatMap emptyList()
            market.numbers.take(MOST).mapIndexedNotNull { index, forGender ->
                val number = forGender[gender]?.let { known[it] } ?: return@mapIndexedNotNull null
                val count = index + 1
                val words = listOf(number.text, if (count == 1) good.text else plural)
                if (listen && words.any { learned.course.clipFor(it) == null }) return@mapIndexedNotNull null
                Want(good, picture, count, words)
            }
        }
    }

    /** The Item *i*, once learned and, heard, said by a Clip. */
    private fun and(learned: Learned, listen: Boolean): Item? {
        val id = learned.course.market?.and ?: return null
        return learned.items.firstOrNull { it.id == id }?.takeIf { !listen || learned.course.clipFor(it.text) != null }
    }

    /** The lists each level can ask: one good, and two goods with different Pictures. */
    private fun levels(learned: Learned, listen: Boolean): List<Pair<String, List<List<Want>>>> {
        val wants = wants(learned, listen)
        if (wants.map { it.good.id }.distinct().size < LEAST) return emptyList()
        val one = ONE to wants.map { listOf(it) }
        and(learned, listen) ?: return listOf(one)
        val pairs = wants.flatMapIndexed { index, first ->
            wants.drop(index + 1).filter { it.good.id != first.good.id && it.picture != first.picture }.map { listOf(first, it) }
        }
        return if (pairs.size >= LEAST) listOf(one, TWO to pairs) else listOf(one)
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (levels(learned, listen = false).isEmpty()) GameState.NEEDS_MORE else GameState.READY

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        val clues = listOf(LISTEN, READ).filter { it == READ || (sound && levels(learned, listen = true).isNotEmpty()) }
        val clue = choice(CLUE, clues.map { OptionView(it) }, wanted.getOrNull(0))
        val levels = levels(learned, listen = clue.selected == LISTEN)
        return listOf(clue, choice(LEVEL, levels.map { OptionView(it.first) }, wanted.getOrNull(1)))
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val listen = selected[0] == LISTEN
        val goods = wants(learned, listen).distinctBy { it.good.id }.map { StallGood(it.good.id, it.picture, learned.localize(it.good.meaning)) }
        val level = levels(learned, listen).first { it.first == selected[1] }
        val and = and(learned, listen)
        val lists = random.draw(level.second, QUESTIONS, seen, avoidFirst) { list -> list.joinToString("+") { it.key } }
        val questions = lists.map { list ->
            val words = list.map { it.words }.reduce { first, second -> first + checkNotNull(and).text + second }
            val wanted = goods.filter { good -> list.any { it.good.id == good.id } }
            MarketQuestion(
                list.joinToString("+") { it.key },
                words,
                list.associate { it.good.id to it.count },
                (wanted + others(goods, wanted, STALL - wanted.size, random)).shuffled(random),
            )
        }
        return MarketRound(selected, listen, Run(questions))
    }

    /** [count] other goods to put beside those [wanted], none with a Picture already there. */
    private fun others(goods: List<StallGood>, wanted: List<StallGood>, count: Int, random: Random): List<StallGood> {
        val chosen = mutableListOf<StallGood>()
        goods.shuffled(random).forEach { good ->
            if (chosen.size < count && (wanted + chosen).none { it.id == good.id || it.picture == good.picture }) chosen += good
        }
        return chosen
    }
}
