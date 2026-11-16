package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals

/** What the command that makes the Clips is told of the content. */
class SpokenContentTest {
    private val valid = CheckerFixture.valid

    @Test
    fun `it lists what the course speaks and where the manifest is`() {
        val spoken = spokenContent(valid.files()::read)

        assertEquals(loadCourse(valid.files()::read).spokenTexts(), spoken.texts)
        assertEquals(CheckerFixture.CLIPS, spoken.manifest)
        assertEquals(emptyList(), spoken.unreadable)
    }

    @Test
    fun `content whose manifest is not there yet, and whose listening Exercises so have no Clip, is still read`() {
        val spoken = spokenContent(valid.without(CheckerFixture.CLIPS).files()::read)

        assertEquals(loadCourse(valid.files()::read).spokenTexts(), spoken.texts)
        assertEquals(emptyList(), spoken.unreadable)
    }

    @Test
    fun `a Daily Set that cannot be read is reported, because its texts are missing`() {
        val spoken = spokenContent(valid.withFile(CheckerFixture.DAY_2, "{").files()::read)

        assertEquals(listOf(CheckerFixture.DAY_2), spoken.unreadable.map { it.file })
    }
}
