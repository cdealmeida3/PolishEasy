package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OppositesSnapTest {
    /** An adjective whose feminine and neuter end in -a and -e, and whose meaning lists its forms, as the course's do. */
    private fun adjective(id: String, meaning: String): Item {
        val stem = id.dropLast(1)
        return item(id, meaning = "$meaning ($id, ${stem}a, ${stem}e)").copy(forms = listOf("${stem}a", "${stem}e"))
    }

    /**
     * Day o1 teaches two pairs of opposites, nowy and stary, dobry and zły. Day o2 a third,
     * młody and stary again, with a word in no pair. Day o3 teaches only half of a fourth pair.
     */
    private val oppositesCourse = Course(
        listOf(
            levelOf(
                "a1",
                DailySet("o1", exercises = listOf("nowy", "stary", "dobry", "zly").mapIndexed { i, id -> choose("o1-e$i", id, id) }),
                DailySet("o2", exercises = listOf("mlody", "fajny").mapIndexed { i, id -> choose("o2-e$i", id, id) }),
                DailySet("o3", exercises = listOf("duzy").mapIndexed { i, id -> choose("o3-e$i", id, id) }),
            ),
        ),
        listOf(
            adjective("nowy", "new"),
            adjective("stary", "old"),
            adjective("dobry", "good"),
            adjective("zly", "bad"),
            adjective("mlody", "young"),
            adjective("fajny", "cool"),
            adjective("duzy", "big"),
            adjective("maly", "small"),
        ),
        opposites = listOf("nowy" to "stary", "dobry" to "zly", "mlody" to "stary", "duzy" to "maly"),
    )

    private fun player() = Player(course = oppositesCourse)

    private fun stateOf(player: Player) = player.practice().view().games.single { it.game == Game.OPPOSITES_SNAP }.state

    private fun Practice.snap(vararg wanted: String): OppositesRound = start(setup(Game.OPPOSITES_SNAP, wanted.toList())) as OppositesRound

    private fun OppositesRound.answerRightly(): OppositesRound = answer(checkNotNull(card).opposites)

    private fun OppositesRound.answerWrongly(): OppositesRound = answer(!checkNotNull(card).opposites)

    @Test
    fun `Opposites snap opens with three learned pairs, each with both its words learned`() {
        assertEquals(GameState.NEEDS_MORE, stateOf(player().completed("o1")))
        assertEquals(GameState.READY, stateOf(player().completed("o1", "o2")))
        // duży without mały is no pair yet.
        assertEquals(GameState.NEEDS_MORE, stateOf(player().completed("o1", "o3")))
    }

    @Test
    fun `it is played for 60 seconds or 120, over the learned pairs`() {
        val practice = player().completed("o1", "o2", "o3").practice()
        assertEquals(listOf("seconds-60", "seconds-120"), practice.setup(Game.OPPOSITES_SNAP).choices.single().options.map { it.id })

        val round = practice.snap("seconds-120")
        assertEquals(120, round.seconds)
        assertEquals(240, round.cards.size)
        assertEquals(setOf("nowy|stary", "dobry|zly", "mlody|stary"), round.cards.map { it.key }.toSet())
    }

    @Test
    fun `the two words are in the same gender, and a pair that is not opposites is never opposites by another pair`() {
        val forms = oppositesCourse.items.associate { it.spellings.last() to 2 } +
            oppositesCourse.items.associate { it.spellings[1] to 1 } + oppositesCourse.items.associate { it.text to 0 }
        val opposites = setOf("nowy" to "stary", "dobry" to "zly", "mlody" to "stary")
        repeat(10) { seed ->
            val round = Player(oppositesCourse, player().completed("o1", "o2").progress, seed = seed).practice().snap()
            round.cards.forEach { card ->
                assertEquals(forms.getValue(card.word), forms.getValue(card.shown), "$card")
                assertEquals(forms.getValue(card.word), forms.getValue(card.opposite), "$card")
                val masculine = listOf(card.word, card.shown).map { word -> oppositesCourse.items.single { word in it.spellings }.id }
                val really = (masculine[0] to masculine[1]) in opposites || (masculine[1] to masculine[0]) in opposites
                assertEquals(really, card.opposites, "$card")
                // fajny is in no pair, so it is never shown.
                assertFalse(masculine.contains("fajny"))
            }
            assertTrue(round.cards.any { it.opposites } && round.cards.any { !it.opposites })
        }
    }

    @Test
    fun `the meanings leave out the forms the course lists after them`() {
        val card = checkNotNull(player().completed("o1", "o2").practice().snap().card)
        val meanings = setOf("new", "old", "good", "bad", "young")
        assertTrue(card.meaning in meanings && card.shownMeaning in meanings && card.oppositeMeaning in meanings, "$card")
    }

    @Test
    fun `every pair comes round before any comes again, and none twice running`() {
        val pairs = player().completed("o1", "o2").practice().snap().cards.map { it.key }
        assertEquals(3, pairs.take(3).toSet().size)
        assertEquals(3, pairs.subList(3, 6).toSet().size)
        assertTrue(pairs.zipWithNext().none { (one, next) -> one == next })
    }

    @Test
    fun `a right answer brings the next pair at once, and a wrong one stays with the real opposite while the clock runs`() {
        val start = player().completed("o1", "o2").practice().snap()
        val right = start.answerRightly()
        assertEquals(1, right.index)
        assertEquals(1, right.right)
        assertFalse(right.missed)

        val missed = start.answerWrongly()
        assertEquals(0, missed.index)
        assertTrue(missed.missed)
        assertEquals(missed, missed.answerRightly())
        assertEquals(59, missed.tick().left)
        val moved = missed.next()
        assertEquals(1, moved.index)
        assertEquals(0, moved.right)
        assertFalse(moved.missed)
    }

    @Test
    fun `the Round ends when the time is up, and the Best is the most right answers in each time`() {
        val player = player().completed("o1", "o2")
        val practice = player.practice()
        var round = practice.snap("seconds-60")
        repeat(5) { round = round.answerRightly() }
        round = round.answerWrongly().next()
        repeat(60) { round = round.tick() }

        assertTrue(round.finished)
        assertEquals(null, round.card)
        assertEquals(Score(5), round.score)
        assertEquals(round, round.tick())
        assertEquals(5, practice.finish(round).best)
        assertEquals(5, player.practice().setup(Game.OPPOSITES_SNAP, listOf("seconds-60")).best)
        assertEquals(null, player.practice().setup(Game.OPPOSITES_SNAP, listOf("seconds-120")).best)
    }

    @Test
    fun `the loader reads the pairs and rejects one that is not two defined Items, or given twice`() {
        fun read(practice: String): Pair<List<String>, List<Pair<String, String>>> {
            val files = mapOf(
                "course.json" to """{"levels": [], "items": ["items.json"], "practice": "practice.json"}""",
                "items.json" to """{"items": [{"id": "nowy", "text": "nowy", "meaning": {"en": "new"}}, {"id": "stary", "text": "stary", "meaning": {"en": "old"}}]}""",
                "practice.json" to practice,
            )
            val content = readContent { files.getValue(it) }
            return loadingProblems(content).map { it.message } to content.course().opposites
        }
        assertEquals(emptyList<String>() to listOf("nowy" to "stary"), read("""{"opposites": [["nowy", "stary"]]}"""))
        assertEquals(
            listOf(
                "A pair of opposites must be two different Items, and [nowy] is not",
                "A pair of opposites names Item mlody, which is not defined",
                "The opposites nowy and stary are given more than once",
            ),
            read("""{"opposites": [["nowy"], ["mlody", "stary"], ["nowy", "stary"], ["stary", "nowy"]]}""").first,
        )
    }
}
