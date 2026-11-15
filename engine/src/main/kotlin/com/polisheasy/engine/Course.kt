package com.polisheasy.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Text keyed by Base Language code, for example `{"en": "hi"}`. */
typealias Localized = Map<String, String>

data class Course(val levels: List<Level>) {
    /** Every Daily Set in Path order. */
    val path: List<DailySet> = levels.flatMap { level -> level.units.flatMap { it.dailySets } }
}

data class Level(val id: String, val title: Localized, val units: List<CourseUnit>)

data class CourseUnit(val id: String, val title: Localized, val dailySets: List<DailySet>)

@Serializable
data class DailySet(val id: String, val exercises: List<Exercise>)

@Serializable
sealed interface Exercise {
    val id: String
}

@Serializable
@SerialName("choose_translation")
data class ChooseTranslation(
    override val id: String,
    /** The Polish word or sentence to translate. */
    val prompt: String,
    val options: List<Localized>,
    val correctOption: Int,
) : Exercise
