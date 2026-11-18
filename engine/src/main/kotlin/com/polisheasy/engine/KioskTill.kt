package com.polisheasy.engine

import kotlin.random.Random

/**
 * What Kiosk till needs from the content, as Item ids: the money, [zloty] and [grosz], each with
 * the forms that go after 2 and after 5 (*złote*, *złotych*); [and], which joins the złoty to the
 * grosze; [costs], *kosztuje*; and the [goods] on the counter, each with a Picture.
 */
data class Till(
    val zloty: String,
    val grosz: String,
    val and: String,
    val costs: String,
    val goods: List<String>,
)

/**
 * One question of Kiosk till: a good on the counter, its [picture], and its [price] in grosze.
 * [words] say it: *gazeta kosztuje trzy złote i czterdzieści groszy*. [numerals] is the price as
 * a till shows it, "3,40 zł". [key] is the price.
 */
data class PriceQuestion(
    val key: String,
    val picture: Picture,
    val words: List<String>,
    val price: Int,
    val numerals: String,
) {
    /** The words as a sentence: a capital first and a full stop. */
    val polish: String get() = words.joinToString(" ").replaceFirstChar { it.uppercase() } + "."
}

/**
 * A Round of Kiosk till, chapter 7's Game: a good's price is said, or with [listen] off written in
 * words, and the Learner pays it exactly from [money], the coins and notes in grosze, smallest
 * first. [paid] is what is on the counter, in the order it was put there. The result is the prices
 * paid right first try.
 */
data class TillRound(
    override val played: List<String>,
    val listen: Boolean,
    val money: List<Int>,
    val run: Run<PriceQuestion>,
    val paid: List<Int> = emptyList(),
) : Round {
    override val game: Game get() = Game.KIOSK_TILL
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.key }

    /** The question being shown, or null once the Round is finished. */
    val question: PriceQuestion? get() = run.current

    /** The answer to the question being shown, until the Learner moves on. Its text is what was paid, as a till shows it. */
    val given: Given? get() = run.given

    /** What is on the counter, in grosze. */
    val onCounter: Int get() = paid.sum()

    /** What is on the counter as a till shows it, "3,40 zł", or nothing while it is empty. */
    val counter: String get() = if (paid.isEmpty()) "" else KioskTill.numerals(onCounter)

    /** Whether there is money on the counter to pay with. */
    val canPay: Boolean get() = given == null && paid.isNotEmpty() && question != null

    /** Puts [coin] on the counter. No change if it is not money of this Round, the counter is full, or the question is answered. */
    fun add(coin: Int): TillRound =
        if (coin !in money || given != null || question == null || paid.size >= KioskTill.MOST_ON_COUNTER) this else copy(paid = paid + coin)

    /** Takes back what is on the counter at [index]. No change once the question is answered. */
    fun takeBack(index: Int): TillRound =
        if (given != null || index !in paid.indices) this else copy(paid = paid.filterIndexed { i, _ -> i != index })

    /** Pays with what is on the counter: right if it is the price exactly. */
    fun pay(): TillRound {
        val question = question ?: return this
        if (!canPay) return this
        return copy(run = run.answer(KioskTill.numerals(onCounter), onCounter == question.price))
    }

    /** Moves on from an answered question, with the counter empty. */
    fun next(): TillRound = if (given == null) this else copy(run = run.next(), paid = emptyList())
}

/**
 * Kiosk till: once the money, *kosztuje* and a good of the content's `till` are learned, prices
 * made of learned numbers. Small prices, below ten złoty, are paid with coins; big ones, from ten
 * to a hundred, with notes too. A price is whole złoty or has tens of grosze, so the coins pay it.
 */
internal object KioskTill : GameRules {
    const val CLUE = "price"
    const val LISTEN = "listen"
    const val READ = "read"
    const val LEVEL = "level"
    const val COINS = "coins"
    const val NOTES = "notes"

    /** The fewest prices a level is offered with, and the most a Round asks. */
    const val LEAST = 4
    const val QUESTIONS = 6

    /** The most coins and notes the counter holds. */
    const val MOST_ON_COUNTER = 20

    /** The coins, then the notes, in grosze. */
    val COIN_VALUES = listOf(10, 20, 50, 100, 200, 500)
    val NOTE_VALUES = listOf(1000, 2000, 5000)

    override val game = Game.KIOSK_TILL

    /** A price in grosze as a till shows it: "3,40 zł". */
    fun numerals(grosze: Int): String = "%d,%02d zł".format(grosze / 100, grosze % 100)

    private class Shop(val zloty: Item, val grosz: Item, val and: Item, val costs: Item, val goods: List<Pair<Item, Picture>>)

    /** The money, the words and the pictured goods, once all are learned; null before. */
    private fun shop(learned: Learned): Shop? {
        val till = learned.course.till ?: return null
        val known = learned.items.associateBy { it.id }
        val goods = till.goods.mapNotNull { id -> known[id]?.let { item -> learned.course.pictures[id]?.let { item to it } } }
        if (goods.isEmpty()) return null
        return Shop(
            known[till.zloty]?.takeIf { it.forms.size >= 2 } ?: return null,
            known[till.grosz]?.takeIf { it.forms.size >= 2 } ?: return null,
            known[till.and] ?: return null,
            known[till.costs] ?: return null,
            goods,
        )
    }

    /** The goods that can be asked about: heard, only those whose name has a Clip. */
    private fun goods(learned: Learned, shop: Shop, listen: Boolean): List<Pair<Item, Picture>> =
        shop.goods.filter { (good, _) -> !listen || learned.course.clipFor(good.text) != null }

    /** The prices each level can ask, said in learned words; heard, only those whose every word has a Clip. */
    private fun levels(learned: Learned, listen: Boolean): List<Pair<String, List<Pair<Int, List<String>>>>> {
        val shop = shop(learned) ?: return emptyList()
        if (goods(learned, shop, listen).isEmpty()) return emptyList()
        val numbers = learned.numbers()
        fun heard(words: List<String>) = !listen || words.all { learned.course.clipFor(it) != null }
        val tens = (10..90 step 10).filter { wordsOf(it, numbers) != null }
        fun prices(zloty: IntRange) = zloty.flatMap { whole ->
            val said = wordsOf(whole, numbers) ?: return@flatMap emptyList()
            (listOf(0) + tens).map { grosze ->
                val words = said + checkNotNull(shop.zloty.countedBy(whole)) +
                    if (grosze == 0) emptyList() else listOf(shop.and.text) + checkNotNull(wordsOf(grosze, numbers)) + checkNotNull(shop.grosz.countedBy(grosze))
                whole * 100 + grosze to words
            }
        }.filter { (_, words) -> heard(words + shop.costs.text) }
        return listOf(COINS to prices(1..9), NOTES to prices(10..99)).filter { it.second.size >= LEAST }
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
        val shop = checkNotNull(shop(learned))
        val level = levels(learned, listen).first { it.first == selected[1] }
        val goods = goods(learned, shop, listen)
        val questions = random.draw(level.second, QUESTIONS, seen, avoidFirst) { "${it.first}" }.map { (price, words) ->
            val (good, picture) = goods.random(random)
            PriceQuestion("$price", picture, listOf(good.text, shop.costs.text) + words, price, numerals(price))
        }
        val money = if (level.first == COINS) COIN_VALUES else COIN_VALUES + NOTE_VALUES
        return TillRound(selected, listen, money, Run(questions))
    }
}
