package com.polisheasy.polishcheck

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The Polish check as the build runs it: against a folder, with its two files beside it. */
internal class PolishCheckCommandTest {
    private val root: File = createTempDirectory("polish-check").toFile()
    private val contentDir = File(root, "content")
    private val reviews = File(root, REVIEWS_FILE)
    private val accepted = File(root, ACCEPTED_FILE)
    private val outDir = File(root, "out")
    private val report = File(outDir, "report.md")
    private val toReview = File(outDir, "to-review.json")

    private val day = "a1/unit-01/day-01.json"
    private val sampleFiles = listOf("course.json", "a1/unit-01/items.json", "practice/practice.json", day)

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    private class Run(val exitCode: Int, val out: String, val err: String)

    private fun run(vararg options: String): Run {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val exitCode = runPolishCheck(
            options.toList() + listOf(contentDir.path, reviews.path, accepted.path, outDir.path),
            PrintStream(out, true, Charsets.UTF_8),
            PrintStream(err, true, Charsets.UTF_8),
        )
        return Run(exitCode, out.toString(Charsets.UTF_8), err.toString(Charsets.UTF_8))
    }

    /** Writes the sample content, with [old] replaced by [new] in the Daily Set. */
    private fun writeSample(old: String = "", new: String = "") {
        sampleFiles.forEach { path ->
            val text = checkNotNull(javaClass.getResource("/sample/$path")) { "No sample file $path" }.readText()
            File(contentDir, path).apply { parentFile.mkdirs() }.writeText(if (path == day && old.isNotEmpty()) text.replace(old, new) else text)
        }
    }

    /** Records a verdict of "right" on everything the content now has. */
    private fun reviewEverything() {
        run()
        assertEquals(0, run("--record", toReview.path).exitCode)
    }

    private fun accept(key: String, reason: String) =
        accepted.writeText(reviewJson.encodeToString(Accepted.serializer(), Accepted(listOf(Acceptance(key, reason)))))

    private fun writeMisspelt() = writeSample("jesteśmy z Polski", "jestesmy z Polski")

    @Test
    fun `the right sample, fully reviewed, passes and writes a report with no problems`() {
        writeSample()
        reviewEverything()

        val run = run()

        assertEquals(0, run.exitCode, run.err)
        assertEquals("", run.err)
        assertEquals("Polish check passed: 14 sentences, 6 verb forms, 1 Tip", run.out.trim())
        assertTrue("There are no problems." in report.readText(), report.readText())
        assertTrue("LanguageTool 6.6" in report.readText(), report.readText())
    }

    @Test
    fun `a misspelt word fails, printing a line that starts with the file and id, and the report lists it under its pass`() {
        writeMisspelt()
        reviewEverything()

        val run = run()

        assertEquals(1, run.exitCode)
        val lines = run.err.trim().lines()
        assertEquals("$day: a1-u01-d01-e03: jestesmy is not in the dictionary", lines.first())
        assertTrue(lines.last().startsWith("Polish check failed: 1 problem. Report: "), run.err)
        assertTrue(lines.last().endsWith("report.md"), run.err)
        assertEquals(2, lines.size, run.err)
        val text = report.readText()
        assertTrue("### Dictionary\n\n- `dictionary: jestesmy`\n  jestesmy is not in the dictionary\n  - $day: a1-u01-d01-e03" in text, text)
    }

    @Test
    fun `with its key and a reason in the accepted file, the problem no longer fails and is listed as accepted`() {
        writeMisspelt()
        reviewEverything()
        accept("dictionary: jestesmy", "Only here to test acceptance.")

        val run = run()

        assertEquals(0, run.exitCode, run.err)
        assertEquals("Polish check passed: 14 sentences, 6 verb forms, 1 Tip, 1 accepted problem", run.out.trim())
        val text = report.readText()
        assertTrue("## Accepted\n\n- `dictionary: jestesmy`" in text, text)
        assertTrue("Accepted because: Only here to test acceptance." in text, text)
    }

    @Test
    fun `with no reviews file every sentence and Tip is unreviewed and listed for review with its uses`() {
        writeSample()

        val run = run()

        assertEquals(1, run.exitCode)
        assertEquals(15, run.err.lines().count { it.endsWith("has not been reviewed") || "has not been reviewed (and " in it }, run.err)
        assertTrue("$day: a1-u01-d01-t01: Tip a1-u01-d01-t01 has not been reviewed" in run.err, run.err)
        val listed = reviewJson.decodeFromString(Reviews.serializer(), toReview.readText())
        assertEquals(14, listed.sentences.size)
        assertEquals(1, listed.tips.size)
        assertTrue(listed.sentences.all { !it.uses.isNullOrEmpty() }, toReview.readText())
        assertTrue(listed.tips.single().text!!.startsWith("The verb być\n"), toReview.readText())
        assertTrue("14 sentences and 1 Tip have no verdict" in report.readText(), report.readText())
        assertFalse(reviews.exists())
    }

