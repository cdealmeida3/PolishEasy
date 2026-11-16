package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The marks an author puts around the Polish in a Tip's body, and what is wrong with them when they are wrong. */
class PolishMarksTest {
    @Test
    fun `the marks are taken out of the text and say where the Polish is`() {
        val read = readPolishMarks("*Dziękuję* is thank you: *dziękuję bardzo*.")

        assertEquals("Dziękuję is thank you: dziękuję bardzo.", read.text)
        assertEquals(listOf("Dziękuję", "dziękuję bardzo"), read.polish.map { read.text.substring(it) })
    }

    @Test
    fun `text without marks is read as it is`() {
        assertEquals(MarkedText("Tap the speaker.", emptyList()), readPolishMarks("Tap the speaker."))
    }

    @Test
    fun `well marked text has no problem, with letters, endings and a question mark inside the marks`() {
        assertNull(marksProblem("*Ś* and *ć* end in *-my*: *jak się masz?* Tap the speaker."))
    }

    @Test
    fun `a mark without its partner is a problem`() {
        assertEquals("has a * without its partner", marksProblem("*Cześć is hello."))
    }

    @Test
    fun `an empty pair and a pair with a space at its edge are problems`() {
        assertEquals("has a pair of * with nothing between them", marksProblem("Say ** now."))
        assertEquals("marks \"tak \" with a space at its edge", marksProblem("Say *tak *now."))
    }

    @Test
    fun `a word spelt with a Polish letter outside the marks is a problem`() {
        assertEquals("has the Polish \"proszę\" outside the marks: write *proszę*", marksProblem("*Dziękuję* and proszę."))
    }
}
