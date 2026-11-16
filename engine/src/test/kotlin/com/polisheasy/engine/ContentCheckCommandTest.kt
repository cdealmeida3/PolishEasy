package com.polisheasy.engine

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The content check as the build runs it: against a folder, with the record of shipped identifiers in a file. */
internal class ContentCheckCommandTest {
    private val root: File = createTempDirectory("content-check").toFile()
    private val contentDir = File(root, "content")
    private val releasedIds = File(root, "released-ids.json")

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    private class Run(val exitCode: Int, val out: String, val err: String)

    private fun run(vararg options: String): Run {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val exitCode = runContentCheck(
            options.toList() + listOf(contentDir.path, releasedIds.path),
            PrintStream(out, true, Charsets.UTF_8),
            PrintStream(err, true, Charsets.UTF_8),
        )
        return Run(exitCode, out.toString(Charsets.UTF_8), err.toString(Charsets.UTF_8))
    }

    /** Replaces the content folder with [fixture]'s files. */
    private fun write(fixture: CheckerFixture) {
        contentDir.deleteRecursively()
        fixture.texts.forEach { (path, text) ->
            File(contentDir, path).apply { parentFile.mkdirs() }.writeText(text)
        }
    }

    private fun record(ids: ReleasedIds) = releasedIds.writeText(contentJson.encodeToString(ReleasedIds.serializer(), ids))

    private val broken = CheckerFixture.valid
        .withExercise("a1-u01-d01-e01") { (it as ChooseTranslation).copy(correctOption = 9) }
        .without("audio/c1.ogg")

    @Test
    fun `a valid folder passes and prints no problem`() {
        write(CheckerFixture.valid)
        record(CheckerFixture.shipped)

        val run = run()

        assertEquals(0, run.exitCode, run.err)
        assertEquals("", run.err)
    }

    @Test
    fun `a broken folder fails, printing one line for each problem and then how many there are`() {
        write(broken)
        record(CheckerFixture.shipped)

        val run = run()

        assertEquals(1, run.exitCode)
        val lines = run.err.trim().lines()
        assertEquals(3, lines.size, run.err)
        assertTrue(lines.any { it.startsWith("a1/unit-01/day-01.json: a1-u01-d01-e01: ") }, run.err)
        assertTrue(lines.any { it.startsWith("audio/clips.json: c1: ") }, run.err)
        assertEquals("Content check failed: 2 problems", lines.last())
    }

    @Test
    fun `a folder with no record of shipped identifiers fails and says so`() {
        write(CheckerFixture.valid)

        val run = run()

        assertEquals(1, run.exitCode)
        assertTrue("released-ids.json" in run.err && "recordContentIds" in run.err, run.err)
        assertTrue(run.err.trim().endsWith("Content check failed: 1 problem"), run.err)
    }

    @Test
    fun `a record of shipped identifiers that cannot be read fails and says so`() {
        write(CheckerFixture.valid)
        releasedIds.writeText("{ not json")

        assertEquals(1, run().exitCode)
        assertEquals(1, run("--record").exitCode)
        assertEquals("{ not json", releasedIds.readText())
    }

    @Test
    fun `recording writes every identifier, after which the check passes`() {
        write(CheckerFixture.valid)

        assertEquals(0, run("--record").exitCode)

        assertEquals(CheckerFixture.shipped, contentJson.decodeFromString(ReleasedIds.serializer(), releasedIds.readText()))
        assertEquals(0, run().exitCode)
    }

    @Test
    fun `recording keeps an identifier the content no longer has, so the check still reports it`() {
        write(CheckerFixture.valid)
        run("--record")
        write(CheckerFixture.valid.withDailySet(CheckerFixture.DAY_2) { it.copy(exercises = it.exercises.dropLast(1)) })

        assertEquals(0, run("--record").exitCode)

        assertEquals(CheckerFixture.shipped, contentJson.decodeFromString(ReleasedIds.serializer(), releasedIds.readText()))
        val check = run()
        assertEquals(1, check.exitCode)
        assertTrue("released-ids.json: a1-u01-d02-e08: " in check.err, check.err)
    }

    @Test
    fun `recording twice leaves the file as it was`() {
        write(CheckerFixture.valid)
        run("--record")
        val first = releasedIds.readText()

        run("--record")

        assertEquals(first, releasedIds.readText())
        assertTrue(first.endsWith("\n"))
    }

    @Test
    fun `the wrong number of arguments fails and says how to run it`() {
        val err = ByteArrayOutputStream()

        assertEquals(2, runContentCheck(listOf(contentDir.path), PrintStream(ByteArrayOutputStream()), PrintStream(err)))
        assertTrue("Usage" in err.toString(), err.toString())
    }
}
