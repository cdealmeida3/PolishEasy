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
data class DailySet(val id: String, val tips: List<Tip> = emptyList(), val exercises: List<Exercise>)

/** Shown once, just before the Exercise whose id is [before]. */
@Serializable
data class Tip(val id: String, val before: String, val title: Localized, val body: Localized)

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

@Serializable
@SerialName("type_answer")
data class TypeAnswer(
    override val id: String,
    /** What to say in Polish, in each Base Language. */
    val prompt: Localized,
    /** Every Polish answer treated as correct, with its proper spelling. The first is shown when the Learner is wrong. */
    val acceptedAnswers: List<String>,
) : Exercise
