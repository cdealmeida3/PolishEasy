package com.polisheasy.engine

import java.util.Locale
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class CourseFile(val levels: List<LevelEntry>, val items: List<String> = emptyList())

@Serializable
private data class ItemsFile(val items: List<Item>)

@Serializable
private data class LevelEntry(val id: String, val title: Localized, val units: List<UnitEntry>)

@Serializable
private data class UnitEntry(val id: String, val title: Localized, val dailySets: List<String>)

private val json = Json { classDiscriminator = "type" }

/**
 * Loads the Authored Content. [readFile] returns the text of a content file given its
 * path relative to the content root; the course starts at `course.json`, which lists
 * the path of every Daily Set file and of every Item file.
 */
fun loadCourse(readFile: (path: String) -> String): Course {
    val index = json.decodeFromString<CourseFile>(readFile("course.json"))
    return checked(
        Course(
            levels(index, readFile),
            items = index.items.flatMap { path -> json.decodeFromString<ItemsFile>(readFile(path)).items },
        ),
    )
}

private fun levels(index: CourseFile, readFile: (path: String) -> String): List<Level> =
    index.levels.map { level ->
        Level(
            level.id,
            level.title,
            level.units.map { unit ->
                CourseUnit(
                    unit.id,
                    unit.title,
                    unit.dailySets.map { path -> checked(json.decodeFromString<DailySet>(readFile(path))) },
                )
            },
        )
    }

/** Rejects a course whose Items or Exercise ids break what Review relies on. */
private fun checked(course: Course): Course {
    val repeatedItems = course.items.map { it.id }.repeated()
    require(repeatedItems.isEmpty()) { "More than one Item has the id: ${repeatedItems.joinToString()}" }
    val blank = course.items.filter { it.text.isBlank() }.map { it.id }
    require(blank.isEmpty()) { "Items with no text: ${blank.joinToString()}" }
    course.path.forEach { dailySet ->
        dailySet.exercises.forEach { exercise ->
            exercise.items.forEach { item ->
                require(item in course.itemsById) {
                    "Daily Set ${dailySet.id}: Exercise ${exercise.id} lists Item $item, which is not defined"
                }
            }
        }
    }
    // Review refers to an Exercise by its id alone.
    val repeatedExercises = course.path.flatMap { it.exercises }.map { it.id }.repeated()
    require(repeatedExercises.isEmpty()) {
        "More than one Exercise in the course has the id: ${repeatedExercises.joinToString()}"
    }
    return course
}

private fun List<String>.repeated(): Set<String> = groupingBy { it }.eachCount().filterValues { it > 1 }.keys

private const val MAX_TIPS = 2

/** Rejects a Daily Set whose Tips, Accepted Answers or conjugations break what the course engine relies on. */
private fun checked(dailySet: DailySet): DailySet {
    val exerciseIds = dailySet.exercises.map { it.id }.toSet()
    val placements = dailySet.tips.map { it.before }
    require(dailySet.tips.size <= MAX_TIPS) {
        "Daily Set ${dailySet.id} has ${dailySet.tips.size} Tips; at most $MAX_TIPS are allowed"
    }
    dailySet.tips.forEach { tip ->
        require(tip.before in exerciseIds) {
            "Daily Set ${dailySet.id}: Tip ${tip.id} is placed before ${tip.before}, which is not one of its Exercises"
        }
    }
    require(placements.size == placements.toSet().size) {
        "Daily Set ${dailySet.id} has more than one Tip before the same Exercise"
    }
    dailySet.exercises.filterIsInstance<TypeAnswer>().forEach { exercise ->
        require(exercise.acceptedAnswers.isNotEmpty()) {
            "Daily Set ${dailySet.id}: Exercise ${exercise.id} has no Accepted Answers"
        }
        require(exercise.acceptedAnswers.none { normalizeAnswer(it).isEmpty() }) {
            "Daily Set ${dailySet.id}: Exercise ${exercise.id} has a blank Accepted Answer"
        }
    }
    dailySet.exercises.filterIsInstance<WordTiles>().forEach { exercise ->
        val name = "Daily Set ${dailySet.id}: Exercise ${exercise.id}"
        require(exercise.acceptedAnswers.isNotEmpty()) { "$name has no Accepted Answers" }
        require(exercise.acceptedAnswers.none { words(it).isEmpty() }) { "$name has a blank Accepted Answer" }
        require(words(exercise.acceptedAnswers.first()).size >= 2) {
            "$name has a first Accepted Answer of fewer than two words, which leaves nothing to build"
        }
        exercise.distractors.forEach { distractor ->
            require(words(distractor).size == 1) { "$name has the distractor \"$distractor\", which is not one word" }
        }
        val tiles = tilesOf(exercise).wordCounts()
        exercise.acceptedAnswers.forEach { accepted ->
            require(words(accepted).wordCounts().all { (word, count) -> count <= (tiles[word] ?: 0) }) {
                "$name has the Accepted Answer \"$accepted\", which cannot be built from its tiles"
            }
        }
    }
    dailySet.exercises.filterIsInstance<Conjugation>().forEach { exercise ->
        val name = "Daily Set ${dailySet.id}: Exercise ${exercise.id}"
        require(exercise.acceptedAnswers.isNotEmpty()) { "$name has no Accepted Answers" }
        require(exercise.acceptedAnswers.none { normalizeAnswer(it).isEmpty() }) { "$name has a blank Accepted Answer" }
        require(exercise.verb.isNotBlank()) { "$name has a blank verb" }
        require((exercise.sentence == null) == (exercise.sentenceMeaning == null)) {
            "$name must have both a sentence and a sentence meaning, or neither"
        }
        exercise.sentence?.let { sentence ->
            val gap = gapOf(sentence)
            require(gap != null) {
                "$name has the sentence \"$sentence\", which does not have exactly one gap written as $GAP"
            }
            require(words(gap.first + " " + gap.second).isNotEmpty()) {
                "$name has the sentence \"$sentence\", which has no words apart from the gap"
            }
        }
    }
    dailySet.tips.forEach { tip ->
        val table = tip.conjugation ?: return@forEach
        val name = "Daily Set ${dailySet.id}: Tip ${tip.id}"
        require(table.verb.isNotBlank()) { "$name has a conjugation table with a blank verb" }
        Person.entries.forEach { person ->
            require(person in table.forms) {
                "$name has a conjugation table with no form for ${person.pronouns}"
            }
            require(table.forms.getValue(person).isNotBlank()) {
                "$name has a conjugation table with a blank form for ${person.pronouns}"
            }
        }
    }
    return dailySet
}

/** How many times each word is in this list, letter case ignored. */
private fun List<String>.wordCounts(): Map<String, Int> = groupingBy { it.lowercase(Locale.ROOT) }.eachCount()
