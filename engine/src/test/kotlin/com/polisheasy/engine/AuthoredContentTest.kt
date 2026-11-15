package com.polisheasy.engine

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AuthoredContentTest {
    @Test
    fun `the Authored Content shipped with the app loads and can be started`() {
        val contentDir = File(System.getProperty("contentDir"))
        val course = loadCourse { path -> File(contentDir, path).readText() }

        val step = CourseEngine(course, InMemoryLearnerProgress(), FakeClock()).startDailySet()

        assertTrue(step is Step.ShowExercise)
    }
}
