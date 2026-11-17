package com.polisheasy.engine

import java.util.Locale
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class CourseFile(
    val levels: List<LevelEntry>,
    val items: List<String> = emptyList(),
    /** Optional: the path of the file that lists the Clips. */
    val audio: String? = null,
    /** Optional: the path of the file that lists the pictures. */
    val pictures: String? = null,
)

@Serializable
internal data class ClipsFile(val clips: List<Clip>)

@Serializable
internal data class ItemsFile(val items: List<Item>)

@Serializable
internal data class LevelEntry(val id: String, val title: Localized, val units: List<UnitEntry>)

@Serializable
internal data class UnitEntry(val id: String, val title: Localized, val dailySets: List<String>)

internal val contentJson = Json { classDiscriminator = "type" }

/** The file the course starts at. */
internal const val COURSE_FILE = "course.json"

/**
 * Loads the Authored Content. [readFile] returns the text of a content file given its
 * path relative to the content root; the course starts at `course.json`, which lists
 * the path of every Daily Set file and of every Item file, and of the file that lists the Clips.
 *
 * Throws an [IllegalArgumentException] that names everything the course engine could not run,
 * one line each. What makes content good enough to ship is judged by [checkContent], not here.
 */
fun loadCourse(readFile: (path: String) -> String): Course {
    val content = readContent(readFile)
    val problems = content.unreadable + loadingProblems(content)
    require(problems.isEmpty()) {
        // A rule's sentence already names what it is about. Only a file that could not be read needs its path.
        problems.joinToString("\n") { if (it.id == null) it.toString() else it.message }
    }
    return content.course()
}

/** Something read from the content, with the file it was written in. */
internal data class Filed<T>(val file: String, val value: T)

/** Everything `course.json` lists that could be read, before any rule has judged it. */
internal class ReadContent(
    /** The Levels and Units as `course.json` writes them. */
    val levels: List<LevelEntry>,
    /** Every listed Daily Set file that could be read, once each, in Path order. */
    val dailySets: List<Filed<DailySet>>,
    val items: List<Filed<Item>>,
    val clips: List<Filed<Clip>>,
    /** The file that lists the Clips, if the course has one. */
    val clipsFile: String?,
    /** The Item files `course.json` lists. */
    val itemFiles: List<String>,
    /** One problem for each listed file that is missing or does not decode. */
    val unreadable: List<ContentProblem>,
    val pictures: List<Filed<PictureEntry>> = emptyList(),
    /** The file that lists the pictures, if the course has one. */
    val picturesFile: String? = null,
) {
    /** Where the pictures' files are: the directory of the file that lists them. */
    val pictureDir: String = picturesFile?.substringBeforeLast('/', "").orEmpty()

    val clipDir: String = clipsFile?.substringBeforeLast('/', "") ?: "audio"

    /** Every path `course.json` names. */
    val listed: Set<String> = (dailySetListings() + itemFiles + listOfNotNull(clipsFile, picturesFile)).toSet()

    private val unreadableFiles = unreadable.map { it.file }.toSet()

    /** False while an Item file cannot be read, when an Item that looks undefined may only be unread. */
    val itemsComplete: Boolean = itemFiles.none { it in unreadableFiles }

    /** False while the Clip manifest cannot be read, when a text that looks unspoken may only be unread. */
    val clipsComplete: Boolean = clipsFile !in unreadableFiles

    private val built by lazy {
        val byFile = dailySets.associate { it.file to it.value }
        val placed = mutableSetOf<String>()
        Course(
            levels.map { level ->
                Level(
                    level.id,
                    level.title,
                    level.units.map { unit ->
                        CourseUnit(unit.id, unit.title, unit.dailySets.filter(placed::add).mapNotNull(byFile::get))
                    },
                )
            },
            items.map { it.value },
            clips.map { it.value },
            clipDir,
            // The first picture of an Item is the one used; a second is reported, not loaded.
            pictures.reversed().mapNotNull { (_, entry) -> pictureOf(entry, pictureDir)?.let { entry.item to it } }.toMap(),
        )
    }

    /** The course these files make. A Daily Set file that could not be read is left out of the Path. */
    fun course(): Course = built

    /** The path of every Daily Set file, as often as `course.json` lists it. */
    fun dailySetListings(): List<String> = levels.flatMap { level -> level.units.flatMap { it.dailySets } }
}

