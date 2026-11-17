package com.polisheasy.engine

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
                      "dailySets": ["a1/unit-01/day-01.json", "a1/unit-01/day-02.json"]
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
                  "body": { "en": "*Cześć* is an informal hello." }
                },
                {
                  "id": "a1-u01-d01-t02",
                  "before": "a1-u01-d01-e03",
                  "title": { "en": "Saying thank you" },
                  "body": { "en": "*Dziękuję* means thank you." }
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
        "a1/unit-01/day-02.json" to """
            {
              "id": "a1-u01-d02",
              "exercises": [
                {
                  "type": "choose_translation",
                  "id": "a1-u01-d02-e01",
                  "prompt": "tak",
                  "options": [
                    { "en": "yes" },
                    { "en": "no" }
                  ],
                  "correctOption": 0
                },
                {
                  "type": "choose_translation",
                  "id": "a1-u01-d02-e02",
                  "prompt": "nie",
                  "options": [
                    { "en": "yes" },
                    { "en": "no" }
                  ],
                  "correctOption": 1
                }
              ]
            }
        """,
    )

    /** The option that answers each Exercise of days 1 and 2 correctly, by prompt. */
    val correctOption = mapOf("cześć" to 1, "do widzenia" to 0, "dziękuję" to 2, "tak" to 0, "nie" to 1)

    /** The fixture course. With [clips], it also lists that Clip manifest as its audio. */
    fun load(clips: String? = null): Course = loadWithDayOne(files.getValue(DAY_ONE), clips)

    /**
     * The fixture course with day 1 replaced by [dayOne], for testing what the loader accepts.
     * With [clips], the course also lists that Clip manifest as its audio.
     */
    fun loadWithDayOne(dayOne: String, clips: String? = null): Course = loadCourse { path ->
        when {
            path == DAY_ONE -> dayOne
            path == "course.json" && clips != null ->
                files.getValue(path).replaceFirst("{", "{ \"audio\": \"$CLIPS\",")
            path == CLIPS && clips != null -> clips
            else -> files.getValue(path)
        }
    }

    private const val DAY_ONE = "a1/unit-01/day-01.json"
    private const val CLIPS = "audio/clips.json"
}

val warsaw: ZoneId = ZoneId.of("Europe/Warsaw")

/** A clock the test moves by hand. It starts on 2 March 2026 at 09:00 in Warsaw. */
class FakeClock(private var now: LocalDateTime = LocalDate.of(2026, 3, 2).atTime(9, 0)) : Clock() {
    fun moveTo(date: LocalDate, time: LocalTime = LocalTime.of(9, 0)) {
        now = date.atTime(time)
    }

    override fun getZone(): ZoneId = warsaw
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = now.atZone(warsaw).toInstant()
}

fun march(day: Int): LocalDate = LocalDate.of(2026, 3, day)

/** One Learner's phone: their saved progress and their clock. */
class Learner(
    val progress: LearnerProgress = InMemoryLearnerProgress(),
    val clock: FakeClock = FakeClock(),
) {
    /** Opening the app, or reopening it after it was closed: a new engine over what was saved. */
    fun openApp(): CourseEngine = CourseEngine(FixtureCourse.load(), progress, clock, oneADay = { true })
}

fun newLearnerEngine(): CourseEngine = Learner().openApp()

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

/** Plays from the Step currently due to the Summary, answering every Exercise correctly. */
fun CourseEngine.playToSummary(first: Step): List<Step> {
    val seen = mutableListOf(first)
    while (true) {
        when (val step = seen.last()) {
            is Step.ShowTip -> Unit
            is Step.ShowExercise ->
                answer(right.getValue((step.exercise as ExerciseView.ChooseTranslation).prompt))
            is Step.ShowSummary -> return seen
        }
        seen += next()
    }
}

fun describe(step: Step): String = when (step) {
    is Step.ShowTip -> "Tip: ${step.tip.title}"
    is Step.ShowExercise -> (step.exercise as ExerciseView.ChooseTranslation).prompt
    is Step.ShowSummary -> "Summary"
}
