package com.polisheasy.polishcheck

import com.polisheasy.engine.ContentProblem
import com.polisheasy.engine.DirectoryContent
import com.polisheasy.engine.polishContent
import java.io.File
import java.io.PrintStream
import kotlin.system.exitProcess
import kotlinx.serialization.SerializationException

private const val USAGE =
    "Usage: [--record <reviewed file>] <content folder> <reviews file> <accepted file> <output folder>"

/** The reference takes seconds to start, so one is shared by every run in the same process. */
private val sharedReference by lazy { PolishReference() }

/** [file] as someone at the working directory would write it. */
private fun shown(file: File): String =
    file.absoluteFile.relativeToOrNull(File("").absoluteFile)?.invariantSeparatorsPath?.takeIf { !it.startsWith("..") } ?: file.path

private inline fun <reified T> read(file: File, empty: T, err: PrintStream): T? = try {
    if (file.isFile) reviewJson.decodeFromString<T>(file.readText()) else empty
} catch (failure: SerializationException) {
    err.println(ContentProblem(file.name, null, "Cannot be read: ${failure.message?.lineSequence()?.firstOrNull()}"))
    null
}

/**
 * The Polish check as the build runs it. [args] are the content folder, the file of review
 * verdicts, the file of accepted problems and the folder for the report. Prints one line for each
 * problem that is not accepted to [err] and returns the exit code: 0 when there is none, 1 when
 * there is any, 2 when [args] are wrong. With `--record <reviewed file>` in front, merges that
 * file into the review verdicts instead of checking.
 */
fun runPolishCheck(args: List<String>, out: PrintStream, err: PrintStream): Int {
    val record = args.firstOrNull() == "--record"
    val paths = if (record) args.drop(2) else args
    if (paths.size != 4 || (record && args.size != 6)) {
        err.println(USAGE)
        return 2
    }
    val reviewsFile = File(paths[1])
    val acceptedFile = File(paths[2])
    val outDir = File(paths[3])

    val content = polishContent(DirectoryContent(File(paths[0])))
    if (content.unreadable.isNotEmpty()) {
        content.unreadable.forEach(err::println)
        err.println("Polish check failed: the content cannot be read. Run ./gradlew :engine:checkContent")
        return 1
    }
    val reviews = read(reviewsFile, Reviews(), err)
    val accepted = read(acceptedFile, Accepted(), err)
    if (reviews == null || accepted == null) {
        err.println("Polish check failed: a file of the check cannot be read")
        return 1
    }

    if (record) {
        val reviewedFile = File(args[1])
        if (!reviewedFile.isFile) {
            err.println(ContentProblem(reviewedFile.name, null, "There is no such file"))
            return 1
        }
        val reviewed = read(reviewedFile, Reviews(), err) ?: return 1
        val strangers = reviewed.strangersTo(content)
        if (strangers.isNotEmpty()) {
            strangers.forEach { err.println(ContentProblem(reviewedFile.name, null, "$it is not in the content as it is now")) }
            err.println("Nothing was recorded. Run the check again and review what it lists")
            return 1
        }
        reviewsFile.writeText(reviewJson.encodeToString(Reviews.serializer(), reviews.with(reviewed, content)) + "\n")
        out.println(
            "Recorded ${reviewed.sentences.size} sentences and ${reviewed.tips.size} Tips in ${reviewsFile.name}",
        )
        return 0
    }

    val report = checkPolish(content, sharedReference, reviews, accepted)
    outDir.mkdirs()
    val reportFile = File(outDir, "report.md")
    val toReviewFile = File(outDir, "to-review.json")
    toReviewFile.writeText(reviewJson.encodeToString(Reviews.serializer(), report.toReview) + "\n")
    reportFile.writeText(reportMarkdown(report, sharedReference.version, shown(toReviewFile)))

    if (report.problems.isEmpty()) {
        out.println(summary(report))
        return 0
    }
    report.problems.forEach { err.println(consoleLine(it)) }
    err.println("${summary(report)}. Report: ${shown(reportFile)}")
    return 1
}

fun main(args: Array<String>) {
    // Without this a Gradle JavaExec prints Polish letters as question marks.
    val out = PrintStream(System.out, true, "UTF-8")
    val err = PrintStream(System.err, true, "UTF-8")
    exitProcess(runPolishCheck(args.toList(), out, err))
}
