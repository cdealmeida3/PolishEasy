package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TenTaToTest {
    private val genders = mapOf(
        "zeszyt" to Gender.MASCULINE,
        "klucz" to Gender.MASCULINE,
        "torba" to Gender.FEMININE,
        "mapa" to Gender.FEMININE,
        "okno" to Gender.NEUTER,
    )
    private val words = listOf("mój", "ten", "ta", "to") + genders.keys

    /** t1 teaches ten, ta and four nouns of theirs, t2 to and okno; mój is learned, but moja and moje never are. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("t1", *listOf("mój", "ten", "ta", "zeszyt", "klucz", "torba", "mapa").map { choose("e-$it", it, it) }.toTypedArray()),
                day("t2", choose("e-to", "to", "to"), choose("e-okno", "okno", "okno")),
            ),
        ),
        words.map { item(it) },
        clips = words.map { clip(it) } + clip("ta torba"),
        genders = genders,
        possessives = mapOf(Gender.MASCULINE to "mój", Gender.FEMININE to "moja", Gender.NEUTER to "moje"),
        demonstratives = mapOf(Gender.MASCULINE to "ten", Gender.FEMININE to "ta", Gender.NEUTER to "to"),
    )

    private fun player() = Player(course = course)

    private fun Practice.tenTaTo(vararg wanted: String): GenderRound = start(setup(Game.TEN_TA_TO, wanted.toList())) as GenderRound

    private fun stateOf(player: Player, game: Game) = player.practice().view().games.single { it.game == game }.state

    @Test
    fun `Ten, ta, to opens with two of its words learned, while Mój, moja, moje still waits for a second word for my`() {
        assertEquals(GameState.NEEDS_MORE, stateOf(Player().completed("d1"), Game.TEN_TA_TO))
        val player = player().completed("t1")
        assertEquals(GameState.READY, stateOf(player, Game.TEN_TA_TO))
        assertEquals(GameState.NEEDS_MORE, stateOf(player, Game.MOJ_MOJA_MOJE))
    }

    @Test
    fun `a noun is sorted under ten, ta or to, and answered the two are written together`() {
        val early = player().completed("t1").practice().tenTaTo()
        assertEquals(Game.TEN_TA_TO, early.game)
        assertEquals(listOf("ten", "ta"), early.buckets.map { it.word })
        assertEquals(setOf("zeszyt", "klucz", "torba", "mapa"), early.asked.toSet())

        val round = player().completed("t1", "t2").practice().tenTaTo()
        assertEquals(listOf("ten", "ta", "to"), round.buckets.map { it.word })
        assertEquals(genders.keys, round.asked.toSet())
        round.run.questions.forEach { question ->
            val word = round.buckets.single { it.gender == question.gender }.word
            assertEquals("$word ${question.polish}", question.phrase)
        }
    }

    @Test
    fun `the result is the nouns right first try, kept as the Best of Ten, ta, to alone`() {
        val practice = player().completed("t1", "t2").practice()
        var round = practice.tenTaTo()
        val first = checkNotNull(round.question)
        val wrong = round.buckets.indexOfFirst { it.gender != first.gender }
        round = round.choose(wrong).next()
        while (!round.finished) {
            val question = checkNotNull(round.question)
            round = round.choose(round.buckets.indexOfFirst { it.gender == question.gender }).next()
        }
        assertEquals(Score(4, of = 5), round.score)
        val result = practice.finish(round)
        assertEquals(Game.TEN_TA_TO, result.game)
        assertEquals(4, result.best)
        assertTrue(practice.view().games.single { it.game == Game.MOJ_MOJA_MOJE }.best == null)
    }

    @Test
    fun `the loader reads the words for this and rejects one that is not defined or not a gender`() {
        fun problems(practice: String): List<String> {
            val files = mapOf(
                "course.json" to """{"levels": [], "items": ["items.json"], "practice": "practice.json"}""",
                "items.json" to """{"items": [{"id": "ten", "text": "ten", "meaning": {"en": "this"}}]}""",
                "practice.json" to practice,
            )
            return loadingProblems(readContent { files.getValue(it) }).map { it.message }
        }
        assertEquals(emptyList(), problems("""{"this": {"m": "ten"}}"""))
        assertEquals(
            listOf(
                "\"x\" is not a gender to give a word for \"this\"; it must be one of \"m\", \"f\", \"n\"",
                "The word for \"this\" is given as Item tamten, which is not defined"),
            problems("""{"this": {"x": "ten", "f": "tamten"}}"""),
        )
    }
}