/**
 * Reads `course.json` and every file it lists. A file that is missing or does not decode becomes
 * a problem and the rest are still read; if `course.json` itself cannot be read, that is the only problem.
 */
internal fun readContent(readFile: (path: String) -> String): ReadContent {
    val unreadable = mutableListOf<ContentProblem>()
    fun <T> read(path: String, decode: (String) -> T): T? = try {
        decode(readFile(path))
    } catch (failure: Exception) {
        // Decoding failures quote the JSON on further lines. One line per problem is enough to find it.
        val reason = failure.message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() } ?: failure.toString()
        unreadable += ContentProblem(path, null, "Cannot be read: $reason")
        null
    }

    val index = read(COURSE_FILE) { contentJson.decodeFromString<CourseFile>(it) }
        ?: return ReadContent(emptyList(), emptyList(), emptyList(), emptyList(), null, emptyList(), unreadable)
    val dailySetPaths = index.levels.flatMap { level -> level.units.flatMap { it.dailySets } }
    return ReadContent(
        levels = index.levels,
        dailySets = dailySetPaths.distinct().mapNotNull { path ->
            read(path) { contentJson.decodeFromString<DailySet>(it) }?.let { Filed(path, it) }
        },
        items = index.items.distinct().flatMap { path ->
            read(path) { contentJson.decodeFromString<ItemsFile>(it) }?.items.orEmpty().map { Filed(path, it) }
        },
        clips = index.audio?.let { path ->
            read(path) { contentJson.decodeFromString<ClipsFile>(it) }?.clips.orEmpty().map { Filed(path, it) }
        }.orEmpty(),
        clipsFile = index.audio,
        itemFiles = index.items,
        pictures = index.pictures?.let { path ->
            read(path) { contentJson.decodeFromString<PicturesFile>(it) }?.pictures.orEmpty().map { Filed(path, it) }
        }.orEmpty(),
        picturesFile = index.pictures,
        unreadable = unreadable,
    )
}

private val clipId = Regex("[a-z0-9]+(-[a-z0-9]+)*")

/**
 * Everything in [content] that the course engine could not run: what Review relies on in Items
 * and Exercise ids, Clips that could not be played, and Tips, Accepted Answers or conjugations
 * the engine would trip over.
 */
