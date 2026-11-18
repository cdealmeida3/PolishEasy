package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StoryStripTest {
    private val words = listOf("najpierw", "potem", "wstalam", "zjadlam", "poszlam", "spalam", "czytalam", "gralem", "jadlem", "bylem").map { item(it) }

    private fun story(title: String, vararg panels: Pair<String, List<String>>) = Story(
        mapOf("en" to title),
        panels.map { (text, items) -> StoryPanel(text, mapOf("en" to "$text (en)"), items.last(), items) },
    )

    /** l1 teaches Ewa's morning, l2 Adam's Saturday, l3 the last story's words. */
    private val stories = listOf(
        story("Ewa", "Najpierw wstałam." to listOf("najpierw", "wstalam"), "Potem zjadłam." to listOf("potem", "zjadlam"), "Potem poszłam." to listOf("potem", "poszlam")),
        story("Adam", "Najpierw grałem." to listOf("najpierw", "gralem"), "Potem jadłem." to listOf("potem", "jadlem"), "Potem byłem." to listOf("potem", "bylem")),
        story("Kasia", "Najpierw spałam." to listOf("najpierw", "spalam"), "Potem czytałam." to listOf("potem", "czytalam")),
    )

    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("l1", *words.take(5).map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
                day("l2", *words.drop(7).map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
                day("l3", *words.drop(5).take(2).map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
            ),
        ),
        words,
        pictures = words.associate { it.id to Picture.Glyph(it.id.take(1)) },
        stories = stories,
    )

    private fun player(seed: Int = 0) = Player(course = course, seed = seed)

    private fun Practice.round(): StoryRound = start(setup(Game.STORY_STRIP, listOf(StoryStrip.READ))) as StoryRound

    @Test
    fun `Story strip opens with a story whose words are all learned, read without a Clip`() {
        val state = { p: Player -> p.practice().view().games.single { it.game == Game.STORY_STRIP }.state }
        assertEquals(GameState.NEEDS_MORE, state(player()))
        assertEquals(GameState.NEEDS_MORE, state(player().completed("l2")))
        assertEquals(GameState.READY, state(player().completed("l1")))
        assertEquals(listOf(StoryStrip.READ), player().completed("l1").practice().setup(Game.STORY_STRIP).choices[0].options.map { it.id })
        assertEquals(listOf("Ewa"), player().completed("l1").practice().round().stories.map { it.title })
    }

    @Test
    fun `a Round is up to three stories, their pictures mixed and never in the story's order`() {
        repeat(10) { seed ->
            val round = player(seed).completed("l1", "l2", "l3").practice().round()
            assertEquals(StoryStrip.STORIES, round.stories.size)
            assertEquals(8, round.total)
            round.stories.forEach { view ->
                val story = stories.single { it.text == view.key }
                assertEquals(story.panels.map { it.text }, view.order.map { view.tiles[it].text })
                assertNotEquals(story.panels.map { it.text }, view.tiles.map { it.text })
                assertEquals(view.tiles.indices.toSet(), view.order.toSet())
            }
        }
    }

    @Test
    fun `the pictures are tapped in the story's order, a wrong one is marked and the right one put in its place`() {
        var round = player().completed("l1").practice().round()
        val story = checkNotNull(round.story)
        assertEquals("Najpierw wstałam. Potem zjadłam. Potem poszłam.", story.text)
        assertEquals(emptyList(), round.placed)

        round = round.choose(story.order[0])
        assertEquals(true, round.given?.correct)
        assertEquals(listOf(story.order[0]), round.placed)
        assertEquals(round, round.choose(story.order[1]))
        round = round.next()
        // A picture already in the strip cannot be tapped again.
        assertEquals(round, round.choose(story.order[0]))

        round = round.choose(story.order[2])
        assertEquals(false, round.given?.correct)
        assertEquals(story.order[2], round.chosen)
        assertEquals(story.order.take(2), round.placed)
        round = round.next().choose(story.order[2]).next()
        assertTrue(round.finished)
        assertNull(round.story)
        assertEquals(Score(2, of = 3), round.score)
    }
}
