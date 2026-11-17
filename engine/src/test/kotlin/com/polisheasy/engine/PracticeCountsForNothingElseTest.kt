package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class PracticeCountsForNothingElseTest {
    @Test
    fun `a Round changes nothing on the Path, the Streak, the Reminder or Review`() {
        val player = Player().completed("d1", "d2")
        val before = player.engine
        val completed = player.progress.completedDailySets()
        val items = player.progress.itemProgress()
        val practice = player.practice()

        practice.finish(practice.verbRound("być").playOut(wrong = 3))

        val after = player.engine
        assertEquals(completed, player.progress.completedDailySets())
        assertEquals(items, player.progress.itemProgress())
        assertEquals(null, player.progress.dailySetInProgress())
        assertEquals(before.streak(), after.streak())
        assertEquals(before.today(), after.today())
        assertEquals(before.path(), after.path())
        assertEquals(before.soFar(), after.soFar())
        assertEquals(before.reminderDue(), after.reminderDue())
    }

    @Test
    fun `a Round left unfinished is not kept`() {
        val player = Player().completed("d1")
        val practice = player.practice()

        practice.verbRound("być").answerAndMoveOn()

        assertEquals(0, player.practice().view().roundsToday)
        assertEquals(null, player.practice().setup(Game.VERB_CHALLENGE).best)
    }
}
