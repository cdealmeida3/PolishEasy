package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MojMojaMojeTest {
    private val genders = mapOf(
        "tata" to Gender.MASCULINE,
        "brat" to Gender.MASCULINE,
        "mama" to Gender.FEMININE,
        "siostra" to Gender.FEMININE,
        "dziecko" to Gender.NEUTER,
        "pytanie" to Gender.NEUTER,
    )
    private val words = listOf("mój", "moja", "moje") + genders.keys + "tak"

    /** m1 teaches mój, moja and four nouns of theirs, m2 a neuter noun and a word that is no noun, m3 moje and another neuter noun. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("m1", *listOf("mój", "moja", "tata", "brat", "mama", "siostra").map { choose("e-$it", it, it) }.toTypedArray()),
                day("m2", choose("e-dziecko", "dziecko", "dziecko"), choose("e-tak", "tak", "tak")),
                day("m3", choose("e-moje", "moje", "moje"), choose("e-pytanie", "pytanie", "pytanie")),
            ),
        ),
        words.map { item(it) },
        clips = (words - "brat").map { clip(it) },
        pictures = mapOf("mama" to Picture.Image("pictures/mama.webp")),
        genders = genders,
        possessives = mapOf(Gender.MASCULINE to "mój", Gender.FEMININE to "moja", Gender.NEUTER to "moje"),
    )

    private fun player(course: Course = this.course) = Player(course = course)

    private fun Practice.my(vararg wanted: String, sound: Boolean = true): GenderRound =
        start(setup(Game.MOJ_MOJA_MOJE, wanted.toList(), sound), sound) as GenderRound

    private fun stateOf(player: Player) = player.practice().view().games.single { it.game == Game.MOJ_MOJA_MOJE }.state

    private fun GenderRound.right(): Int = buckets.indexOfFirst { it.gender == checkNotNull(question).gender }

    private fun GenderRound.playOut(wrong: Int = 0): GenderRound {
        var round = this
        var missed = 0
        while (!round.finished) round = round.choose(if (missed++ < wrong) (round.right() + 1) % round.buckets.size else round.right()).next()
        return round
    }

    @Test
    fun `the Game opens with two of the words for my and four nouns that go with them`() {
        assertEquals(GameState.READY, stateOf(player().completed("m1")))
        // One word for "my" is no choice.
        val oneWord = course.copy(possessives = mapOf(Gender.MASCULINE to "mój"))
        assertEquals(GameState.NEEDS_MORE, stateOf(player(oneWord).completed("m1")))
        // Three nouns are too few.
        val threeNouns = course.copy(genders = genders - "tata")
        assertEquals(GameState.NEEDS_MORE, stateOf(player(threeNouns).completed("m1")))
        // A course that gives no genders never opens it.
        assertEquals(GameState.NEEDS_MORE, stateOf(Player().completed("d1", "d2", "d3")))
    }

    @Test
    fun `a noun is asked only once its own word for my has been learned`() {
        val early = player().completed("m1", "m2").practice().my()
        // "dziecko" is learned and "moje" is not: there is nowhere to put it.
        assertEquals(listOf(GenderBucket(Gender.MASCULINE, "mój"), GenderBucket(Gender.FEMININE, "moja")), early.buckets)
        assertEquals(setOf("tata", "brat", "mama", "siostra"), early.asked.toSet())

        val later = player().completed("m1", "m2", "m3").practice().my()
        assertEquals(listOf("mój", "moja", "moje"), later.buckets.map { it.word })
        assertEquals(genders.keys, later.asked.toSet())
    }

    @Test
    fun `a question gives the noun, its meaning, its picture and the two words together`() {
        val round = player().completed("m1", "m2", "m3").practice().my()

        round.run.questions.forEach { question ->
            assertEquals(question.itemId, question.polish)
            assertEquals("${question.polish} in English", question.meaning)
            assertEquals(genders.getValue(question.itemId), question.gender)
            val my = mapOf(Gender.MASCULINE to "mój", Gender.FEMININE to "moja", Gender.NEUTER to "moje").getValue(question.gender)
            assertEquals("$my ${question.polish}", question.phrase)
        }
        assertEquals(Picture.Image("pictures/mama.webp"), round.run.questions.single { it.itemId == "mama" }.picture)
        assertEquals(null, round.run.questions.single { it.itemId == "tata" }.picture)
    }

    @Test
    fun `the noun is written, heard or given by its meaning`() {
        val practice = player().completed("m1", "m2", "m3").practice()

        assertEquals(listOf("word", "listen", "meaning"), practice.setup(Game.MOJ_MOJA_MOJE).choices.single().options.map { it.id })
        assertEquals(listOf("word", "meaning"), practice.setup(Game.MOJ_MOJA_MOJE, sound = false).choices.single().options.map { it.id })
        assertEquals(NounClue.WORD, practice.my("word").clue)
        assertEquals(NounClue.MEANING, practice.my("meaning").clue)
        val heard = practice.my("listen")
        assertEquals(NounClue.LISTEN, heard.clue)
        // "brat" has no Clip.
        assertEquals(genders.keys - "brat", heard.asked.toSet())
    }

    @Test
    fun `the word tapped answers the question, right or wrong, once`() {
        val start = player().completed("m1").practice().my()
        val right = start.right()

        assertEquals(Given(start.buckets[right].word, true), start.choose(right).given)
        val wrong = start.choose(1 - right)
        assertEquals(Given(start.buckets[1 - right].word, false), wrong.given)
        assertEquals(wrong, wrong.choose(right))
        assertEquals(start, start.choose(5))
        assertEquals(start, start.next())
        assertEquals(1, wrong.next().step)
    }

    @Test
    fun `each word counts the nouns answered so far that belong under it`() {
        val round = player().completed("m1").practice().my()
        val first = checkNotNull(round.question).gender

        assertEquals(0, round.sorted(first))
        // Answered wrongly, the noun is still shown where it belongs.
        assertEquals(1, round.choose((round.right() + 1) % 2).sorted(first))
        val done = round.playOut()
        assertEquals(listOf(2, 2), done.buckets.map { done.sorted(it.gender) })
    }

    @Test
    fun `the result is the answers right first try, and the most is the Best`() {
        val player = player().completed("m1")
        val practice = player.practice()

        val first = practice.finish(practice.my().playOut(wrong = 2))
        assertEquals(Score(2, of = 4), first.score)
        assertTrue(first.first)
        assertTrue(practice.finish(practice.my().playOut()).newBest)
        assertEquals(4, player.practice().setup(Game.MOJ_MOJA_MOJE, listOf("word")).best)
        assertEquals(null, player.practice().setup(Game.MOJ_MOJA_MOJE, listOf("meaning")).best)
    }
}
