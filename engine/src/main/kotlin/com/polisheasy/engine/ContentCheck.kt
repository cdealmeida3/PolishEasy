package com.polisheasy.engine

import java.io.File
import java.util.Locale
import kotlinx.serialization.Serializable

/**
 * One thing wrong with the Authored Content. [file] is the content file to open, relative to the
 * content root. [id] is the most specific identifier concerned, or null when the problem is the
 * file itself.
 */
data class ContentProblem(val file: String, val id: String?, val message: String) {
    override fun toString() = listOfNotNull(file, id, message).joinToString(": ")
}

/** The content files as the checker sees them. Paths are relative to the content root, with `/`. */
interface ContentFiles {
    /** The text of the file at [path]. Throws if there is no such file. */
    fun read(path: String): String

    /** Every file under the root. */
    fun paths(): Set<String>
}

/** The content files in a folder on disk. */
class DirectoryContent(private val root: File) : ContentFiles {
    override fun read(path: String): String {
        val file = File(root, path)
        require(file.isFile) { "There is no such file" }
        return file.readText()
    }

    override fun paths(): Set<String> =
        root.walkTopDown().filter { it.isFile }.map { it.relativeTo(root).invariantSeparatorsPath }.toSet()
}

/** An Item practised by fewer Exercises than this gives Review nothing to vary. */
const val MIN_EXERCISES_PER_ITEM = 2

/** How many Exercises a Daily Set may have. */
val DAILY_SET_EXERCISES = 12..18

/** How many pairs a match_pairs Exercise has: the book's rows. */
val MATCH_PAIRS = 4..6

/** How many statements a true_false Exercise has. */
val TRUE_FALSE_STATEMENTS = 3..5

/** How many lines an order_lines dialogue has. */
val ORDER_LINES = 3..6

/** The most Items one Daily Set may introduce. */
const val MAX_NEW_ITEMS = 8

/** Every Base Language the content must be written in. */
val BASE_LANGUAGES = listOf("en")

/** Where the identifiers that have shipped are recorded, at the repository root. */
const val RELEASED_IDS_FILE = "released-ids.json"

/**
 * Every identifier Learner progress can refer to. Progress names content by id alone, so an id
 * that has shipped must stay, and the record of those that have is kept in [RELEASED_IDS_FILE].
 */
@Serializable
data class ReleasedIds(
    val levels: List<String> = emptyList(),
    val units: List<String> = emptyList(),
    val dailySets: List<String> = emptyList(),
    val exercises: List<String> = emptyList(),
    val items: List<String> = emptyList(),
) {
    /** This record with every identifier of [current] added. Nothing is ever removed. */
    fun with(current: ReleasedIds): ReleasedIds = ReleasedIds(
        levels = (levels + current.levels).distinct().sorted(),
        units = (units + current.units).distinct().sorted(),
        dailySets = (dailySets + current.dailySets).distinct().sorted(),
        exercises = (exercises + current.exercises).distinct().sorted(),
        items = (items + current.items).distinct().sorted(),
    )
}

/** The identifiers the content has now, sorted. A file that cannot be read contributes none. */
fun identifiersOf(files: ContentFiles): ReleasedIds =
    ReleasedIds().with(identified(readContent(files::read)).toReleasedIds())

/**
 * Every problem in the Authored Content: files that cannot be read, what the course engine could
 * not run, and what is not good enough to ship. It never stops at the first. With [released],
 * also every identifier that shipped and is gone, and every identifier that is not yet recorded.
 */
fun checkContent(files: ContentFiles, released: ReleasedIds? = null): List<ContentProblem> {
    val content = readContent(files::read)
    // Without the course file nothing else is known to be content.
    if (content.unreadable.any { it.file == COURSE_FILE }) return content.unreadable
    return content.unreadable +
        loadingProblems(content) +
        authoringProblems(content, files.paths()) +
        released?.let { releasedProblems(content, it) }.orEmpty()
}

