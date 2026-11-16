package com.polisheasy.polishcheck

import com.polisheasy.engine.ContentProblem

private fun count(number: Int, one: String, many: String = one + "s") = "$number ${if (number == 1) one else many}"

/** The problem as one console line, in the shape of the content checker's: file, id, what is wrong. */
fun consoleLine(problem: PolishProblem): String {
    val first = problem.places.firstOrNull() ?: Place(ACCEPTED_FILE, null)
    val more = problem.places.size - 1
    val others = if (more > 0) " (and ${count(more, "more place")})" else ""
    return ContentProblem(first.file, first.id, problem.message + others).toString()
}

/** What the check passed with, for the console's last line and the report's first. */
fun summary(report: PolishReport): String {
    val counts = listOf(count(report.sentences, "sentence"), count(report.verbForms, "verb form"), count(report.tips, "Tip")) +
        listOfNotNull(count(report.accepted.size, "accepted problem").takeIf { report.accepted.isNotEmpty() })
    return if (report.problems.isEmpty()) {
        "Polish check passed: ${counts.joinToString(", ")}"
    } else {
        "Polish check failed: ${count(report.problems.size, "problem")}"
    }
}

/**
 * The report as Markdown. It has no date and no timing, so the same content gives the same file.
 * [toReviewPath] is where the unreviewed sentences were written.
 */
fun reportMarkdown(report: PolishReport, sources: String, toReviewPath: String): String = buildString {
    appendLine("# Polish check")
    appendLine()
    appendLine(summary(report) + if (report.problems.isEmpty()) "." else ". ${counted(report)}.")
    appendLine()
    appendLine("| Pass | Looked at | Problems | Accepted |")
    appendLine("|---|---|---|---|")
    val units = mapOf(
        Pass.DICTIONARY to "distinct words",
        Pass.VERB_FORMS to "claimed forms",
        Pass.GRAMMAR to "texts",
        Pass.REVIEW to "sentences and Tips",
    )
    units.forEach { (pass, unit) ->
        val problems = report.problems.count { it.pass == pass }
        val accepted = report.accepted.count { it.problem.pass == pass }
        appendLine("| ${pass.title} | ${report.looked[pass] ?: 0} $unit | $problems | $accepted |")
    }
    appendLine()

    appendLine("## Problems")
    appendLine()
    if (report.problems.isEmpty()) {
        appendLine("There are no problems.")
        appendLine()
    }
    val waiting = report.problems.filter { it.key.startsWith("unreviewed: ") }
    report.problems.filter { it !in waiting }.groupBy { it.pass }.forEach { (pass, problems) ->
        appendLine("### ${pass.title}")
        appendLine()
        problems.forEach { problem ->
            appendLine("- `${problem.key}`")
            appendLine("  ${problem.message}")
            problem.places.forEach { appendLine("  - ${listOfNotNull(it.file, it.id).joinToString(": ")}") }
        }
        appendLine()
    }

    appendLine("## Awaiting review")
    appendLine()
    if (waiting.isEmpty()) {
        appendLine("Every sentence and every Tip has been reviewed.")
    } else {
        val sentences = report.toReview.sentences.size
        val tips = report.toReview.tips.size
        appendLine(
            "${count(sentences, "sentence")} and ${count(tips, "Tip")} have no verdict. They are listed in `$toReviewPath`. " +
                "Have them reviewed by someone who did not draft them, then record the reviewed file:",
        )
        appendLine()
        appendLine("```")
        appendLine("./gradlew :polishcheck:recordPolishReview -Previewed=<file>")
        appendLine("```")
    }
    appendLine()

    appendLine("## Accepted")
    appendLine()
    if (report.accepted.isEmpty()) appendLine("No problem is accepted.")
    report.accepted.forEach { (problem, reason) ->
        appendLine("- `${problem.key}`")
        appendLine("  ${problem.message}")
        appendLine("  Accepted because: $reason")
        problem.places.forEach { appendLine("  - ${listOfNotNull(it.file, it.id).joinToString(": ")}") }
    }
    appendLine()

    appendLine("## Sources")
    appendLine()
    appendLine("$sources. What each pass can and cannot see, and the licences, are in `docs/polish-check.md`.")
}

private fun counted(report: PolishReport): String =
    listOf(count(report.sentences, "sentence"), count(report.verbForms, "verb form"), count(report.tips, "Tip")).joinToString(", ")
