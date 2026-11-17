package com.polisheasy.engine

/**
 * A set of learned Items a Game can be played with. [label] is set when the name comes from the
 * content, as a Unit's title does; otherwise the app names the deck by its [id].
 */
internal class Deck(val id: String, val label: String?, val items: List<Item>) {
    val option: OptionView get() = OptionView(id, label)
}

internal object Decks {
    /** Every learned Item the Game can use. */
    const val ALL = "all"

    /** The Items the last [LATEST_DAILY_SETS] completed Daily Sets introduced. */
    const val LATEST = "latest"

    /** The Items low in the Review schedule: the ones the Learner got wrong lately or has only just met. */
    const val TRICKY = "tricky"

    const val LATEST_DAILY_SETS = 3

    /** An Item in this box or a lower one is tricky. */
    const val TRICKY_BOX = 2
}

/**
 * The decks a Game offers, out of the learned Items it can [use]: everything, then each Unit, the
 * latest words and the tricky ones. A deck is offered only if it has at least [least] Items and is
 * not everything under another name; with fewer than [least] Items in all, none is.
 */
internal fun Learned.decks(least: Int, use: (Item) -> Boolean): List<Deck> {
    val usable = items.filter(use)
    if (usable.size < least) return emptyList()
    val all = Deck(Decks.ALL, null, usable)
    val units = units.map { (unit, introduced) -> Deck(unit.id, localize(unit.title), introduced.filter(use)) }
    val latest = Deck(Decks.LATEST, null, introducedBy(dailySets.takeLast(Decks.LATEST_DAILY_SETS)).filter(use))
    val tricky = Deck(Decks.TRICKY, null, usable.filter { (itemProgress[it.id]?.schedule?.box ?: 1) <= Decks.TRICKY_BOX })
    return listOf(all) + (units + latest + tricky).filter { it.items.size >= least && it.items.size < usable.size }
}

/** The deck with the id [wanted] among [decks], or else the first, which is everything. */
internal fun deck(decks: List<Deck>, wanted: String?): Deck = decks.firstOrNull { it.id == wanted } ?: decks.first()