/** What makes content not good enough to ship, though the course engine could run it. */
private fun authoringProblems(content: ReadContent, paths: Set<String>): List<ContentProblem> {
    val course = content.course()
    // Rules that count across files would blame content that is fine when a file is missing from the count.
    val complete = content.unreadable.isEmpty()
    val problems = mutableListOf<ContentProblem>()
    fun report(file: String, id: String?, message: String) {
        problems += ContentProblem(file, id, message)
    }

    /** Reports each Base Language in which [text] is missing or blank. */
    fun needsText(file: String, id: String, what: String, text: Localized?) {
        BASE_LANGUAGES.filter { text?.get(it).isNullOrBlank() }.forEach { language ->
            report(file, id, "$what has no text in the Base Language \"$language\"")
        }
    }

    // Levels and Units
    problems += repeatedIds("Level", content.levels.map { Filed(COURSE_FILE, it.id) })
    problems += repeatedIds("Unit", content.levels.flatMap { it.units }.map { Filed(COURSE_FILE, it.id) })
    val dailySetsByFile = content.dailySets.associateBy { it.file }
    content.levels.forEach { level ->
        needsText(COURSE_FILE, level.id, "The title of Level ${level.id}", level.title)
        if (level.units.isEmpty()) report(COURSE_FILE, level.id, "Level ${level.id} has no Units")
        level.units.forEach { unit ->
            needsText(COURSE_FILE, unit.id, "The title of Unit ${unit.id}", unit.title)
            if (unit.dailySets.isEmpty() && !unit.coming) report(COURSE_FILE, unit.id, "Unit ${unit.id} has no Daily Sets")
            if (unit.dailySets.isNotEmpty() && unit.coming) {
                report(COURSE_FILE, unit.id, "Unit ${unit.id} is marked as coming but has Daily Sets")
            }
            if (unit.number != null && unit.polishTitle.isNullOrBlank()) {
                report(COURSE_FILE, unit.id, "Unit ${unit.id} is a chapter of the book and has no Polish title")
            }
            if (!unit.id.startsWith("${level.id}-")) {
                report(COURSE_FILE, unit.id, "Unit id ${unit.id} does not start with ${level.id}-, the id of its Level")
            }
            unit.dailySets.distinct().mapNotNull(dailySetsByFile::get).forEach { (file, dailySet) ->
                if (!dailySet.id.startsWith("${unit.id}-")) {
                    report(file, dailySet.id, "Daily Set id ${dailySet.id} does not start with ${unit.id}-, the id of its Unit")
                }
            }
        }
    }

    // Daily Sets, with their Tips and Exercises
    problems += repeatedIds("Daily Set", content.dailySets.map { Filed(it.file, it.value.id) })
    problems += repeatedIds("Tip", content.dailySets.flatMap { day -> day.value.tips.map { Filed(day.file, it.id) } })
    content.dailySets.forEach { (file, dailySet) ->
        val size = dailySet.exercises.size
        if (size !in DAILY_SET_EXERCISES) {
            report(
                file,
                dailySet.id,
                "Daily Set ${dailySet.id} has $size Exercises; " +
                    "it needs ${DAILY_SET_EXERCISES.first} to ${DAILY_SET_EXERCISES.last}",
            )
        }
        val introduced = course.introducedIn(dailySet).size
        if (introduced > MAX_NEW_ITEMS) {
            report(file, dailySet.id, "Daily Set ${dailySet.id} introduces $introduced new Items; at most $MAX_NEW_ITEMS are allowed")
        }
        dailySet.section?.let { section ->
            if (section.heading.isBlank()) report(file, dailySet.id, "The section of Daily Set ${dailySet.id} has no heading")
            needsText(file, dailySet.id, "The meaning of the section heading of Daily Set ${dailySet.id}", section.meaning)
        }
        if (dailySet.tips.isEmpty()) {
            report(file, dailySet.id, "Daily Set ${dailySet.id} has no Tip; the title of its first Tip is its title on the Path")
        }
        dailySet.tips.forEach { tip ->
            val name = "Tip ${tip.id}"
            if (!tip.id.startsWith("${dailySet.id}-")) {
                report(file, tip.id, "Tip id ${tip.id} does not start with ${dailySet.id}-, the id of its Daily Set")
            }
            needsText(file, tip.id, "The title of $name", tip.title)
            needsText(file, tip.id, "The body of $name", tip.body)
            tip.body.values.mapNotNull(::marksProblem).distinct().forEach { problem ->
                report(file, tip.id, "The body of $name $problem")
            }
            tip.conjugation?.let { needsText(file, tip.id, "The meaning in the conjugation table of $name", it.meaning) }
            tip.table?.let { table ->
                needsText(file, tip.id, "The title of the grammar table of $name", table.title)
                if (table.columns.any { it.header == null && it.gender == null }) {
                    report(file, tip.id, "A column of the grammar table of $name has neither a header nor a gender")
                }
                if (table.rows.any { it.case == null && it.header == null } && table.rows.any { it.case != null || it.header != null }) {
                    report(file, tip.id, "Some rows of the grammar table of $name have a head and some do not")
                }
            }
            if (tip.kind != TipKind.NOTE && tip.table != null) {
                report(file, tip.id, "$name is an ${tip.kind.name.lowercase(Locale.ROOT)} callout with a table; a callout is a short warning or rule, its table goes in a Tip of its own")
            }
        }
        dailySet.exercises.forEach { exercise ->
            val name = "Exercise ${exercise.id}"
            if (!exercise.id.startsWith("${dailySet.id}-")) {
                report(file, exercise.id, "Exercise id ${exercise.id} does not start with ${dailySet.id}-, the id of its Daily Set")
            }
            if (exercise.items.isEmpty()) report(file, exercise.id, "$name lists no Item, so Review can never bring it back")
            when (exercise) {
                is ChooseTranslation -> {
                    if (exercise.options.size < 2) {
                        report(file, exercise.id, "$name has fewer than 2 options, which leaves nothing to choose")
                    }
                    exercise.options.forEachIndexed { index, option ->
                        needsText(file, exercise.id, "Option ${index + 1} of $name", option)
                    }
                    BASE_LANGUAGES.forEach { language ->
                        exercise.options.mapNotNull { it[language] }.filter { it.isNotBlank() }
                            .map { it.trim().lowercase(Locale.ROOT) }.repeated()
                            .forEach { same ->
                                report(file, exercise.id, "$name has more than one option that would read the same: $same")
                            }
                    }
                }
                is TypeAnswer -> needsText(file, exercise.id, "The prompt of $name", exercise.prompt)
                is WordTiles -> needsText(file, exercise.id, "The prompt of $name", exercise.prompt)
                is Conjugation -> {
                    needsText(file, exercise.id, "The meaning of the verb of $name", exercise.meaning)
                    if (exercise.sentence != null && exercise.sentenceMeaning != null) {
                        needsText(file, exercise.id, "The sentence meaning of $name", exercise.sentenceMeaning)
                    }
                }
                is ListenChoose -> needsText(file, exercise.id, "The meaning of $name", exercise.meaning)
                is ListenType -> needsText(file, exercise.id, "The meaning of $name", exercise.meaning)
                is FillGaps -> needsText(file, exercise.id, "The meaning of $name", exercise.meaning)
                is MatchPairs -> {
                    if (exercise.pairs.size !in MATCH_PAIRS) {
                        report(file, exercise.id, "$name has ${exercise.pairs.size} pairs; it must have ${MATCH_PAIRS.first} to ${MATCH_PAIRS.last}")
                    }
                    exercise.pairs.forEachIndexed { index, pair -> needsText(file, exercise.id, "The meaning of pair ${index + 1} of $name", pair.meaning) }
                    BASE_LANGUAGES.forEach { language ->
                        exercise.pairs.mapNotNull { it.meaning[language] }.filter { it.isNotBlank() }
                            .map { it.trim().lowercase(Locale.ROOT) }.repeated()
                            .forEach { same -> report(file, exercise.id, "$name has more than one meaning that would read the same: $same") }
                    }
                }
                is TrueFalse -> {
                    needsText(file, exercise.id, "The meaning of $name", exercise.meaning)
                    if (exercise.statements.size !in TRUE_FALSE_STATEMENTS) {
                        report(file, exercise.id, "$name has ${exercise.statements.size} statements; it must have ${TRUE_FALSE_STATEMENTS.first} to ${TRUE_FALSE_STATEMENTS.last}")
                    } else if (exercise.statements.all { it.`true` } || exercise.statements.none { it.`true` }) {
                        report(file, exercise.id, "$name has every statement ${if (exercise.statements.first().`true`) "true" else "false"}; give both kinds")
                    }
                    exercise.statements.forEachIndexed { index, statement ->
                        needsText(file, exercise.id, "The meaning of statement ${index + 1} of $name", statement.meaning)
                    }
                }
                is OrderLines -> {
                    needsText(file, exercise.id, "The meaning of $name", exercise.meaning)
                    if (exercise.lines.size !in ORDER_LINES) {
                        report(file, exercise.id, "$name has ${exercise.lines.size} lines; it must have ${ORDER_LINES.first} to ${ORDER_LINES.last}")
                    }
                }
            }
            // A listening Exercise with no Clip cannot run at all, which the loading rules report.
            if (content.clipsComplete && exercise !is ListenChoose && exercise !is ListenType && !(exercise is TrueFalse && exercise.listen)) {
                spoken(exercise).filter { it.isNotBlank() && course.clipFor(it) == null }.forEach { text ->
                    report(file, exercise.id, "$name has no Clip for \"$text\", so its speaker icon would be missing")
                }
            }
        }
    }

    // Instructions
    content.instructionsFile?.takeIf { file -> file !in content.unreadable.map { it.file } }?.let { file ->
        InstructionKind.entries.filter { it !in content.instructions }.forEach { kind ->
            report(file, kind.name.lowercase(Locale.ROOT), "No instruction is given for ${kind.name.lowercase(Locale.ROOT)}")
        }
        content.instructions.forEach { (kind, instruction) ->
            val id = kind.name.lowercase(Locale.ROOT)
            if (instruction.polish.isBlank()) report(file, id, "The instruction for $id has no Polish")
            needsText(file, id, "The instruction for $id", instruction.meaning)
        }
    }

    // Items
    val exercisesOfItem = course.path.flatMap { it.exercises }
        .flatMap { exercise -> exercise.items.map { it to exercise.id } }
        .groupBy({ it.first }, { it.second })
        .mapValues { it.value.distinct().size }
    content.items.distinctBy { it.value.id }.forEach { (file, item) ->
        needsText(file, item.id, "The meaning of Item ${item.id}", item.meaning)
        val practised = exercisesOfItem[item.id] ?: 0
        if (complete && practised < MIN_EXERCISES_PER_ITEM) {
            val count = when (practised) {
                0 -> "no Exercise"
                1 -> "only 1 Exercise"
                else -> "only $practised Exercises"
            }
            report(file, item.id, "Item ${item.id} is practised by $count; it needs at least $MIN_EXERCISES_PER_ITEM")
        }
    }

    // Clips and their files
    val manifest = content.clipsFile
    if (manifest != null && content.clipsComplete) {
        val dir = content.clipDir
        fun fileOf(clip: Clip) = if (dir.isEmpty()) "${clip.id}.ogg" else "$dir/${clip.id}.ogg"
        val spokenKeys = course.spokenTexts().map(::audioKey).toSet()
        content.clips.distinctBy { it.value.id }.forEach { (file, clip) ->
            if (fileOf(clip) !in paths) report(file, clip.id, "Clip ${clip.id} has no audio file ${fileOf(clip)}")
            if (complete && clip.text.isNotBlank() && audioKey(clip.text) !in spokenKeys) {
                report(file, clip.id, "Clip ${clip.id} says \"${clip.text}\", which nothing in the course speaks")
            }
        }
        val expected = content.clips.map { fileOf(it.value) }.toSet() + content.listed + COURSE_FILE
        paths.filter { it.substringBeforeLast('/', "") == dir && it !in expected }.sorted().forEach { stray ->
            report(manifest, null, "$stray is beside the Clip manifest but is neither the manifest nor a Clip's file")
        }
    }

    // Pictures and their files
    val pictureList = content.picturesFile
    if (pictureList != null && pictureList !in content.unreadable.map { it.file }) {
        val dir = content.pictureDir
        fun fileOf(image: String) = if (dir.isEmpty()) image else "$dir/$image"
        val images = content.pictures.mapNotNull { (_, entry) -> entry.image?.let { entry.item to it } }
        images.filter { fileOf(it.second) !in paths }.forEach { (item, image) ->
            report(pictureList, item, "The picture of Item $item has no image file ${fileOf(image)}")
        }
        val expected = images.map { fileOf(it.second) }.toSet() + content.listed + COURSE_FILE
        paths.filter { it.substringBeforeLast('/', "") == dir && it !in expected }.sorted().forEach { stray ->
            report(pictureList, null, "$stray is beside the list of pictures but is neither the list nor a picture's file")
        }
        // Two words of one Unit with the same picture could not be told apart in a Game.
        val unitOf = course.levels.flatMap { it.units }.flatMap { unit ->
            unit.dailySets.flatMap { course.introducedIn(it) }.map { it.id to unit.id }
        }.toMap()
        content.pictures.map { it.value }.filter { it.item in unitOf }
            .groupBy { unitOf.getValue(it.item) to (it.image ?: "glyph ${it.glyph}") }
            .filterValues { it.size > 1 }
            .forEach { (shared, entries) ->
                report(
                    pictureList,
                    entries[1].item,
                    "Items ${entries.joinToString { it.item }} of Unit ${shared.first} have the same picture, ${shared.second}",
                )
            }
    }

    // Files nothing loads
    paths.filter { it.endsWith(".json") && it != COURSE_FILE && it !in content.listed }.sorted().forEach { path ->
        report(path, null, "$COURSE_FILE does not list this file, so nothing in it is part of the course")
    }

    // The class recordings (ADR-0014)
    paths.filter { it.endsWith(".mp3", ignoreCase = true) }.sorted().forEach { path ->
        report(path, null, "An MP3 is in the content. The class recordings are imported on the phone and never packaged (ADR-0014)")
    }
    content.tracksFile?.let { file ->
        val segmentsFile = content.segmentsFile
        content.tracks.filter { segmentsFile != null && content.segments[it.number.toString()].isNullOrEmpty() }.forEach { track ->
            report(segmentsFile.orEmpty(), track.number.toString(), "Class recording ${track.number} has no lines for step mode")
        }
        content.tracks.zipWithNext().filter { (a, b) -> b.number <= a.number }.forEach { (_, b) ->
            report(file, b.number.toString(), "Class recording ${b.number} is listed out of order")
        }
        val chapters = content.levels.flatMap { it.units }.mapNotNull { it.number }.toSet()
        content.tracks.filter { chapters.isNotEmpty() && it.lesson !in chapters }.forEach { track ->
            report(file, track.number.toString(), "Class recording ${track.number} is of chapter ${track.lesson}, which the course does not have")
        }
    }
    return problems
}

