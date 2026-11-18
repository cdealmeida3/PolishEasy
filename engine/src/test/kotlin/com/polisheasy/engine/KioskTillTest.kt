package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KioskTillTest {
    private val units = listOf("jeden", "dwa", "trzy", "cztery", "pięć", "sześć", "siedem", "osiem", "dziewięć")
    private val tens = listOf("dziesięć", "dwadzieścia", "trzydzieści", "czterdzieści", "pięćdziesiąt")

    /** k1 teaches 1 to 9, k2 the tens to 50, k3 the money, i, kosztuje and the goods. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("k1", *units.map { choose("e-$it", it, it) }.toTypedArray()),
                day("k2", *tens.map { choose("e-$it", it, it) }.toTypedArray()),
                day("k3", *listOf("zloty", "grosz", "i", "kosztuje", "gazeta", "bilet").map { choose("e-$it", it, it) }.toTypedArray()),
            ),
        ),
        (units + tens + "i" + "kosztuje").map { item(it) } +
            item("zloty", "złoty").copy(forms = listOf("złote", "złotych")) +
            item("grosz").copy(forms = listOf("grosze", "groszy")) +
            item("gazeta") + item("bilet"),
        clips = (units + tens + listOf("złoty", "złote", "złotych", "groszy", "i", "kosztuje", "gazeta", "bilet")).map { clip(it) },
        pictures = units.withIndex().associate { (i, name) -> name to Picture.Glyph("${i + 1}") } +
            tens.withIndex().associate { (i, name) -> name to Picture.Glyph("${(i + 1) * 10}") } +
            mapOf("gazeta" to Picture.Image("pictures/newspaper.webp"), "bilet" to Picture.Image("pictures/ticket.webp")),
        till = Till("zloty", "grosz", "i", "kosztuje", listOf("gazeta", "bilet", "kawa")),
    )

    private fun player(course: Course = this.course) = Player(course = course)

    private fun stateOf(player: Player) = player.practice().view().games.single { it.game == Game.KIOSK_TILL }.state

    private fun Practice.till(vararg wanted: String, sound: Boolean = true): TillRound =
        start(setup(Game.KIOSK_TILL, wanted.toList(), sound), sound) as TillRound

    /** Pays [grosze] with the fewest coins and notes the Round has. */
    private fun TillRound.payWith(grosze: Int): TillRound {
        var left = grosze
        var round = this
        money.sortedDescending().forEach { coin -> while (left >= coin) { round = round.add(coin); left -= coin } }
        return round
    }

    @Test
    fun `Kiosk till opens once the money, kosztuje and a pictured good are learned`() {
        assertEquals(GameState.NEEDS_MORE, stateOf(player().completed("k1", "k2")))
        assertEquals(GameState.READY, stateOf(player().completed("k1", "k3")))
        assertEquals(GameState.NEEDS_MORE, stateOf(player(course.copy(till = null)).completed("k1", "k2", "k3")))
        assertEquals(GameState.READY, player().completed("k1", "k3").practice().view(sound = false).games.single { it.game == Game.KIOSK_TILL }.state)
    }

    @Test
    fun `small prices are paid with coins, and big ones with notes too once the tens are learned`() {
        val practice = player().completed("k1", "k3").practice()
        assertEquals(listOf(KioskTill.COINS), practice.setup(Game.KIOSK_TILL, listOf(KioskTill.READ)).choices[1].options.map { it.id })
        val all = player().completed("k1", "k2", "k3").practice()
        assertEquals(listOf(KioskTill.COINS, KioskTill.NOTES), all.setup(Game.KIOSK_TILL, listOf(KioskTill.READ)).choices[1].options.map { it.id })
        assertEquals(KioskTill.COIN_VALUES, all.till(KioskTill.READ, KioskTill.COINS).money)
        assertEquals(KioskTill.COIN_VALUES + KioskTill.NOTE_VALUES, all.till(KioskTill.READ, KioskTill.NOTES).money)
        assertTrue(all.till(KioskTill.READ, KioskTill.NOTES).run.questions.all { it.price / 100 in 10..59 })
    }

    @Test
    fun `a price is said with the good, kosztuje, and the money in the form that goes after its number`() {
        val round = player().completed("k1", "k2", "k3").practice().till(KioskTill.READ, KioskTill.COINS)
        assertEquals(KioskTill.QUESTIONS, round.total)
        round.run.questions.forEach { question ->
            assertTrue(question.words.take(2) in listOf(listOf("gazeta", "kosztuje"), listOf("bilet", "kosztuje")), "$question")
            val zloty = question.price / 100
            val grosze = question.price % 100
            val money = if (zloty == 1) "złoty" else if (zloty in 2..4) "złote" else "złotych"
            assertEquals(listOf(units[zloty - 1], money), question.words.subList(2, 4), "$question")
            if (grosze == 0) assertEquals(4, question.words.size) else assertEquals(listOf("i", tens[grosze / 10 - 1], "groszy"), question.words.drop(4))
            assertEquals("%d,%02d zł".format(zloty, grosze), question.numerals)
            assertTrue(question.polish.first().isUpperCase() && question.polish.endsWith("."))
        }
    }

    @Test
    fun `the price is paid exactly, coin by coin, and a coin can be taken back`() {
        var round = player().completed("k1", "k2", "k3").practice().till(KioskTill.READ, KioskTill.COINS)
        assertFalse(round.canPay)
        assertEquals(round, round.pay())
        assertEquals(round, round.add(1000))
        round = round.add(500).add(20)
        assertEquals(listOf(500, 20), round.paid)
        round = round.takeBack(0)
        assertEquals(listOf(20), round.paid)

        val price = checkNotNull(round.question).price
        round = round.takeBack(0).payWith(price).pay()
        assertEquals(Given(KioskTill.numerals(price), true), round.given)
        assertEquals(round, round.add(10))
        round = round.next()
        assertEquals(emptyList(), round.paid)

        // Too much is as wrong as too little.
        val next = checkNotNull(round.question).price
        round = round.payWith(next).add(10).pay()
        assertEquals(false, round.given?.correct)
        round = round.next()
        while (!round.finished) round = round.payWith(checkNotNull(round.question).price).pay().next()
        assertEquals(Score(KioskTill.QUESTIONS - 1, of = KioskTill.QUESTIONS), round.score)
    }

    @Test
    fun `heard, only prices and goods with a Clip of every word are asked, and with sound off it is read`() {
        val noTens = course.copy(clips = course.clips.filter { it.text != "czterdzieści" })
        val practice = player(noTens).completed("k1", "k2", "k3").practice()
        val heard = practice.till(KioskTill.LISTEN, KioskTill.COINS)
        assertTrue(heard.listen)
        assertTrue(heard.run.questions.none { "czterdzieści" in it.words })
        assertEquals(listOf(KioskTill.READ), practice.setup(Game.KIOSK_TILL, sound = false).choices[0].options.map { it.id })
    }

    @Test
    fun `the loader reads the till and rejects an undefined Item, money without its forms, and a good with no picture or listed twice`() {
        fun read(practice: String): Pair<List<String>, Course> {
            val files = mapOf(
                "course.json" to """{"levels": [], "items": ["items.json"], "practice": "practice.json", "pictures": "pictures/pictures.json"}""",
                "items.json" to """{"items": [
                    {"id": "zloty", "text": "złoty", "meaning": {"en": "złoty"}, "forms": ["złote", "złotych"]},
                    {"id": "grosz", "text": "grosz", "meaning": {"en": "grosz"}},
                    {"id": "i", "text": "i", "meaning": {"en": "and"}},
                    {"id": "kosztuje", "text": "kosztuje", "meaning": {"en": "costs"}},
                    {"id": "gazeta", "text": "gazeta", "meaning": {"en": "a newspaper"}},
                    {"id": "sok", "text": "sok", "meaning": {"en": "juice"}}
                ]}""",
                "pictures/pictures.json" to """{"pictures": [{"item": "gazeta", "image": "newspaper.webp"}]}""",
                "practice.json" to practice,
            )
            val content = readContent { files.getValue(it) }
            return loadingProblems(content).map { it.message } to content.course()
        }
        val (_, course) = read("""{"till": {"zloty": "zloty", "grosz": "grosz", "and": "i", "costs": "kosztuje", "goods": ["gazeta"]}}""")
        assertEquals(Till("zloty", "grosz", "i", "kosztuje", listOf("gazeta")), course.till)
        assertEquals(
            listOf(
                "Kiosk till names Item kawa, which is not defined",
                "The money grosz needs two forms: the one after 2 and the one after 5",
                "The good sok has no picture",
                "The good kawa has no picture",
                "The good gazeta is listed more than once",
            ),
            read("""{"till": {"zloty": "zloty", "grosz": "grosz", "and": "i", "costs": "kosztuje", "goods": ["gazeta", "sok", "kawa", "gazeta"]}}""")
                .first.filter { "till" in it || "money" in it || "good" in it },
        )
    }
}
