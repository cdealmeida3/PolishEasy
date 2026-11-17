package com.polisheasy.engine

import java.util.Locale
import kotlinx.serialization.SerialName
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
    /** Optional: the path of the file with what the Games of Practice need beyond the Items. */
    val practice: String? = null,
    /** Optional: the path of the file with the Polish instruction of each kind of Exercise. */
    val instructions: String? = null,
    /** Optional: the path of the file that lists the book's class recordings. */
    val tracks: String? = null,
    /** Optional: the path of the file with each class recording's lines, for step mode. */
    val segments: String? = null,
    /** Optional: the path of the file with the listening tasks on the class recordings. */
    val listening: String? = null,
    /** The paths of the conversations of the Chat Game, one file each. */
    val chats: List<String> = emptyList(),
    /** Optional: the path of the file with the forms and people of the Ankieta Game. */
    val ankieta: String? = null,
)

/** The Polish instruction of each kind of Exercise, as the book words it. */
@Serializable
internal data class InstructionsFile(val instructions: Map<InstructionKind, Instruction>)

/** What the Games of Practice need to know that no Item says. */
@Serializable
internal data class PracticeFile(
    /** Real Polish words that a wrong option of Sound gaps would spell: a gap that would offer one is never asked. */
    val realWords: List<String> = emptyList(),
    /** The gender of each noun Item that Mój, moja, moje asks: its id, and "m", "f" or "n". */
    val genders: Map<String, String> = emptyMap(),
    /** The Item that says "my" with a noun of each gender: "m", "f" or "n", and its id. */
    val my: Map<String, String> = emptyMap(),
    /** The Item that says "this" with a noun of each gender: "m", "f" or "n", and its id. */
    @SerialName("this")
    val demonstratives: Map<String, String> = emptyMap(),
    /** What each small letter is called when a word is spelled aloud: the letter, and its name. */
    val letters: Map<String, String> = emptyMap(),
)

@Serializable
internal data class ClipsFile(val clips: List<Clip>)

@Serializable
internal data class ItemsFile(val items: List<Item>)

@Serializable
internal data class LevelEntry(val id: String, val title: Localized, val units: List<UnitEntry>)

/**
 * A Unit as `course.json` writes it. A chapter of the book also has its [number], the book's
 * [polishTitle], an [accent] colour, its [strands] from the contents, its [lookUp] Items, and is
 * [coming] while it has no Daily Sets yet.
 */
