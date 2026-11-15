package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LoadingContentTest {
    @Test
    fun `a Daily Set without Tips loads`() {
        val course = FixtureCourse.loadWithDayOne(dayOne(tips = null))

        assertEquals(emptyList(), course.path.first().tips)
    }

    @Test
    fun `a Daily Set with more than two Tips is rejected`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            FixtureCourse.loadWithDayOne(dayOne(tips = listOf("e01", "e02", "e03")))
        }

        assertNamesTheDailySet(failure)
    }

    @Test
    fun `a Tip placed before an Exercise that is not in the Daily Set is rejected`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            FixtureCourse.loadWithDayOne(dayOne(tips = listOf("e99")))
        }

        assertNamesTheDailySet(failure)
    }

    @Test
    fun `two Tips placed before the same Exercise are rejected`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            FixtureCourse.loadWithDayOne(dayOne(tips = listOf("e02", "e02")))
        }

        assertNamesTheDailySet(failure)
    }

    @Test
    fun `a typing Exercise with no Accepted Answers is rejected`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            FixtureCourse.loadWithDayOne(typingDay())
        }

        assertNamesTheDailySet(failure)
        assertTrue("a1-u01-d01-e01" in failure.message.orEmpty(), "Message was: ${failure.message}")
    }

    @Test
    fun `a typing Exercise with a blank or punctuation-only Accepted Answer is rejected`() {
        listOf("", "   ", "?!").forEach { blank ->
            val failure = assertFailsWith<IllegalArgumentException>("Accepted Answer \"$blank\"") {
                FixtureCourse.loadWithDayOne(typingDay("dziękuję", blank))
            }

            assertNamesTheDailySet(failure)
        }
    }

    private fun assertNamesTheDailySet(failure: IllegalArgumentException) =
        assertTrue("a1-u01-d01" in failure.message.orEmpty(), "Message was: ${failure.message}")

    /** Day 1 with three Exercises and one Tip before each Exercise named in [tips]. */
    private fun dayOne(tips: List<String>?): String {
        val tipsJson = tips?.mapIndexed { index, before ->
            """{ "id": "t$index", "before": "$before", "title": { "en": "Title" }, "body": { "en": "Body" } }"""
        }?.joinToString(prefix = "\"tips\": [", postfix = "],")
        val exercises = listOf("e01", "e02", "e03").joinToString { id ->
            """{ "type": "choose_translation", "id": "$id", "prompt": "tak",
                 "options": [{ "en": "yes" }, { "en": "no" }], "correctOption": 0 }"""
        }
        return """{ "id": "a1-u01-d01", ${tipsJson.orEmpty()} "exercises": [$exercises] }"""
    }
}
