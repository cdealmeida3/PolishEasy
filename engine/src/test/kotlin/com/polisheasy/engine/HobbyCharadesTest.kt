package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HobbyCharadesTest {
    /** Day h1 teaches grać, jeździć and five of what follows them; day h2 pływać and śpiewać. */
    private val hobbyCourse = Course(
        listOf(
            levelOf(
                "a1",
                DailySet("h1", exercises = listOf("grac", "jezdzic", "w-pilke", "w-szachy", "na-rowerze", "na-nartach", "konno").mapIndexed { i, id -> choose("h1-e$i", id, id) }),
                DailySet("h2", exercises = listOf("plywac", "spiewac").mapIndexed { i, id -> choose("h2-e$i", id, id) }),
            ),
        ),
        listOf(
            item("grac").copy(text = "grać"), item("jezdzic").copy(text = "jeździć"), item("w-pilke").copy(text = "w piłkę"),
            item("w-szachy").copy(text = "w szachy"), item("na-rowerze").copy(text = "na rowerze"), item("na-nartach").copy(text = "na nartach"),
            item("konno"), item("plywac").copy(text = "pływać"), item("spiewac").copy(text = "śpiewać"),
        ),
        pictures = mapOf(
            "w-pilke" to Picture.Image("pictures/goal-net.webp"),
            "w-szachy" to Picture.Image("pictures/chess-pawn.webp"),
            "na-rowerze" to Picture.Image("pictures/bicycle.webp"),
            "na-nartach" to Picture.Image("pictures/skier.webp"),
            "plywac" to Picture.Image("pictures/person-swimming.webp"),
            "spiewac" to Picture.Image("pictures/microphone.webp"),
        ),
        hobbies = listOf(
            listOf("grac", "w-pilke"), listOf("grac", "w-szachy"), listOf("jezdzic", "na-rowerze"), listOf("jezdzic", "na-nartach"),
            listOf("jezdzic", "konno"), listOf("plywac"), listOf("spiewac"),
        ),
    )

    private fun player() = Player(course = hobbyCourse)

    private fun stateOf(player: Player) = player.practice().view().games.single { it.game == Game.HOBBY_CHARADES }.state

    private fun Practice.charades(answer: String = HobbyCharades.CHOOSE): HobbyRound = start(setup(Game.HOBBY_CHARADES, listOf(answer))) as HobbyRound

    @Test
    fun `Hobby charades opens with four learned hobbies that have a picture`() {
        assertEquals(GameState.READY, stateOf(player().completed("h1")))
        assertEquals(GameState.NEEDS_MORE, stateOf(Player(hobbyCourse.copy(hobbies = hobbyCourse.hobbies.drop(1))).completed("h1")))
    }

    @Test
    fun `a hobby is asked by its picture, with its words joined, and never one with no picture or not learned`() {
        val round = player().completed("h1").practice().charades()
        assertEquals(
            setOf("grać w piłkę", "grać w szachy", "jeździć na rowerze", "jeździć na nartach"),
            round.run.questions.map { it.answer }.toSet(),
        )
        assertEquals(Picture.Image("pictures/bicycle.webp"), round.run.questions.single { it.answer == "jeździć na rowerze" }.picture)
        assertEquals(setOf("grac w-pilke", "grac w-szachy", "jezdzic na-rowerze", "jezdzic na-nartach"), round.asked.toSet())
    }

    @Test
    fun `the options are four hobbies, those with the same verb first`() {
        repeat(10) { seed ->
            val round = Player(hobbyCourse, player().completed("h1", "h2").progress, seed = seed).practice().charades()
            round.run.questions.forEach { question ->
                assertEquals(HobbyCharades.OPTIONS, question.options.size)
                assertEquals(question.options.size, question.options.toSet().size)
                assertTrue(question.answer in question.options)
                val verb = question.answer.substringBefore(" ")
                if (question.answer.contains(" ")) {
                    assertTrue(question.options.count { it.startsWith("$verb ") } >= 2, "$question")
                }
            }
        }
    }

    @Test
    fun `a typed hobby is right apart from its accents, and the result is the answers right first try`() {
        var round = player().completed("h1", "h2").practice().charades(HobbyCharades.TYPE)
        assertTrue(round.typed)
        assertEquals(round, round.choose(0))
        assertEquals(6, round.total)
        round = round.type("zle")
        assertEquals(false, round.given?.correct)
        round = round.next()
        round = round.type(round.question!!.answer.replace('ć', 'c').replace('ź', 'z'))
        assertEquals(true, round.given?.correct)
        round = round.next()
        while (!round.finished) round = round.type(round.question!!.answer).next()
        assertNull(round.question)
        assertEquals(Score(5, of = 6), round.score)
    }

    @Test
    fun `a chosen hobby counts when it is the one shown`() {
        var round = player().completed("h1").practice().charades()
        val question = round.question!!
        round = round.choose(question.options.indexOfFirst { it != question.answer })
        assertEquals(false, round.given?.correct)
        round = player().completed("h1").practice().charades()
        round = round.choose(round.question!!.options.indexOf(round.question!!.answer))
        assertEquals(true, round.given?.correct)
    }

    @Test
    fun `the loader reads the hobbies and rejects one with an undefined Item, no picture, or listed twice`() {
        fun read(practice: String): Pair<List<String>, Course> {
            val files = mapOf(
                "course.json" to """{"levels": [], "items": ["items.json"], "practice": "practice.json", "pictures": "pictures/pictures.json"}""",
                "items.json" to """{"items": [
                    {"id": "grac", "text": "grać", "meaning": {"en": "to play"}},
                    {"id": "w-pilke", "text": "w piłkę", "meaning": {"en": "football"}},
                    {"id": "spiewac", "text": "śpiewać", "meaning": {"en": "to sing"}}
                ]}""",
                "pictures/pictures.json" to """{"pictures": [{"item": "w-pilke", "image": "goal-net.webp"}]}""",
                "practice.json" to practice,
            )
            val content = readContent { files.getValue(it) }
            return loadingProblems(content).map { it.message } to content.course()
        }
        val (problems, course) = read("""{"hobbies": [["grac", "w-pilke"]]}""")
        assertEquals(emptyList(), problems.filter { "hobby" in it })
        assertEquals(listOf(listOf("grac", "w-pilke")), course.hobbies)
        assertEquals(
            listOf(
                "The hobby grac w-karty names Item w-karty, which is not defined",
                "The hobby grac w-karty has no Item with a picture",
                "The hobby spiewac has no Item with a picture",
                "The hobby grac w-pilke is listed more than once",
            ),
            read("""{"hobbies": [["grac", "w-pilke"], ["grac", "w-karty"], ["spiewac"], ["grac", "w-pilke"]]}""").first.filter { "hobby" in it },
        )
    }
}