internal fun loadingProblems(content: ReadContent): List<ContentProblem> {
    val course = content.course()
    val exercises = content.dailySets.flatMap { dailySet ->
        dailySet.value.exercises.map { Filed(dailySet.file, dailySet.value to it) }
    }
    val problems = mutableListOf<ContentProblem>()

    val dailySetIds = content.dailySets.associate { it.file to it.value.id }
    content.dailySetListings().repeated().forEach { path ->
        problems += ContentProblem(path, dailySetIds[path], "This Daily Set file is listed more than once in $COURSE_FILE")
    }
    content.dailySets.forEach { problems += dailySetProblems(it.file, it.value) }

    problems += repeatedIds("Item", content.items.map { Filed(it.file, it.value.id) })
    content.items.filter { it.value.text.isBlank() }.forEach { item ->
        problems += ContentProblem(item.file, item.value.id, "Item ${item.value.id} has no text")
    }
    exercises.forEach { (file, placed) ->
        val (dailySet, exercise) = placed
        exercise.items.filter { content.itemsComplete && it !in course.itemsById }.forEach { item ->
            problems += ContentProblem(
                file,
                exercise.id,
                "Daily Set ${dailySet.id}: Exercise ${exercise.id} lists Item $item, which is not defined",
            )
        }
    }
    // Review refers to an Exercise by its id alone.
    problems += repeatedIds("Exercise in the course", exercises.map { Filed(it.file, it.value.second.id) })

    problems += repeatedIds("Clip", content.clips.map { Filed(it.file, it.value.id) })
    content.clips.forEach { (file, clip) ->
        // The id is the name of the Clip's file.
        if (!clipId.matches(clip.id)) {
            problems += ContentProblem(
                file,
                clip.id,
                "Clip id \"${clip.id}\" is not lower-case letters and digits, in groups joined by single hyphens",
            )
        }
        if (clip.text.isBlank()) problems += ContentProblem(file, clip.id, "Clip ${clip.id} has no text")
    }
    content.clips.groupBy { audioKey(it.value.text) }.values.filter { it.size > 1 }.forEach { same ->
        problems += ContentProblem(
            same[1].file,
            same[1].value.id,
            "More than one Clip says \"${same.first().value.text}\": ${same.joinToString { it.value.id }}",
        )
    }
    content.pictures.forEach { (file, entry) ->
        if (content.itemsComplete && entry.item !in course.itemsById) {
            problems += ContentProblem(file, entry.item, "A picture is given for Item ${entry.item}, which is not defined")
        }
        if ((entry.image == null) == (entry.glyph == null)) {
            problems += ContentProblem(file, entry.item, "The picture of Item ${entry.item} must have an image or a glyph, and not both")
        } else if ((entry.image ?: entry.glyph).isNullOrBlank()) {
            problems += ContentProblem(file, entry.item, "The picture of Item ${entry.item} is blank")
        }
    }
    content.pictures.map { it.value.item }.repeated().forEach { item ->
        problems += ContentProblem(content.picturesFile.orEmpty(), item, "Item $item has more than one picture")
    }
    exercises.forEach { (file, placed) ->
        val (dailySet, exercise) = placed
        if (content.clipsComplete && (exercise is ListenChoose || exercise is ListenType)) {
            spoken(exercise).filter { course.clipFor(it) == null }.forEach { text ->
                problems += ContentProblem(
                    file,
                    exercise.id,
                    "Daily Set ${dailySet.id}: Exercise ${exercise.id} has no Clip for \"$text\"",
                )
            }
        }
    }
    return problems
}

/** One problem for each id after the first that repeats an earlier one, naming the file of the first. */
internal fun repeatedIds(kind: String, ids: List<Filed<String>>): List<ContentProblem> {
    val first = mutableMapOf<String, String>()
    return ids.mapNotNull { (file, id) ->
        val earlier = first.putIfAbsent(id, file) ?: return@mapNotNull null
        ContentProblem(file, id, "More than one $kind has the id $id. The first is in $earlier")
    }
}

internal fun List<String>.repeated(): Set<String> = groupingBy { it }.eachCount().filterValues { it > 1 }.keys

internal const val MAX_TIPS = 2

/** What the course engine could not run in one Daily Set: its Tips first, then at most one problem for each Exercise. */
private fun dailySetProblems(file: String, dailySet: DailySet): List<ContentProblem> {
    val problems = mutableListOf<ContentProblem>()
    val exerciseIds = dailySet.exercises.map { it.id }.toSet()
    val placements = dailySet.tips.map { it.before }
    if (dailySet.tips.size > MAX_TIPS) {
        problems += ContentProblem(
            file,
            dailySet.id,
            "Daily Set ${dailySet.id} has ${dailySet.tips.size} Tips; at most $MAX_TIPS are allowed",
        )
    }
    dailySet.tips.filter { it.before !in exerciseIds }.forEach { tip ->
        problems += ContentProblem(
            file,
            tip.id,
            "Daily Set ${dailySet.id}: Tip ${tip.id} is placed before ${tip.before}, which is not one of its Exercises",
        )
    }
    if (placements.size != placements.toSet().size) {
        problems += ContentProblem(
            file,
            dailySet.id,
            "Daily Set ${dailySet.id} has more than one Tip before the same Exercise",
        )
    }
    dailySet.exercises.forEach { exercise ->
        exerciseProblem(exercise)?.let { problem ->
            problems += ContentProblem(file, exercise.id, "Daily Set ${dailySet.id}: Exercise ${exercise.id} $problem")
        }
    }
    dailySet.tips.forEach { tip ->
        tip.conjugation?.let(::tableProblem)?.let { problem ->
            problems += ContentProblem(file, tip.id, "Daily Set ${dailySet.id}: Tip ${tip.id} $problem")
        }
    }
    return problems
}