@Serializable
internal data class UnitEntry(
    val id: String,
    val title: Localized,
    val dailySets: List<String> = emptyList(),
    val number: Int? = null,
    val polishTitle: String? = null,
    val accent: Int = 0,
    val strands: Map<Strand, List<String>> = emptyMap(),
    val lookUp: List<String> = emptyList(),
    val coming: Boolean = false,
)

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
    val practice: PracticeFile? = null,
    /** The file with what the Games of Practice need, if the course has one. */
    val practiceFile: String? = null,
    val instructions: Map<InstructionKind, Instruction> = emptyMap(),
    /** The file with the instructions, if the course has one. */
    val instructionsFile: String? = null,
    val tracks: List<ClassTrack> = emptyList(),
    val tracksFile: String? = null,
    /** Each class recording's lines as the segments file writes them, by track number as text. */
    val segments: Map<String, List<List<Long>>> = emptyMap(),
    val segmentsFile: String? = null,
    val listening: List<ListeningTask> = emptyList(),
    val listeningFile: String? = null,
    /** The conversations of the Chat Game, each with its file. */
    val chats: List<Filed<ChatScript>> = emptyList(),
    val chatFiles: List<String> = emptyList(),
    val ankieta: AnkietaFile = AnkietaFile(),
    val ankietaFile: String? = null,
) {
    /** Where the pictures' files are: the directory of the file that lists them. */
    val pictureDir: String = picturesFile?.substringBeforeLast('/', "").orEmpty()

    val clipDir: String = clipsFile?.substringBeforeLast('/', "") ?: "audio"

    /** Every path `course.json` names. */
    val listed: Set<String> =
        (dailySetListings() + itemFiles + listOfNotNull(clipsFile, picturesFile, practiceFile, instructionsFile, tracksFile, segmentsFile, listeningFile, ankietaFile) + chatFiles).toSet()

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
                        CourseUnit(
                            unit.id,
                            unit.title,
                            unit.dailySets.filter(placed::add).mapNotNull(byFile::get),
                            number = unit.number,
                            polishTitle = unit.polishTitle,
                            accent = unit.accent,
                            strands = unit.strands,
                            lookUp = unit.lookUp,
                            coming = unit.coming,
                        )
                    },
                )
            },
            items.map { it.value },
            clips.map { it.value },
            clipDir,
            // The first picture of an Item is the one used; a second is reported, not loaded.
            pictures.reversed().mapNotNull { (_, entry) -> pictureOf(entry, pictureDir)?.let { entry.item to it } }.toMap(),
            practice?.realWords.orEmpty().map { it.lowercase(Locale.ROOT) }.toSet(),
            practice?.genders.orEmpty().mapNotNull { (item, gender) -> Gender.of(gender)?.let { item to it } }.toMap(),
            practice?.my.orEmpty().mapNotNull { (gender, item) -> Gender.of(gender)?.let { it to item } }.toMap(),
            instructions,
            tracks = tracks,
            trackLines = segments.mapNotNull { (number, lines) ->
                number.toIntOrNull()?.let { it to lines.filter { line -> line.size == 2 }.map { (start, end) -> TrackLine(start, end) } }
            }.toMap(),
            listening = listening,
            chats = chats.map { it.value },
            ankieta = ankieta,
            letterNames = practice?.letters.orEmpty(),
            demonstratives = practice?.demonstratives.orEmpty().mapNotNull { (gender, item) -> Gender.of(gender)?.let { it to item } }.toMap(),
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
        practice = index.practice?.let { path -> read(path) { contentJson.decodeFromString<PracticeFile>(it) } },
        practiceFile = index.practice,
        instructions = index.instructions?.let { path ->
            read(path) { contentJson.decodeFromString<InstructionsFile>(it) }?.instructions
        }.orEmpty(),
        instructionsFile = index.instructions,
        tracks = index.tracks?.let { path -> read(path) { contentJson.decodeFromString<TracksFile>(it) }?.tracks }.orEmpty(),
        tracksFile = index.tracks,
        segments = index.segments?.let { path -> read(path) { contentJson.decodeFromString<SegmentsFile>(it) }?.lines }.orEmpty(),
        segmentsFile = index.segments,
        listening = index.listening?.let { path -> read(path) { contentJson.decodeFromString<ListeningFile>(it) }?.tasks }.orEmpty(),
        listeningFile = index.listening,
        chats = index.chats.distinct().mapNotNull { path -> read(path) { contentJson.decodeFromString<ChatScript>(it) }?.let { Filed(path, it) } },
        chatFiles = index.chats,
        ankieta = index.ankieta?.let { path -> read(path) { contentJson.decodeFromString<AnkietaFile>(it) } } ?: AnkietaFile(),
        ankietaFile = index.ankieta,
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
    content.items.filter { item -> item.value.forms.any { it.isBlank() } || item.value.spellings.distinct().size < item.value.spellings.size }.forEach { item ->
        problems += ContentProblem(item.file, item.value.id, "Item ${item.value.id} has a blank form, or a form that repeats its text or another form")
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
    content.levels.flatMap { it.units }.forEach { unit ->
        unit.lookUp.filter { content.itemsComplete && it !in course.itemsById }.forEach { item ->
            problems += ContentProblem(COURSE_FILE, unit.id, "Unit ${unit.id} looks up Item $item, which is not defined")
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
    content.practice?.realWords.orEmpty().let { words ->
        val file = content.practiceFile.orEmpty()
        words.filter { it.isBlank() || it.any(Char::isWhitespace) }.forEach { word ->
            problems += ContentProblem(file, null, "A real word of Sound gaps must be one word, and \"$word\" is not")
        }
        words.map { it.lowercase(Locale.ROOT) }.repeated().forEach { word ->
            problems += ContentProblem(file, null, "The real word \"$word\" is listed more than once")
        }
    }
    content.practice?.let { practice ->
        val file = content.practiceFile.orEmpty()
        val known = Gender.entries.joinToString(", ") { "\"${it.id}\"" }
        practice.genders.forEach { (item, gender) ->
            val defined = course.itemsById[item]
            if (content.itemsComplete && defined == null) {
                problems += ContentProblem(file, item, "A gender is given for Item $item, which is not defined")
            } else if (defined != null && defined.text.any(Char::isWhitespace)) {
                problems += ContentProblem(file, item, "A gender is given for Item $item, which is not one word: \"${defined.text}\"")
            }
            if (Gender.of(gender) == null) problems += ContentProblem(file, item, "The gender of Item $item is \"$gender\"; it must be one of $known")
        }
        practice.my.forEach { (gender, item) ->
            if (Gender.of(gender) == null) problems += ContentProblem(file, item, "\"$gender\" is not a gender to give a word for \"my\"; it must be one of $known")
            if (content.itemsComplete && item !in course.itemsById) {
                problems += ContentProblem(file, item, "The word for \"my\" is given as Item $item, which is not defined")
            }
        }
        practice.demonstratives.forEach { (gender, item) ->
            if (Gender.of(gender) == null) problems += ContentProblem(file, item, "\"$gender\" is not a gender to give a word for \"this\"; it must be one of $known")
            if (content.itemsComplete && item !in course.itemsById) {
                problems += ContentProblem(file, item, "The word for \"this\" is given as Item $item, which is not defined")
            }
        }
        practice.genders.values.toSet().filter { Gender.of(it) != null && it !in practice.my }.forEach { gender ->
            problems += ContentProblem(file, null, "Nouns are given the gender \"$gender\", and no word for \"my\" is given for it")
        }
        practice.letters.forEach { (letter, name) ->
            if (letter.length != 1 || !letter[0].isLetter() || letter != letter.lowercase(Locale.ROOT)) {
                problems += ContentProblem(file, null, "\"$letter\" is given a name to spell it by, and it is not one small letter")
            }
            if (name.isBlank()) problems += ContentProblem(file, null, "The letter \"$letter\" is given a blank name")
        }
    }
    problems += classAudioProblems(content)
    problems += chatProblems(content)
    problems += ankietaProblems(content)
    content.pictures.map { it.value.item }.repeated().forEach { item ->
        problems += ContentProblem(content.picturesFile.orEmpty(), item, "Item $item has more than one picture")
    }
    exercises.forEach { (file, placed) ->
        val (dailySet, exercise) = placed
        if (content.clipsComplete && (exercise is ListenChoose || exercise is ListenType || (exercise is TrueFalse && exercise.listen))) {
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
        val problem = tip.conjugation?.let(::tableProblem)
            ?: tip.table?.let(::grammarTableProblem)
            ?: if (tip.conjugation != null && tip.table != null) "has both a conjugation table and a grammar table; give one" else null
        problem?.let { problems += ContentProblem(file, tip.id, "Daily Set ${dailySet.id}: Tip ${tip.id} $it") }
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
    is FillGaps -> {
        val parts = gapParts(exercise.text)
        val gaps = (parts?.size ?: 1) - 1
        val notes = (exercise.gaps.mapNotNull { it.firstOrNull() } + exercise.distractors).map(::normalizeAnswer)
        when {
            parts == null -> "has the text \"${exercise.text}\", with a run of underscores that is not a gap written as $GAP"
            gaps !in 1..MAX_GAPS -> "has $gaps gaps; it must have 1 to $MAX_GAPS"
            exercise.gaps.size != gaps -> "has $gaps gaps in its text and answers for ${exercise.gaps.size}"
            words(parts.joinToString(" ")).isEmpty() -> "has no words apart from its gaps"
            else -> exercise.gaps.firstNotNullOfOrNull(::typedAnswersProblem)
                ?: when {
                    exercise.distractors.any { normalizeAnswer(it).isEmpty() } -> "has a blank distractor"
                    notes.repeated().isNotEmpty() -> "has more than one note that reads: ${notes.repeated().joinToString()}"
                    else -> null
                }
        }
    }
    is MatchPairs -> {
        val polish = exercise.pairs.map { normalizeAnswer(it.polish) }
        when {
            exercise.pairs.size < 2 -> "has fewer than two pairs, which leaves nothing to match"
            polish.any { it.isEmpty() } -> "has a pair with no Polish"
            polish.repeated().isNotEmpty() -> "has more than one pair that reads: ${polish.repeated().joinToString()}"
            else -> null
        }
    }
    is TrueFalse -> when {
        exercise.lines.isEmpty() || exercise.lines.any { normalizeAnswer(it).isEmpty() } -> "has no text, or a blank line"
        exercise.statements.isEmpty() -> "has no statements"
        exercise.statements.any { normalizeAnswer(it.text).isEmpty() } -> "has a blank statement"
        else -> null
    }
    is OrderLines -> {
        val lines = exercise.lines.map(::normalizeAnswer)
        when {
            lines.size < 2 -> "has fewer than two lines, which leaves nothing to order"
            lines.any { it.isEmpty() } -> "has a blank line"
            lines.repeated().isNotEmpty() -> "has more than one line that reads: ${lines.repeated().joinToString()}"
            else -> null
        }
    }
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

/** What the course engine could not run in the class recordings' list, their lines and their listening tasks. */
private fun classAudioProblems(content: ReadContent): List<ContentProblem> {
    val problems = mutableListOf<ContentProblem>()
    val tracksFile = content.tracksFile.orEmpty()
    val tracks = content.tracks.associateBy { it.number }
    problems += repeatedIds("class recording", content.tracks.map { Filed(tracksFile, it.number.toString()) })
    content.tracks.forEach { track ->
        val id = track.number.toString()
        if (track.number !in 1..999) problems += ContentProblem(tracksFile, id, "Class recording ${track.number} is not numbered 1 to 999")
        if (track.title.isBlank()) problems += ContentProblem(tracksFile, id, "Class recording ${track.number} has no title")
        if (track.lengthMs <= 0) problems += ContentProblem(tracksFile, id, "Class recording ${track.number} has no length")
    }
    val segmentsFile = content.segmentsFile.orEmpty()
    content.segments.forEach { (number, lines) ->
        val track = number.toIntOrNull()?.let(tracks::get)
        if (track == null) {
            problems += ContentProblem(segmentsFile, number, "Lines are given for class recording $number, which is not listed")
            return@forEach
        }
        val broken = lines.indexOfFirst { it.size != 2 || it[0] < 0 || it[1] <= it[0] || it[1] > track.lengthMs + 1_000 }
        val unordered = lines.zipWithNext().indexOfFirst { (a, b) -> a.size == 2 && b.size == 2 && b[0] < a[1] }
        when {
            broken >= 0 -> problems += ContentProblem(segmentsFile, number, "Line ${broken + 1} of class recording $number is not a start and an end within the recording")
            unordered >= 0 -> problems += ContentProblem(segmentsFile, number, "Line ${unordered + 2} of class recording $number starts before the line before it ends")
        }
    }
    val listeningFile = content.listeningFile.orEmpty()
    problems += repeatedIds("listening task", content.listening.map { Filed(listeningFile, it.id) })
    content.listening.forEach { task ->
        fun problem(message: String) {
            problems += ContentProblem(listeningFile, task.id, "Listening task ${task.id} $message")
        }
        if (content.tracks.isNotEmpty() && task.track !in tracks) problem("is about class recording ${task.track}, which is not listed")
        task.lines?.let { lines ->
            val count = content.segments[task.track.toString()]?.size ?: 0
            if (lines.size != 2 || lines[0] < 1 || lines[1] < lines[0] || lines[1] > count) {
                problem("gives the lines $lines, which are not a first and a last of the recording's $count lines")
            }
        }
        when (task) {
            is TrueFalseTask -> if (task.statement.isBlank()) problem("has no statement")
            is ChooseTask -> when {
                task.question.isBlank() -> problem("has no question")
                task.options.size < 2 -> problem("has fewer than two options")
                task.options.any { normalizeAnswer(it).isEmpty() } -> problem("has a blank option")
                task.correctOption !in task.options.indices ->
                    problem("has correctOption ${task.correctOption}, which is not one of its ${task.options.size} options")
                task.options.map(::normalizeAnswer).repeated().isNotEmpty() -> problem("has two options that read the same")
            }
            is DictationTask -> if (task.question.isBlank()) problem("has no question") else typedAnswersProblem(task.acceptedAnswers)?.let(::problem)
        }
    }
    return problems
}

/**
 * What the Chat Game could not play in the conversations: repeated ids, a turn led to that is not
 * there, a turn to reply to with no right reply, a reply that cannot be typed, an Item not defined.
 */
private fun chatProblems(content: ReadContent): List<ContentProblem> {
    val problems = mutableListOf<ContentProblem>()
    problems += repeatedIds("chat", content.chats.map { Filed(it.file, it.value.id) })
    content.chats.forEach { (file, script) ->
        fun problem(message: String) {
            problems += ContentProblem(file, script.id, "Chat ${script.id} $message")
        }
        if (script.turns.isEmpty()) problem("has no turns")
        script.turns.map { it.id }.repeated().forEach { problem("has more than one turn $it") }
        script.items.filter { content.itemsComplete && it !in content.course().itemsById }.forEach { problem("lists Item $it, which is not defined") }
        val turns = script.turns.map { it.id }.toSet()
        script.turns.forEach { turn ->
            if (turn.says.isBlank()) problem("turn ${turn.id} says nothing")
            if (turn.replies.isNotEmpty() && turn.replies.none { it.right }) problem("turn ${turn.id} has no right reply")
            turn.replies.map { normalizeAnswer(it.text) }.repeated().forEach { problem("turn ${turn.id} has two replies that read \"$it\"") }
            turn.replies.forEach { reply ->
                if (normalizeAnswer(reply.text).isEmpty()) problem("turn ${turn.id} has a blank reply")
                reply.next?.let { next -> if (next !in turns) problem("turn ${turn.id} leads to turn $next, which is not there") }
                if (!reply.right && reply.next != null) problem("turn ${turn.id} has a wrong reply that leads on")
                if (reply.right) typedAnswersProblem(reply.answers)?.let { problem("turn ${turn.id}: the reply \"${reply.text}\" $it") }
            }
        }
    }
    return problems
}

/**
 * What the Ankieta Game could not play: repeated ids, an answer to a field that is not there, a
 * field someone answers with too little to choose among, an Item not defined.
 */
private fun ankietaProblems(content: ReadContent): List<ContentProblem> {
    val file = content.ankietaFile ?: return emptyList()
    val ankieta = content.ankieta
    val problems = mutableListOf<ContentProblem>()
    ankieta.fields.map { it.id }.repeated().forEach { problems += ContentProblem(file, it, "Ankieta has more than one field $it") }
    ankieta.people.map { it.id }.repeated().forEach { problems += ContentProblem(file, it, "Ankieta has more than one person $it") }
    val fields = ankieta.fields.map { it.id }.toSet()
    ankieta.fields.filter { it.label.isBlank() }.forEach { problems += ContentProblem(file, it.id, "Ankieta field ${it.id} has no label") }
    ankieta.people.forEach { person ->
        fun problem(message: String) {
            problems += ContentProblem(file, person.id, "Ankieta person ${person.id} $message")
        }
        if (person.says.isEmpty()) problem("says nothing")
        if (person.items.isEmpty()) problem("lists no Items, so they would be met before anything is learned")
        if (person.says.any { it.text.isBlank() }) problem("has a blank line")
        person.items.filter { content.itemsComplete && it !in content.course().itemsById }.forEach { problem("lists Item $it, which is not defined") }
        (person.answers.keys + person.wrong.keys).filter { it !in fields }.forEach { problem("answers field $it, which is not there") }
        if (person.answers.keys.none { it in fields }) problem("answers no field")
        person.answers.forEach { (field, answer) ->
            if (answer.isBlank()) problem("has a blank answer to $field")
            val others = ankieta.people.filter { it.id != person.id }.mapNotNull { it.answers[field] } + person.wrong[field].orEmpty()
            val choices = others.map { it.lowercase(Locale.ROOT) }.toSet() - answer.lowercase(Locale.ROOT)
            if (choices.size < 2) problem("answers $field with fewer than two other answers to choose among")
        }
        person.wrong.forEach { (field, wrong) ->
            if (wrong.any { it.equals(person.answers[field], ignoreCase = true) }) problem("gives the right answer to $field as a wrong one")
        }
    }
    return problems
}
