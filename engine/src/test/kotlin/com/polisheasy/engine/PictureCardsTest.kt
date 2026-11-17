package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PictureCardsTest {
    private fun player() = Player(course = wordsCourse)

    private fun PictureCardsView.words() = groups.map { group -> group.title to group.cards.map { it.polish } }

    private fun stateOf(player: Player) = player.practice().view().games.single { it.game == Game.PICTURE_CARDS }.state

    @Test
    fun `Picture cards opens once four learned Items have a picture`() {
        // Three pictures are not enough: a course whose first day has only three.
        val few = wordsCourse.copy(pictures = wordPictures - "tak")
        assertEquals(GameState.NEEDS_MORE, stateOf(Player(course = few).completed("w1")))
        assertEquals(GameState.READY, stateOf(player().completed("w1")))
    }

    @Test
    fun `the cards are the learned Items that have a picture, under the Unit that taught them`() {
        val cards = player().completed("w1", "w2", "w3").practice().pictureCards()

        assertEquals(
            listOf("Animals" to listOf("kot", "pies", "dom", "tak", "ryba", "ptak"), "Family" to listOf("jeden", "dwa")),
            cards.words(),
        )
        val kot = cards.cards.first()
        assertEquals(PictureCard("kot", Picture.Image("pictures/kot.webp"), "kot", "kot in English"), kot)
        assertEquals(Picture.Glyph("1"), cards.cards.single { it.itemId == "jeden" }.picture)
    }

    @Test
    fun `with one Unit and a few days there is one deck, everything`() {
        val cards = player().completed("w1", "w2").practice().pictureCards()

        assertEquals(listOf(Decks.ALL), cards.decks.options.map { it.id })
        assertEquals(Decks.ALL, cards.decks.selected)
    }

    @Test
    fun `further along there is a deck for each Unit and one of the latest words`() {
        val practice = player().completed("w1", "w2", "w3", "w4", "w5", "w6").practice()
        val cards = practice.pictureCards()

        assertEquals(listOf(Decks.ALL, "u1", "u2", Decks.LATEST), cards.decks.options.map { it.id })
        // A Unit's deck is named by the content; the others by the app.
        assertEquals(listOf(null, "Animals", "Family", null), cards.decks.options.map { it.label })

        assertEquals(listOf("Family" to listOf("jeden", "dwa", "mama", "tata", "brat", "siostra")), practice.pictureCards("u2").words())
        // The latest words are those of the last three Daily Sets.
        assertEquals(listOf("Family" to listOf("mama", "tata", "brat", "siostra")), practice.pictureCards(Decks.LATEST).words())
        assertEquals("u2", practice.pictureCards("u2").decks.selected)
    }

    @Test
    fun `the tricky words are the pictured Items low in the Review schedule`() {
        val player = player()
        val today = march(2)
        fun inBox(box: Int) = ItemProgress(ItemSchedule(box, due = today.plusDays(1), lastAnswered = today))
        player.progress.recordCompleted(
            "w1",
            today,
            mapOf("kot" to inBox(1), "pies" to inBox(2), "dom" to inBox(3), "tak" to inBox(5), "bardzo" to inBox(1)),
        )
        player.progress.recordCompleted("w2", today, mapOf("ryba" to inBox(1), "ptak" to inBox(2)))

        val tricky = player.practice().pictureCards(Decks.TRICKY)

        assertEquals(listOf("Animals" to listOf("kot", "pies", "ryba", "ptak")), tricky.words())
        assertEquals(Decks.TRICKY, tricky.decks.selected)
    }

    @Test
    fun `a deck that is not offered gives way to everything`() {
        val cards = player().completed("w1", "w2").practice().pictureCards("u2")

        assertEquals(Decks.ALL, cards.decks.selected)
        assertEquals(6, cards.cards.size)
    }

    @Test
    fun `as words, every learned Item is a card, and one without a picture shows its meaning`() {
        val practice = player().completed("w1", "w2").practice()
        val words = practice.pictureCards(kind = PictureCards.WORDS)

        assertEquals(listOf("Animals" to listOf("kot", "pies", "dom", "tak", "bardzo", "ryba", "ptak")), words.words())
        assertNull(words.cards.single { it.itemId == "bardzo" }.picture)
        assertEquals(Picture.Image("pictures/kot.webp"), words.cards.first().picture)
        assertEquals(PictureCards.WORDS, words.kinds.selected)
        assertEquals(listOf(PictureCards.PICTURES, PictureCards.WORDS), words.kinds.options.map { it.id })
        // Without a word about it, the cards are the pictures.
        assertEquals(PictureCards.PICTURES, practice.pictureCards().kinds.selected)
    }

    @Test
    fun `the decks are of the kind of card chosen`() {
        val practice = player().completed("w1", "w2", "w3", "w4", "w5", "w6").practice()

        val latest = practice.pictureCards(Decks.LATEST, PictureCards.WORDS)

        assertEquals(listOf("Family" to listOf("mama", "tata", "brat", "siostra", "bo")), latest.words())
    }

    @Test
    fun `Picture cards has no Rounds, no Best and nothing to set up`() {
        val practice = player().completed("w1").practice()

        assertNull(practice.view().games.single { it.game == Game.PICTURE_CARDS }.best)
        assertFailsWith<IllegalStateException> { practice.setup(Game.PICTURE_CARDS) }
    }

    @Test
    fun `the cards cannot be opened while Practice is locked`() {
        val player = player().completed("w1")
        player.clock.moveTo(march(3))

        assertFailsWith<IllegalStateException> { player.practice().pictureCards() }
    }
}