/** One kind of identifier Learner progress can refer to, with the file each id is written in. */
private class Identified(val kind: String, val files: Map<String, String>)

private fun identified(content: ReadContent): List<Identified> {
    fun kind(name: String, ids: List<Filed<String>>) = Identified(name, ids.reversed().associate { it.value to it.file })
    return listOf(
        kind("Level", content.levels.map { Filed(COURSE_FILE, it.id) }),
        kind("Unit", content.levels.flatMap { it.units }.map { Filed(COURSE_FILE, it.id) }),
        kind("Daily Set", content.dailySets.map { Filed(it.file, it.value.id) }),
        kind("Exercise", content.dailySets.flatMap { day -> day.value.exercises.map { Filed(day.file, it.id) } }),
        kind("Item", content.items.map { Filed(it.file, it.value.id) }),
    )
}

private fun List<Identified>.toReleasedIds(): ReleasedIds {
    val (levels, units, dailySets, exercises, items) = map { it.files.keys.toList() }
    return ReleasedIds(levels, units, dailySets, exercises, items)
}

/** Identifiers that shipped and are gone, and identifiers that are not recorded yet. */
private fun releasedProblems(content: ReadContent, released: ReleasedIds): List<ContentProblem> {
    val recorded = listOf(released.levels, released.units, released.dailySets, released.exercises, released.items)
    return identified(content).zip(recorded).flatMap { (now, before) ->
        // While a file cannot be read, its identifiers only look gone.
        val gone = if (content.unreadable.isEmpty()) before.filter { it !in now.files } else emptyList()
        gone.map { id ->
            ContentProblem(
                RELEASED_IDS_FILE,
                id,
                "This ${now.kind} shipped before and is no longer in the content. Put it back, or if it really " +
                    "must go, delete its line from $RELEASED_IDS_FILE in the same change",
            )
        } + now.files.filterKeys { it !in before }.map { (id, file) ->
            ContentProblem(
                file,
                id,
                "This ${now.kind} is not recorded in $RELEASED_IDS_FILE. " +
                    "Run ./gradlew :engine:recordContentIds and commit the file",
            )
        }
    }
}
