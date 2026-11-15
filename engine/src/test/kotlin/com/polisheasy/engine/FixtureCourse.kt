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
                }
              ]
            }
        """,
    )

    fun load(): Course = loadCourse { path -> files.getValue(path) }
}

class FakeClock(private var date: LocalDate = LocalDate.of(2026, 3, 2)) : Clock() {
    private val zone: ZoneId = ZoneId.of("Europe/Warsaw")

    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = date.atTime(9, 0).atZone(zone).toInstant()
}

fun newLearnerEngine(clock: Clock = FakeClock()): CourseEngine =
    CourseEngine(FixtureCourse.load(), InMemoryLearnerProgress(), clock)