    @Test
    fun `recording what was listed for review passes the check, and the record holds no context`() {
        writeSample()
        run()

        val record = run("--record", toReview.path)

        assertEquals(0, record.exitCode, record.err)
        assertEquals(0, run().exitCode)
        val recorded = reviews.readText()
        assertFalse("uses" in recorded || "The verb" in recorded, recorded)
        assertTrue(recorded.endsWith("}\n"))
        val texts = reviewJson.decodeFromString(Reviews.serializer(), recorded).sentences.map { it.text }
        assertEquals(texts.sorted(), texts)
    }

    @Test
    fun `recording a file with a finding fails the check with the finding`() {
        writeSample()
        run()
        val listed = reviewJson.decodeFromString(Reviews.serializer(), toReview.readText())
        val reviewed = File(root, "reviewed.json")
        reviewed.writeText(
            reviewJson.encodeToString(
                Reviews.serializer(),
                listed.copy(sentences = listed.sentences.map { if (it.text == "my jesteśmy tutaj") it.copy(finding = "the pronoun is not needed") else it }),
            ),
        )

        assertEquals(0, run("--record", reviewed.path).exitCode)
        val run = run()

        assertEquals(1, run.exitCode)
        assertEquals("$day: a1-u01-d01-e04: the pronoun is not needed", run.err.lines().first())
        assertTrue("### Review\n\n- `review: my jesteśmy tutaj`" in report.readText(), report.readText())
    }

    @Test
    fun `recording drops verdicts on sentences the content no longer has`() {
        writeSample()
        reviewEverything()
        assertTrue("dzięki" in reviews.readText())
        writeSample(", \"readingAnswers\": [\"dzięki\"]", "")
        run()

        assertEquals(0, run("--record", toReview.path).exitCode)

        assertFalse("dzięki" in reviews.readText(), reviews.readText())
        assertEquals(0, run().exitCode)
    }

    @Test
    fun `recording refuses a file with a verdict on something the content does not have, changing nothing`() {
        writeSample()
        reviewEverything()
        val before = reviews.readText()
        val reviewed = File(root, "reviewed.json")
        reviewed.writeText("""{ "sentences": [ { "text": "dobry wieczór", "meaning": "good evening" } ], "tips": [ { "id": "a1-u01-d01-t01", "hash": "000000000000" } ] }""")

        val run = run("--record", reviewed.path)

        assertEquals(1, run.exitCode)
        assertTrue("\"dobry wieczór\" meaning \"good evening\" is not in the content" in run.err, run.err)
        assertTrue("Tip a1-u01-d01-t01" in run.err, run.err)
        assertEquals(before, reviews.readText())
    }

    @Test
    fun `checking twice gives the same report, byte for byte`() {
        writeMisspelt()
        run()
        val first = report.readBytes()
        val firstToReview = toReview.readBytes()

        run()

        assertTrue(first.contentEquals(report.readBytes()))
        assertTrue(firstToReview.contentEquals(toReview.readBytes()))
    }

    @Test
    fun `a content file that cannot be read fails and says to run the content checker`() {
        writeSample()
        File(contentDir, day).writeText("{ not json")

        val run = run()

        assertEquals(1, run.exitCode)
        assertTrue(run.err.startsWith("$day: Cannot be read"), run.err)
        assertTrue(":engine:checkContent" in run.err, run.err)
        assertFalse(report.exists())
    }

    @Test
    fun `a reviews file that cannot be read fails and names it`() {
        writeSample()
        reviews.writeText("{ not json")

        val run = run()

        assertEquals(1, run.exitCode)
        assertTrue(run.err.startsWith("$REVIEWS_FILE: Cannot be read"), run.err)
    }

    @Test
    fun `the wrong number of arguments fails and says how to run it`() {
        val err = ByteArrayOutputStream()

        assertEquals(2, runPolishCheck(listOf(contentDir.path), PrintStream(ByteArrayOutputStream()), PrintStream(err)))
        assertEquals(2, runPolishCheck(listOf("--record", "a", "b", "c", "d"), PrintStream(ByteArrayOutputStream()), PrintStream(err)))
        assertTrue("Usage" in err.toString(), err.toString())
    }
}
