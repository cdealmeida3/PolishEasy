package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlateBuilderTest {
    private val first = listOf("chleb", "z masłem", "masło", "herbata", "z cytryną", "cytryna")
    private val second = listOf("kawa", "z mlekiem", "mleko", "kanapka", "z serem", "ser", "i")
    private val foods = listOf("chleb", "masło", "herbata", "cytryna", "kawa", "mleko", "kanapka", "ser")

    /** l1 teaches two dishes, l2 three more (herbata z mlekiem too) and *i*. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("l1", *first.map { choose("e-$it", it, it) }.toTypedArray()),
                day("l2", *second.map { choose("e-$it", it, it) }.toTypedArray()),
            ),
        ),
        (first + second).map { item(it) },
        clips = (first + second).map { clip(it) },
        pictures = foods.associateWith { Picture.Image("$it.webp") },
        plates = Plates(
            "i",
            listOf(
                Dish("chleb", "z masłem", "masło"),
                Dish("herbata", "z cytryną", "cytryna"),
                Dish("kawa", "z mlekiem", "mleko"),
                Dish("herbata", "z mlekiem", "mleko"),
                Dish("kanapka", "z serem", "ser"),
            ),
        ),
    )

    private fun player(course: Course = this.course, seed: Int = 0) = Player(course = course, seed = seed)

    private fun stateOf(player: Player) = player.practice().view().games.single { it.game == Game.PLATE_BUILDER }.state

    private fun Practice.plates(vararg wanted: String, sound: Boolean = true): PlateRound =
        start(setup(Game.PLATE_BUILDER, wanted.toList(), sound), sound) as PlateRound

    @Test
    fun `Plate builder opens once four dishes can be ordered, and two dishes are a level once i is learned`() {
        assertEquals(GameState.NEEDS_MORE, stateOf(player().completed("l1")))
        assertEquals(GameState.READY, stateOf(player().completed("l1", "l2")))
        val levels = player().completed("l1", "l2").practice().setup(Game.PLATE_BUILDER, listOf(PlateBuilder.READ)).choices[1].options.map { it.id }
        assertEquals(listOf(PlateBuilder.ONE, PlateBuilder.TWO), levels)
        val noAnd = course.copy(plates = course.plates?.copy(and = "oraz"))
        val one = player(noAnd).completed("l1", "l2").practice().setup(Game.PLATE_BUILDER, listOf(PlateBuilder.READ)).choices[1].options.map { it.id }
        assertEquals(listOf(PlateBuilder.ONE), one)
    }

    @Test
    fun `an order is the dish said, and the tray holds its foods among others, each picture once`() {
        repeat(10) { seed ->
            val round = player(seed = seed).completed("l1", "l2").practice().plates(PlateBuilder.READ, PlateBuilder.ONE)
            round.run.questions.forEach { question ->
                assertEquals(2, question.wanted.size)
                assertTrue(question.tray.map { it.id }.containsAll(question.wanted))
                assertEquals(PlateBuilder.TRAY_ONE, question.tray.size)
                assertEquals(question.tray.size, question.tray.map { it.picture }.toSet().size)
                assertEquals(2, question.words.size)
                assertTrue(question.words[1].startsWith("z "), question.polish)
            }
            assertEquals(round.run.questions.map { it.key }.toSet().size, round.run.questions.size)
        }
    }

    @Test
    fun `two dishes are joined by i and need four different foods`() {
        repeat(10) { seed ->
            val round = player(seed = seed).completed("l1", "l2").practice().plates(PlateBuilder.READ, PlateBuilder.TWO)
            round.run.questions.forEach { question ->
                assertEquals(5, question.words.size)
                assertEquals("i", question.words[2])
                assertEquals(4, question.wanted.size)
                assertTrue(question.tray.map { it.id }.containsAll(question.wanted))
                assertTrue(question.tray.size <= PlateBuilder.TRAY_TWO)
            }
        }
    }

    @Test
    fun `a food is put on the plate and taken off again, and only exactly the foods wanted are right`() {
        var round = player().completed("l1", "l2").practice().plates(PlateBuilder.READ, PlateBuilder.ONE)
        val question = checkNotNull(round.question)
        assertFalse(round.canServe)
        assertEquals(round, round.serve())
        assertEquals(round, round.place("nothing"))
        val other = question.tray.first { it.id !in question.wanted }.id
        round = round.place(other).place(other)
        assertEquals(emptyList(), round.placed)
        question.wanted.forEach { round = round.place(it) }
        round = round.place(other)
        assertTrue(round.canServe)
        round = round.serve()
        assertEquals(false, round.given?.correct)
        assertEquals(round, round.place(other))
        round = round.next()
        assertEquals(emptyList(), round.placed)
        while (!round.finished) round = checkNotNull(round.question).wanted.fold(round) { r, id -> r.place(id) }.serve().next()
        assertEquals(Score(round.total - 1, of = round.total), round.score)
    }

    @Test
    fun `heard, only dishes whose words have Clips are ordered, and with sound off it is read`() {
        val noLemon = course.copy(clips = course.clips.filter { it.text != "z cytryną" })
        val practice = player(noLemon).completed("l1", "l2").practice()
        val round = practice.plates(PlateBuilder.LISTEN, PlateBuilder.ONE)
        assertTrue(round.listen)
        assertTrue(round.run.questions.none { "z cytryną" in it.words })
        assertEquals(listOf(PlateBuilder.READ), practice.setup(Game.PLATE_BUILDER, sound = false).choices[0].options.map { it.id })
    }

    @Test
    fun `the loader reads the dishes and rejects an undefined Item, a dish not of three, one without pictures or with one picture twice, and one listed twice`() {
        fun read(practice: String): Pair<List<String>, Course> {
            val files = mapOf(
                "course.json" to """{"levels": [], "items": ["items.json"], "practice": "practice.json", "pictures": "pictures/pictures.json"}""",
                "items.json" to """{"items": [
                    {"id": "i", "text": "i", "meaning": {"en": "and"}},
                    {"id": "chleb", "text": "chleb", "meaning": {"en": "bread"}},
                    {"id": "z-maslem", "text": "z masłem", "meaning": {"en": "with butter"}},
                    {"id": "maslo", "text": "masło", "meaning": {"en": "butter"}},
                    {"id": "bulka", "text": "bułka", "meaning": {"en": "a roll"}},
                    {"id": "kawa", "text": "kawa", "meaning": {"en": "coffee"}},
                    {"id": "z-mlekiem", "text": "z mlekiem", "meaning": {"en": "with milk"}},
                    {"id": "mleko", "text": "mleko", "meaning": {"en": "milk"}}
                ]}""",
                "pictures/pictures.json" to """{"pictures": [
                    {"item": "chleb", "image": "bread.webp"},
                    {"item": "maslo", "image": "butter.webp"},
                    {"item": "bulka", "image": "bread.webp"},
                    {"item": "kawa", "image": "coffee.webp"}
                ]}""",
                "practice.json" to practice,
            )
            val content = readContent { files.getValue(it) }
            return loadingProblems(content).map { it.message } to content.course()
        }
        val (_, course) = read("""{"plates": {"and": "i", "dishes": [["chleb", "z-maslem", "maslo"]]}}""")
        assertEquals(Plates("i", listOf(Dish("chleb", "z-maslem", "maslo"))), course.plates)
        assertEquals(
            listOf(
                "Plate builder names Item oraz, which is not defined",
                "A dish must be three Items, its base, its phrase with z and its topping, and chleb maslo is not",
                "The dish kawa z-mlekiem mleko needs a picture of mleko",
                "The dish chleb z-maslem bulka has the same picture for its base and its topping",
                "The dish kawa z-cukrem cukier names Item z-cukrem, which is not defined",
                "The dish kawa z-cukrem cukier names Item cukier, which is not defined",
                "The dish kawa z-cukrem cukier needs a picture of cukier",
                "The dish chleb z-maslem maslo is listed more than once",
            ),
            read(
                """{"plates": {"and": "oraz", "dishes": [
                    ["chleb", "z-maslem", "maslo"], ["chleb", "maslo"], ["kawa", "z-mlekiem", "mleko"], ["chleb", "z-maslem", "bulka"],
                    ["kawa", "z-cukrem", "cukier"], ["chleb", "z-maslem", "maslo"]
                ]}}""",
            ).first.filter { "Plate" in it || "dish" in it },
        )
    }
}
