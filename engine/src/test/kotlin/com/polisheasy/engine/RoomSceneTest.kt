package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomSceneTest {
    private val places = listOf(
        item("lozko", "łóżko", "a bed"), item("szafa", "szafa", "a wardrobe"), item("biurko", "biurko", "a desk"),
        item("polka", "półka", "a shelf"), item("sciana", "ściana", "a wall"), item("podloga", "podłoga", "a floor"), item("sufit", "sufit", "a ceiling"),
    ).map { it.copy(forms = listOf("w ${it.text}", "na ${it.text}")) }
    private val verbs = listOf(item("stoi", "stoi", "it stands"), item("lezy", "leży", "it lies"), item("wisi", "wisi", "it hangs"))
    private val plurals = listOf(item("stoja", "stoją", "they stand"), item("leza", "leżą", "they lie"), item("wisza", "wiszą", "they hang"))
    private val things = listOf(item("lampa", "lampa", "a lamp"), item("obraz", "obraz", "a picture"), item("kot", "kot", "a cat"), item("zdjecia", "zdjęcia", "photos"))

    private fun scene(thing: String, verb: String, place: String): RoomSceneLine {
        val text = "${things.single { it.id == thing }.text.replaceFirstChar { it.uppercase() }} ${(verbs + plurals).single { it.id == verb }.text} na ${places.single { it.id == place }.text}."
        return RoomSceneLine(text, mapOf("en" to text), thing, place, verb, listOf(thing, verb, place))
    }

    private val scenes = listOf(
        scene("lampa", "stoi", "biurko"), scene("obraz", "wisi", "sciana"), scene("kot", "lezy", "lozko"),
        scene("lampa", "wisi", "sufit"), scene("kot", "lezy", "podloga"), scene("zdjecia", "wisza", "sciana"),
    )

    /** l1 teaches the places, l2 the things and the singular verbs, l3 the plural verbs. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("l1", *places.map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
                day("l2", *(things + verbs).map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
                day("l3", *plurals.map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
            ),
        ),
        places + verbs + plurals + things,
        room = Room(places.map { it.id }, listOf(verbs.map { it.id }, plurals.map { it.id }), scenes),
    )

    private fun player(seed: Int = 0) = Player(course = course, seed = seed)

    private fun state(player: Player, game: Game) = player.practice().view().games.single { it.game == game }.state

    @Test
    fun `Room scene opens once sentences to three places and six places are learned, and is read or heard`() {
        assertEquals(GameState.NEEDS_MORE, state(player().completed("l1"), Game.ROOM_SCENE))
        assertEquals(GameState.READY, state(player().completed("l1", "l2"), Game.ROOM_SCENE))
        val setup = player().completed("l1", "l2").practice().setup(Game.ROOM_SCENE, emptyList())
        // Without Clips for the sentences only reading is offered.
        assertEquals(listOf(RoomScene.READ), setup.choices.single().options.map { it.id })
    }

    @Test
    fun `a Round asks each sentence to a place of its own, on a board of six learned places`() {
        repeat(10) { seed ->
            val round = player(seed).completed("l1", "l2", "l3").practice().start(player(seed).completed("l1", "l2", "l3").practice().setup(Game.ROOM_SCENE, listOf(RoomScene.READ))) as RoomSceneRound
            assertEquals(RoomScene.BOARD, round.board.size)
            assertEquals(round.board.size, round.board.distinctBy { it.item }.size)
            assertTrue(round.total in RoomScene.FEWEST..RoomScene.QUESTIONS)
            assertEquals(round.total, round.run.questions.map { it.place }.distinct().size)
            round.run.questions.forEach { question ->
                val scene = scenes.single { it.text == question.text }
                assertEquals(scene.place, round.board[question.place].item)
                // The thing as the sentence says it: zdjęcia, not zdjęcie.
                assertEquals(scene.text.substringBefore(' ').lowercase(), question.thing)
            }
        }
    }

    @Test
    fun `a thing goes on its place once answered, and the score counts the sentences right first try`() {
        val practice = player().completed("l1", "l2").practice()
        var round = practice.start(practice.setup(Game.ROOM_SCENE, listOf(RoomScene.READ))) as RoomSceneRound
        val first = round.question!!
        assertEquals(emptyList(), round.things(first.place))
        round = round.choose(first.place)
        assertTrue(round.given!!.correct)
        assertEquals(listOf(first), round.things(first.place))
        // Answered once: a second tap changes nothing.
        assertEquals(round, round.choose((first.place + 1) % round.board.size))
        round = round.next()
        val second = round.question!!
        round = round.choose((second.place + 1) % round.board.size)
        assertEquals(false, round.given!!.correct)
        assertEquals((second.place + 1) % round.board.size, round.chosen)
        // The thing goes where the sentence puts it, not where it was tapped.
        assertEquals(listOf(second), round.things(second.place))
        round = round.next()
        while (!round.finished) round = round.choose(round.question!!.place).next()
        assertEquals(Score(round.total - 1, of = round.total), round.score)
        assertNull(round.question)
    }

    @Test
    fun `Stoi, leży czy wisi? opens once four sentences and their row of verbs are learned, and gaps the verb`() {
        assertEquals(GameState.NEEDS_MORE, state(player().completed("l1"), Game.STOI_LEZY_WISI))
        assertEquals(GameState.READY, state(player().completed("l1", "l2"), Game.STOI_LEZY_WISI))
        val practice = player().completed("l1", "l2").practice()
        val round = practice.start(practice.setup(Game.STOI_LEZY_WISI, emptyList())) as PositionsRound
        // The plural sentence waits for its row: zdjęcia wiszą.
        assertEquals(5, round.total)
        round.run.questions.forEach { question ->
            assertEquals(listOf("stoi", "leży", "wisi"), question.offered)
            assertEquals(question.text, question.gapped.replace(StoiLezyWisi.GAP, question.offered[question.right]))
            assertTrue(StoiLezyWisi.GAP in question.gapped)
        }
        val all = player().completed("l1", "l2", "l3").practice()
        val plural = (all.start(all.setup(Game.STOI_LEZY_WISI, emptyList())) as PositionsRound).run.questions.single { it.text.startsWith("Zdjęcia") }
        assertEquals(listOf("stoją", "leżą", "wiszą"), plural.offered)
        assertEquals("Zdjęcia ___ na ściana.", plural.gapped)
    }

    @Test
    fun `a verb is tapped once, and the score counts the sentences right first try`() {
        val practice = player().completed("l1", "l2").practice()
        var round = practice.start(practice.setup(Game.STOI_LEZY_WISI, emptyList())) as PositionsRound
        val first = round.question!!
        round = round.choose((first.right + 1) % first.offered.size)
        assertEquals(false, round.given!!.correct)
        assertEquals(round, round.choose(first.right))
        round = round.next()
        while (!round.finished) round = round.choose(round.question!!.right).next()
        assertEquals(Score(4, of = 5), round.score)
    }

    @Test
    fun `the gap takes the verb as a word, not inside another`() {
        assertEquals("Lampa ___ na stole.", StoiLezyWisi.gap("Lampa stoi na stole.", "stoi"))
        assertEquals("Stoisko ___ tu.", StoiLezyWisi.gap("Stoisko stoi tu.", "stoi"))
    }
}
