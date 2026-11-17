package com.polisheasy.engine

/**
 * A learned Item as a card: its [picture], or with none its meaning, and turned over, the Polish
 * and what it means.
 */
data class PictureCard(val itemId: String, val picture: Picture?, val polish: String, val meaning: String)

/** The cards of one Unit, under its title. */
data class PictureGroup(val title: String, val cards: List<PictureCard>)

/**
 * Picture cards: the learned Items to look at and listen to, grouped by the Unit that taught
 * them. [kinds] chooses between the Items that have a picture and every Item, the ones without a
 * picture showing their meaning instead; [decks] are the sets of cards to choose from. Nothing is
 * asked, so there is no Round and no Best.
 */
data class PictureCardsView(val kinds: ChoiceView, val decks: ChoiceView, val groups: List<PictureGroup>) {
    val cards: List<PictureCard> get() = groups.flatMap { it.cards }
}

internal object PictureCards : GameRules {
    /** How many pictured Items there must be before the Game opens. */
    const val LEAST = 4

    const val KIND = "cards"
    const val PICTURES = "pictures"
    const val WORDS = "words"
    const val DECK = "deck"

    override val game = Game.PICTURE_CARDS

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (learned.pictured.size < LEAST) GameState.NEEDS_MORE else GameState.READY

    /**
     * The cards of the [kind] and [deck] wanted. A kind that is not one gives way to the pictures,
     * and a deck that is not offered to everything.
     */
    fun view(learned: Learned, deck: String?, kind: String?): PictureCardsView {
        val kinds = choice(KIND, listOf(OptionView(PICTURES), OptionView(WORDS)), kind)
        val pictures = learned.course.pictures
        val decks = learned.decks(LEAST) { kinds.selected == WORDS || it.id in pictures }
        val chosen = deck(decks, deck)
        val inDeck = chosen.items.map { it.id }.toSet()
        return PictureCardsView(
            kinds = kinds,
            decks = ChoiceView(DECK, decks.map { it.option }, chosen.id),
            groups = learned.units.mapNotNull { (unit, introduced) ->
                val cards = introduced.filter { it.id in inDeck }.map { item ->
                    PictureCard(item.id, pictures[item.id], item.text, learned.localize(item.meaning))
                }
                if (cards.isEmpty()) null else PictureGroup(learned.localize(unit.title), cards)
            },
        )
    }
}
