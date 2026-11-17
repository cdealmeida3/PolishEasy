package com.polisheasy.engine

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PracticeBestsTest {
    private fun Practice.play(verb: String, wrong: Int, level: String = "choose"): RoundResult =
        finish(verbRound(verb, level).playOut(wrong))

    @Test
    fun `the first result is the Best, and beating it is a new Best`() {
        val practice = Player().completed("d1").practice()

        val first = practice.play("być", wrong = 4)
        assertEquals(RoundResult(Game.VERB_CHALLENGE, listOf("być", "choose"), Score(12, of = 16), best = 12, first = true), first)

        val worse = practice.play("być", wrong = 6)
        assertEquals(12, worse.best)
        assertFalse(worse.newBest)

        val better = practice.play("być", wrong = 1)
        assertEquals(15, better.best)
        assertTrue(better.newBest)
        assertEquals(15, practice.setup(Game.VERB_CHALLENGE, listOf("być", "choose")).best)
    }

    @Test
    fun `each way of playing has its own Best`() {
        val practice = Player().completed("d1", "d2").practice()
        practice.play("być", wrong = 2)
        practice.play("mieć", wrong = 0)
        practice.play("być", wrong = 5, level = "type")

        assertEquals(14, practice.setup(Game.VERB_CHALLENGE, listOf("być", "choose")).best)
        assertEquals(14, practice.setup(Game.VERB_CHALLENGE, listOf("mieć", "choose")).best)
        assertEquals(11, practice.setup(Game.VERB_CHALLENGE, listOf("być", "type")).best)
        assertEquals(null, practice.setup(Game.VERB_CHALLENGE, listOf("mixed", "choose")).best)
    }

    @Test
    fun `the Practice screen shows the Best of the way the Game was last played`() {
        val practice = Player().completed("d1", "d2").practice()
        assertEquals(GameView(Game.VERB_CHALLENGE, GameState.READY), practice.view().games.single { it.game == Game.VERB_CHALLENGE })

        practice.play("być", wrong = 2)
        practice.play("mieć", wrong = 1, level = "type")

        assertEquals(
            GameView(Game.VERB_CHALLENGE, GameState.READY, best = 13, lastPlayed = listOf(OptionView("mieć", "mieć"), OptionView("type"))),
            practice.view().games.single { it.game == Game.VERB_CHALLENGE },
        )
        // The Game opens the way it was last played.
        assertEquals(listOf("mieć", "type"), practice.setup(Game.VERB_CHALLENGE).selected)
    }

    @Test
    fun `an option that is not offered gives way to the first of its choice`() {
        val practice = Player().completed("d1").practice()

        assertEquals(listOf("być", "choose"), practice.setup(Game.VERB_CHALLENGE, listOf("mieć", "expert")).selected)
    }

    @Test
    fun `today's Rounds are counted and the count starts again the next day`() {
        val player = Player().completed("d1")
        val practice = player.practice()
        assertEquals(0, practice.view().roundsToday)

        practice.play("być", wrong = 0)
        practice.play("być", wrong = 0)
        assertEquals(2, practice.view().roundsToday)

        player.clock.moveTo(march(3))
        assertEquals(0, player.practice().view().roundsToday)
    }

    @Test
    fun `Bests are kept when the app is closed, in a file of their own`() {
        val folder = Files.createTempDirectory("practice").toFile()
        val file = File(folder, "practice.json")
        val progressFile = File(folder, "progress.json")
        val player = Player(progress = FileLearnerProgress(progressFile), bests = FilePracticeProgress(file)).completed("d1")
        val pathProgress = progressFile.readText()
        player.practice().play("być", wrong = 3)

        val reopened = Player(progress = FileLearnerProgress(progressFile), bests = FilePracticeProgress(file))
        assertEquals(13, reopened.practice().setup(Game.VERB_CHALLENGE).best)
        assertEquals(1, reopened.practice().view().roundsToday)
        assertEquals(pathProgress, progressFile.readText())
        assertFalse(File(folder, "practice.json.tmp").exists())
    }

    @Test
    fun `a Bests file that cannot be read is moved aside and the Learner starts with none`() {
        val folder = Files.createTempDirectory("practice").toFile()
        val file = File(folder, "practice.json").apply { writeText("{ not json") }

        val bests = FilePracticeProgress(file)

        assertEquals(null, bests.best("verb-challenge:być:choose"))
        assertEquals("{ not json", File(folder, "practice.json.corrupt").readText())
        bests.recordRound("verb-challenge", listOf("być", "choose"), "verb-challenge:być:choose", 9, march(2))
        assertEquals(9, FilePracticeProgress(file).best("verb-challenge:być:choose"))
    }

    @Test
    fun `a Bests file from a later version is read for what this one knows`() {
        val folder = Files.createTempDirectory("practice").toFile()
        val file = File(folder, "practice.json")
        file.writeText("""{"version":2,"bests":{"verb-challenge:być:choose":12},"medals":["gold"]}""")

        assertEquals(12, FilePracticeProgress(file).best("verb-challenge:być:choose"))
    }
}
