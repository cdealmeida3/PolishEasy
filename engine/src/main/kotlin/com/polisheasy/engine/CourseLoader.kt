package com.polisheasy.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class CourseFile(val levels: List<LevelEntry>)

@Serializable
private data class LevelEntry(val id: String, val title: Localized, val units: List<UnitEntry>)

@Serializable
private data class UnitEntry(val id: String, val title: Localized, val dailySets: List<String>)

private val json = Json { classDiscriminator = "type" }

/**
 * Loads the Authored Content. [readFile] returns the text of a content file given its
 * path relative to the content root; the course starts at `course.json`, which lists
 * the path of every Daily Set file.
 */
fun loadCourse(readFile: (path: String) -> String): Course {
    val index = json.decodeFromString<CourseFile>(readFile("course.json"))
    return Course(
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
        },
    )
}

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
