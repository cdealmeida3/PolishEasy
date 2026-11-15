package com.polisheasy.engine

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A tiny course held in memory, in the same file format as the real Authored Content. */
object FixtureCourse {
    private val files = mapOf(
        "course.json" to """
            {
              "levels": [
                {
                  "id": "a1",
                  "title": { "en": "A1" },
                  "units": [
                    {
                      "id": "a1-u01",
                      "title": { "en": "Greetings" },
                      "dailySets": ["a1/unit-01/day-01.json"]
                    }
                  ]
                }
              ]
            }
        """,
        "a1/unit-01/day-01.json" to """
            {
              "id": "a1-u01-d01",
              "tips": [
                {
                  "id": "a1-u01-d01-t01",
                  "before": "a1-u01-d01-e01",
                  "title": { "en": "Saying hello" },
                  "body": { "en": "Cześć is an informal hello." }
                },
                {
                  "id": "a1-u01-d01-t02",
                  "before": "a1-u01-d01-e03",
                  "title": { "en": "Saying thank you" },
                  "body": { "en": "Dziękuję means thank you." }
                }
              ],
              "exercises": [
                {
                  "type": "choose_translation",
                  "id": "a1-u01-d01-e01",
                  "prompt": "cześć",
                  "options": [
                    { "en": "thank you" },
                    { "en": "hi" },
                    { "en": "goodbye" }
                  ],
                  "correctOption": 1
                },
                {
                  "type": "choose_translation",
                  "id": "a1-u01-d01-e02",
                  "prompt": "do widzenia",
                  "options": [
                    { "en": "goodbye" },
                    { "en": "hi" },
                    { "en": "please" }
                  ],
                  "correctOption": 0
                },
                {
                  "type": "choose_translation",
                  "id": "a1-u01-d01-e03",
                  "prompt": "dziękuję",
                  "options": [
                    { "en": "yes" },
                    { "en": "no" },
                    { "en": "thank you" }
                  ],
                  "correctOption": 2
                }
              ]
            }
        """,
    )

    /** The option that answers each Exercise of day 1 correctly, by prompt. */
    val correctOption = mapOf("cześć" to 1, "do widzenia" to 0, "dziękuję" to 2)

    fun load(): Course = loadCourse { path -> files.getValue(path) }

    /** The fixture course with day 1 replaced by [dayOne], for testing what the loader accepts. */
    fun loadWithDayOne(dayOne: String): Course =
        loadCourse { path -> if (path == "a1/unit-01/day-01.json") dayOne else files.getValue(path) }
}

class FakeClock(private var date: LocalDate = LocalDate.of(2026, 3, 2)) : Clock() {
    private val zone: ZoneId = ZoneId.of("Europe/Warsaw")

    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = date.atTime(9, 0).atZone(zone).toInstant()
}

fun newLearnerEngine(clock: Clock = FakeClock()): CourseEngine =
    CourseEngine(FixtureCourse.load(), InMemoryLearnerProgress(), clock)

/**
 * Plays from the Step currently due, continuing past Tips and giving one of [answers] to each
 * Exercise shown, until the answers run out or the Summary is reached. Returns every Step seen,
 * ending with the Step that is due next.
 */
fun CourseEngine.play(first: Step, answers: List<Answer>): List<Step> {
    val seen = mutableListOf(first)
    val remaining = ArrayDeque(answers)
    while (true) {
        when (seen.last()) {
            is Step.ShowTip -> Unit
            is Step.ShowExercise -> answer(remaining.removeFirstOrNull() ?: return seen)
            is Step.ShowSummary -> return seen
        }
        seen += next()
    }
}

/** The prompts of the Exercises among [steps], in the order they were shown. */
fun prompts(steps: List<Step>): List<String> =
    steps.filterIsInstance<Step.ShowExercise>().map { (it.exercise as ExerciseView.ChooseTranslation).prompt }

val right = FixtureCourse.correctOption.mapValues { Answer.Option(it.value) }
val wrongHello = Answer.Option(0)
