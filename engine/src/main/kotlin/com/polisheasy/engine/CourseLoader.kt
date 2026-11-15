package com.polisheasy.engine

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

/** Rejects a Daily Set whose Tips or Accepted Answers break what the course engine relies on. */
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
    return dailySet
}
