package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WhosWhoTest {
    private fun job(id: String, instrumental: String) = item(id).copy(forms = listOf(instrumental))

    /**
     * Day w1 teaches four jobs, three of them pictured, and a noun that is no job. Day w2 teaches
     * kim, which opens the Game, and two more jobs. Day w3 a job with no gender.
     */
    private val jobsCourse = Course(
        listOf(
            levelOf(
                "a1",
                DailySet("w1", exercises = listOf("lekarz", "lekarka", "kelner", "aktorka", "dom").mapIndexed { i, id -> choose("w1-e$i", id, id) }),
                DailySet("w2", exercises = listOf("kim", "muzyk", "kucharz").mapIndexed { i, id -> choose("w2-e$i", id, id) }),
                DailySet("w3", exercises = listOf("rolnik").mapIndexed { i, id -> choose("w3-e$i", id, id) }),
            ),
        ),
        listOf(
            job("lekarz", "lekarzem"), job("lekarka", "lekarką"), job("kelner", "kelnerem"), job("aktorka", "aktorką"),
            job("muzyk", "muzykiem"), job("kucharz", "kucharzem"), job("rolnik", "rolnikiem"), item("dom"), item("kim"),
        ),
        genders = mapOf(
            "lekarz" to Gender.MASCULINE, "lekarka" to Gender.FEMININE, "kelner" to Gender.MASCULINE, "aktorka" to Gender.FEMININE,
            "muzyk" to Gender.MASCULINE, "kucharz" to Gender.MASCULINE, "dom" to Gender.MASCULINE,
        ),
        pictures = mapOf(
            "lekarz" to Picture.Image("pictures/man-health-worker.webp"),
            "lekarka" to Picture.Image("pictures/woman-health-worker.webp"),
            "kucharz" to Picture.Image("pictures/man-cook.webp"),
            "muzyk" to Picture.Image("pictures/man-singer.webp"),
            "dom" to Picture.Image("pictures/house.webp"),
        ),
        jobs = listOf("lekarz", "lekarka", "kelner", "aktorka", "muzyk", "kucharz", "rolnik"),
        jobsAfter = "kim",
    )

    private fun player() = Player(course = jobsCourse)

    private fun stateOf(player: Player) = player.practice().view().games.single { it.game == Game.WHOS_WHO }.state

    private fun Practice.whosWho(vararg wanted: String): WhoRound = start(setup(Game.WHOS_WHO, wanted.toList())) as WhoRound

    private fun WhoRound.chooseRightly(): WhoRound = choose(checkNotNull(question).options.indexOf(question!!.answer))

    private fun WhoRound.chooseWrongly(): WhoRound = choose(checkNotNull(question).options.indexOfFirst { it != question!!.answer })

    @Test
    fun `Who's who opens with four learned jobs, once kim is learned`() {
        assertEquals(GameState.NEEDS_MORE, stateOf(player().completed("w1")))
        assertEquals(GameState.READY, stateOf(player().completed("w1", "w2")))
    }

    @Test
    fun `a job is given by its picture once four have one, or by its word`() {
        val practice = player().completed("w1", "w2", "w3").practice()
        assertEquals(listOf(WhosWho.PICTURE, WhosWho.WORD), practice.setup(Game.WHOS_WHO).choices.single().options.map { it.id })

        val pictured = practice.whosWho(WhosWho.PICTURE)
        assertEquals(JobClue.PICTURE, pictured.clue)
        assertEquals(setOf("lekarz", "lekarka", "kucharz", "muzyk"), pictured.asked.toSet())
        assertTrue(pictured.run.questions.all { it.picture != null })

        // The job with no gender, and the noun that is no job, are never asked.
        val words = practice.whosWho(WhosWho.WORD)
        assertEquals(setOf("lekarz", "lekarka", "kelner", "aktorka", "muzyk", "kucharz"), words.asked.toSet())
    }

    @Test
    fun `the options are the instrumental, the word alone and two other jobs, the same gender first`() {
        repeat(10) { seed ->
            val round = Player(jobsCourse, player().completed("w1", "w2").progress, seed = seed).practice().whosWho(WhosWho.WORD)
            round.run.questions.forEach { question ->
                val job = jobsCourse.itemsById.getValue(question.itemId)
                assertEquals(job.forms.first(), question.answer)
                assertEquals(WhosWho.OPTIONS, question.options.size)
                assertEquals(question.options.size, question.options.toSet().size)
                assertTrue(job.text in question.options, "$question")
                val others = question.options - setOf(question.answer, job.text)
                val sameGender = jobsCourse.jobs.filter { it != job.id && jobsCourse.genders[it] == question.gender }
                    .map { jobsCourse.itemsById.getValue(it).forms.first() }
                if (sameGender.size >= 2) assertTrue(sameGender.containsAll(others), "$question")
            }
        }
    }

    @Test
    fun `a right answer counts, a wrong one shows the answer, and the result is the answers right first try`() {
        var round = player().completed("w1", "w2").practice().whosWho(WhosWho.WORD)
        assertEquals(6, round.total)

        round = round.chooseWrongly()
        assertEquals(false, round.given?.correct)
        assertEquals(round, round.chooseRightly())
        round = round.next()
        while (!round.finished) round = round.chooseRightly().next()
        assertNull(round.question)
        assertEquals(Score(5, of = 6), round.score)
    }

    @Test
    fun `the loader reads the jobs and rejects one that is undefined, has no forms or no gender, or is listed twice`() {
        fun read(practice: String): Pair<List<String>, Course> {
            val files = mapOf(
                "course.json" to """{"levels": [], "items": ["items.json"], "practice": "practice.json"}""",
                "items.json" to """{"items": [
                    {"id": "kim", "text": "kim", "meaning": {"en": "who"}},
                    {"id": "lekarz", "text": "lekarz", "meaning": {"en": "a doctor"}, "forms": ["lekarzem"]},
                    {"id": "kelner", "text": "kelner", "meaning": {"en": "a waiter"}}
                ]}""",
                "practice.json" to practice,
            )
            val content = readContent { files.getValue(it) }
            return loadingProblems(content).map { it.message } to content.course()
        }
        val (problems, course) = read("""{"my": {"m": "kim"}, "genders": {"lekarz": "m"}, "jobs": {"after": "kim", "items": ["lekarz"]}}""")
        assertEquals(emptyList(), problems)
        assertEquals(listOf("lekarz"), course.jobs)
        assertEquals("kim", course.jobsAfter)
        assertEquals(
            listOf(
                "Who's who waits for Item kto, which is not defined",
                "The job kelner has no forms; its first form must be its instrumental",
                "The job kelner must be given the gender \"m\" or \"f\" under genders",
                "The job aktor is not a defined Item",
                "The job aktor must be given the gender \"m\" or \"f\" under genders",
                "The job lekarz is listed more than once",
            ),
            read("""{"my": {"m": "kim"}, "genders": {"lekarz": "m"}, "jobs": {"after": "kto", "items": ["lekarz", "kelner", "aktor", "lekarz"]}}""").first,
        )
    }
}
