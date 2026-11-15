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
                        unit.dailySets.map { path -> json.decodeFromString<DailySet>(readFile(path)) },
                    )
                },
            )
        },
    )
}