/** The first thing about [exercise] that the course engine could not run, as the end of a sentence, or null. */
private fun exerciseProblem(exercise: Exercise): String? = when (exercise) {
    is ChooseTranslation -> when {
        // The engine reads the option at this position to give feedback.
        exercise.correctOption !in exercise.options.indices ->
            "has correctOption ${exercise.correctOption}, which is not one of its ${exercise.options.size} options"
        else -> null
    }
    is TypeAnswer -> typedAnswersProblem(exercise.acceptedAnswers)
    is WordTiles -> when {
        exercise.acceptedAnswers.isEmpty() -> "has no Accepted Answers"
        exercise.acceptedAnswers.any { words(it).isEmpty() } -> "has a blank Accepted Answer"
        words(exercise.acceptedAnswers.first()).size < 2 ->
            "has a first Accepted Answer of fewer than two words, which leaves nothing to build"
        else -> {
            val tiles = tilesOf(exercise).wordCounts()
            exercise.distractors.firstOrNull { words(it).size != 1 }
                ?.let { "has the distractor \"$it\", which is not one word" }
                ?: exercise.acceptedAnswers
                    .firstOrNull { accepted ->
                        words(accepted).wordCounts().any { (word, count) -> count > (tiles[word] ?: 0) }
                    }
                    ?.let { "has the Accepted Answer \"$it\", which cannot be built from its tiles" }
        }
    }
    is Conjugation -> typedAnswersProblem(exercise.acceptedAnswers) ?: when {
        exercise.verb.isBlank() -> "has a blank verb"
        (exercise.sentence == null) != (exercise.sentenceMeaning == null) ->
            "must have both a sentence and a sentence meaning, or neither"
        else -> exercise.sentence?.let { sentence ->
            val gap = gapOf(sentence)
            when {
                gap == null -> "has the sentence \"$sentence\", which does not have exactly one gap written as $GAP"
                words(gap.first + " " + gap.second).isEmpty() ->
                    "has the sentence \"$sentence\", which has no words apart from the gap"
                else -> null
            }
        }
    }
    is ListenChoose -> {
        // Options that differ only by an accent are a real listening test, so they are allowed.
        val same = (listOf(exercise.text) + exercise.distractors).map(::normalizeAnswer).repeated()
        when {
            normalizeAnswer(exercise.text).isEmpty() -> "has blank text"
            exercise.distractors.isEmpty() -> "has no distractors"
            exercise.distractors.any { normalizeAnswer(it).isEmpty() } -> "has a blank distractor"
            same.isNotEmpty() -> "has more than one option that reads: ${same.joinToString()}"
            else -> null
        }
    }
    is ListenType -> typedAnswersProblem(exercise.acceptedAnswers)
        ?: if (exercise.readingAnswers.any { normalizeAnswer(it).isEmpty() }) "has a blank reading answer" else null
}

private fun typedAnswersProblem(acceptedAnswers: List<String>): String? = when {
    acceptedAnswers.isEmpty() -> "has no Accepted Answers"
    acceptedAnswers.any { normalizeAnswer(it).isEmpty() } -> "has a blank Accepted Answer"
    else -> null
}

/** The first thing wrong with a Tip's conjugation [table], as the end of a sentence, or null. */
private fun tableProblem(table: ConjugationTable): String? =
    if (table.verb.isBlank()) {
        "has a conjugation table with a blank verb"
    } else {
        Person.entries.firstNotNullOfOrNull { person ->
            when {
                person !in table.forms -> "has a conjugation table with no form for ${person.pronouns}"
                table.forms.getValue(person).isBlank() ->
                    "has a conjugation table with a blank form for ${person.pronouns}"
                else -> null
            }
        }
    }

/** How many times each word is in this list, letter case ignored. */
private fun List<String>.wordCounts(): Map<String, Int> = groupingBy { it.lowercase(Locale.ROOT) }.eachCount()
