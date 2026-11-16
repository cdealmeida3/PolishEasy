package com.polisheasy.engine

import java.io.File
import java.io.PrintStream
import kotlin.system.exitProcess
import kotlinx.serialization.json.Json

private val recordJson = Json { prettyPrint = true }

private const val USAGE = "Usage: [--record] <content folder> <released-ids file>"

/**
 * The content check as the build runs it. [args] are the content folder and the file that
 * records the identifiers that have shipped. Prints one line for each problem to [err] and
 * returns the exit code: 0 when there is none. With `--record` in front, adds the content's
 * identifiers to the file instead of checking.
 */
fun runContentCheck(args: List<String>, out: PrintStream, err: PrintStream): Int {
    val record = args.firstOrNull() == "--record"
    val paths = if (record) args.drop(1) else args
    if (paths.size != 2) {
        err.println(USAGE)
        return 2
    }
    val files = DirectoryContent(File(paths[0]))
    val releasedFile = File(paths[1])
    val released = try {
        if (releasedFile.isFile) recordJson.decodeFromString(ReleasedIds.serializer(), releasedFile.readText()) else null
    } catch (failure: IllegalArgumentException) {
        err.println(ContentProblem(releasedFile.name, null, "Cannot be read: ${failure.message?.lineSequence()?.firstOrNull()}"))
        err.println("Content check failed: 1 problem")
        return 1
    }

    if (record) {
        val updated = (released ?: ReleasedIds()).with(identifiersOf(files))
        releasedFile.writeText(recordJson.encodeToString(ReleasedIds.serializer(), updated) + "\n")
        out.println("Recorded the content's identifiers in ${releasedFile.name}")
        return 0
    }

    val problems = checkContent(files, released) + listOfNotNull(
        ContentProblem(
            releasedFile.name,
            null,
            "There is no such file. Run ./gradlew :engine:recordContentIds and commit it",
        ).takeIf { released == null },
    )
    if (problems.isEmpty()) {
        out.println("Content check passed")
        return 0
    }
    problems.forEach(err::println)
    err.println("Content check failed: ${problems.size} ${if (problems.size == 1) "problem" else "problems"}")
    return 1
}

fun main(args: Array<String>) {
    exitProcess(runContentCheck(args.toList(), System.out, System.err))
}
