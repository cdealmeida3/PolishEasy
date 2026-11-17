package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ChatTest {
    private fun en(text: String) = mapOf("en" to text)

    private fun reply(
        text: String,
        right: Boolean = false,
        next: String? = null,
        answers: List<String> = emptyList(),
        register: Register? = null,
        note: String? = null,
    ) = ChatReply(text, en("$text in English"), right, next, answers, register, note?.let(::en))

    /**
     * Hello, then where from, which branches: "Z Anglii" leads to the goodbye, "A ty?" to a turn
     * of its own first. The goodbye is the person's last line.
     */
    private val script = ChatScript(
        id = "chat-cafe",
        person = "Ewa",
        title = en("At the café"),
        scene = en("You meet Ewa."),
        register = Register.INFORMAL,
        items = listOf("czesc", "skad"),
        turns = listOf(
            ChatTurn(
                "t1",
                "Cześć!",
                en("Hi!"),
                listOf(
                    reply("Cześć!", right = true, next = "t2", answers = listOf("Cześć!", "Hej!")),
                    reply("Dzień dobry!", register = Register.FORMAL, note = "*Dzień dobry* is formal."),
                    reply("Dobranoc!"),
                ),
            ),
            ChatTurn(
                "t2",
                "Skąd jesteś?",
                en("Where are you from?"),
                listOf(
                    reply("Jestem z Anglii.", right = true, next = "t4", answers = listOf("Jestem z Anglii.", "Z Anglii.")),
                    reply("A ty?", right = true, next = "t3"),
                    reply("Jestem tutaj."),
                ),
            ),
            ChatTurn("t3", "Z Polski. A ty?", en("From Poland. And you?"), listOf(reply("Z Anglii.", right = true, next = "t4"), reply("Tak."))),
            ChatTurn("t4", "Pa!", en("Bye!")),
        ),
    )

    private val course = Course(
        listOf(levelOf("a1", day("d1", choose("d1-e1", "cześć", "czesc")), day("d2", choose("d2-e1", "skąd", "skad")))),
        listOf(item("czesc", "cześć"), item("skad", "skąd")),
        clips = listOf("Cześć!", "Skąd jesteś?").map { clip(it) },
        chats = listOf(script),
    )

    private fun player() = Player(course = course)

    private fun Practice.chat(way: String = "choose", speech: Boolean = false): ChatRound =
        start(setup(Game.CHAT, listOf("chat-cafe", way), speech = speech), speech = speech) as ChatRound

    private fun ChatRound.option(text: String): Int = options.indexOfFirst { it.text == text }.also { check(it >= 0) { "No option $text in $options" } }

    @Test
    fun `a chat is offered once every Item it lists is learned`() {
        fun state(vararg done: String) = player().completed(*done).practice().view().games.single { it.game == Game.CHAT }.state
        assertEquals(GameState.NEEDS_MORE, state())
        assertEquals(GameState.NEEDS_MORE, state("d1"))
        assertEquals(GameState.READY, state("d1", "d2"))
        val setup = player().completed("d1", "d2").practice().setup(Game.CHAT)
        assertEquals(listOf(OptionView("chat-cafe", "At the café")), setup.choices[0].options)
    }

    @Test
    fun `the ways to play are choose and type, and say only when the Learner can be heard`() {
        val practice = player().completed("d1", "d2").practice()
        assertEquals(listOf("choose", "type"), practice.setup(Game.CHAT).choices[1].options.map { it.id })
        assertEquals(listOf("choose", "type", "say"), practice.setup(Game.CHAT, speech = true).choices[1].options.map { it.id })
        // Say is not kept when it cannot be offered.
        assertEquals("choose", practice.setup(Game.CHAT, listOf("chat-cafe", "say")).choices[1].selected)
    }

    @Test
    fun `a chat opens on the person's first line and goes on with each right reply chosen`() {
        var round = player().completed("d1", "d2").practice().chat()
        assertEquals(listOf(ChatLine(mine = false, "Cześć!", "Hi!")), round.lines)
        assertEquals(setOf("Cześć!", "Dzień dobry!", "Dobranoc!"), round.options.map { it.text }.toSet())
        assertEquals(Register.FORMAL, round.options.single { it.text == "Dzień dobry!" }.register)
        assertEquals(2, round.total)

        round = round.choose(round.option("Cześć!"))
        assertEquals(true, round.given?.correct)
        // Answered, the options stay to be seen, and no other reply can be given.
        assertSame(round, round.choose(0))
        round = round.next()
        assertEquals(
            listOf(ChatLine(false, "Cześć!", "Hi!"), ChatLine(true, "Cześć!", "Cześć! in English"), ChatLine(false, "Skąd jesteś?", "Where are you from?")),
            round.lines,
        )
        round = round.choose(round.option("Jestem z Anglii.")).next()
        assertEquals(ChatLine(false, "Pa!", "Bye!"), round.lines.last())
        assertTrue(round.finished)
        assertEquals(Score(2, of = 2), round.score)
    }

    @Test
    fun `a wrong reply shows the right one and its note, and the chat goes on with the right one`() {
        var round = player().completed("d1", "d2").practice().chat()
        round = round.choose(round.option("Dzień dobry!"))
        val given = checkNotNull(round.given)
        assertFalse(given.correct)
        assertEquals("Cześć!", given.reply.text)
        assertEquals("Dzień dobry is formal.", given.note?.text)
        assertEquals(listOf(0..10), given.note?.polish)
        assertEquals(Register.FORMAL, given.register)
        round = round.next()
        assertEquals(ChatLine(true, "Cześć!", "Cześć! in English", missed = true), round.lines[1])
        round = round.choose(round.option("Jestem tutaj."))
        assertNull(round.given?.note)
        round = round.next()
        assertTrue(round.finished)
        assertEquals(Score(0, of = 2), round.score)
    }

    @Test
    fun `a second right reply leads down its own branch`() {
        var round = player().completed("d1", "d2").practice().chat()
        round = round.choose(round.option("Cześć!")).next()
        round = round.choose(round.option("A ty?")).next()
        assertEquals("Z Polski. A ty?", round.lines.last().text)
        assertEquals(3, round.total)
        round = round.choose(round.option("Z Anglii.")).next()
        assertTrue(round.finished)
        assertEquals(Score(3, of = 3), round.score)
    }

    @Test
    fun `typed, any Accepted Answer of a right reply will do, an accent left out is a warning, and a wrong reply typed gets its note`() {
        var round = player().completed("d1", "d2").practice().chat("type")
        assertEquals(ChatWay.TYPE, round.way)
        assertSame(round, round.type("  "))
        round = round.type("hej")
        assertEquals(true, round.given?.correct)
        assertEquals("Cześć!", round.given?.reply?.text)
        round = round.next().type("z anglii")
        assertEquals(true, round.given?.correct)
        assertEquals("Jestem z Anglii.", round.given?.reply?.text)

        var other = player().completed("d1", "d2").practice().chat("type")
        other = other.type("czesc")
        assertEquals(true, other.given?.correct)
        assertEquals(true, other.given?.accentWarning)
        other = player().completed("d1", "d2").practice().chat("type").type("dzień dobry")
        assertEquals(false, other.given?.correct)
        assertEquals("Dzień dobry is formal.", other.given?.note?.text)
        other = player().completed("d1", "d2").practice().chat("type").type("siema")
        assertEquals(false, other.given?.correct)
        assertEquals("siema", other.given?.text)
        assertEquals("Cześć!", other.given?.reply?.text)
    }

    @Test
    fun `said, it is heard leniently, nothing heard changes nothing, and the replies can be chosen instead`() {
        var round = player().completed("d1", "d2").practice().chat("say", speech = true)
        assertEquals(ChatWay.SAY, round.way)
        assertSame(round, round.say(emptyList()))
        assertSame(round, round.say(listOf("")))
        // The recognizer's second guess will do.
        round = round.say(listOf("cze", "Cześć"))
        assertEquals(true, round.given?.correct)
        round = round.next().say(listOf("jestem z anglli"))
        assertEquals(true, round.given?.correct)

        var other = player().completed("d1", "d2").practice().chat("say", speech = true)
        other = other.say(listOf("dzien dobry"))
        assertEquals(false, other.given?.correct)
        assertEquals("Dzień dobry is formal.", other.given?.note?.text)

        other = player().completed("d1", "d2").practice().chat("say", speech = true)
        assertFalse(other.choosing)
        other = other.chooseInstead()
        assertTrue(other.choosing)
        other = other.choose(other.option("Cześć!"))
        assertEquals(true, other.given?.correct)
        // Choosing instead lasts one turn.
        assertFalse(other.next().choosing)
        assertSame(other, other.chooseInstead())
    }

    @Test
    fun `a Best is kept for each chat and each way, of the replies right first time`() {
        val player = player().completed("d1", "d2")
        val practice = player.practice()
        fun play(way: String, wrongFirst: Boolean): RoundResult {
            var round = practice.chat(way)
            round = round.choose(round.option(if (wrongFirst) "Dobranoc!" else "Cześć!")).next()
            round = round.choose(round.option("Jestem z Anglii.")).next()
            return practice.finish(round)
        }
        assertEquals(1, play("choose", wrongFirst = true).best)
        val better = play("choose", wrongFirst = false)
        assertEquals(2, better.best)
        assertTrue(better.newBest)
        assertTrue(play("type", wrongFirst = true).first)
        assertEquals(2, practice.setup(Game.CHAT, listOf("chat-cafe", "choose")).best)
        assertEquals(1, practice.setup(Game.CHAT, listOf("chat-cafe", "type")).best)
    }

    @Test
    fun `speech is matched leniently - case, punctuation and marks never matter, and one word may be near`() {
        assertEquals("Dziękuję!", saidAs(listOf("dziekuje"), listOf("Dziękuję!")))
        assertEquals("Mam na imię Adam.", saidAs(listOf("mam na imie adam"), listOf("Mam na imię Adam.")))
        assertEquals("Jestem z Anglii.", saidAs(listOf("jestam z anglii"), listOf("Jestem z Anglii.")))
        // Two near words, a word too far, a word missing or another word are not it.
        assertNull(saidAs(listOf("jestam z angli"), listOf("Jestem z Anglii.")))
        assertNull(saidAs(listOf("jestem z polski"), listOf("Jestem z Anglii.")))
        assertNull(saidAs(listOf("jestem anglii"), listOf("Jestem z Anglii.")))
        assertNull(saidAs(listOf("tak"), listOf("nie")))
        assertNull(saidAs(emptyList(), listOf("tak")))
        assertEquals("pa", saidAs(listOf("Pa."), listOf("tak", "pa")))
        assertEquals(1, editDistance("jestam", "jestem"))
    }

    @Test
    fun `the loader rejects a chat that leads nowhere or has no right reply`() {
        fun problems(chat: ChatScript): List<String> {
            val files = mapOf(
                "course.json" to """{"levels": [], "chats": ["chat.json"]}""",
                "chat.json" to contentJson.encodeToString(ChatScript.serializer(), chat),
            )
            return loadingProblems(readContent { files.getValue(it) }).map { it.message }
        }
        assertEquals(emptyList(), problems(script.copy(items = emptyList())))
        val broken = script.copy(
            items = emptyList(),
            turns = listOf(
                ChatTurn("t1", "Cześć!", en("Hi!"), listOf(reply("Cześć!", right = true, next = "t9"), reply("Pa!", next = "t1"))),
                ChatTurn("t2", "Pa!", en("Bye!"), listOf(reply("Pa!"), reply("Hej!"))),
            ),
        )
        assertEquals(
            listOf(
                "Chat chat-cafe turn t1 leads to turn t9, which is not there",
                "Chat chat-cafe turn t1 has a wrong reply that leads on",
                "Chat chat-cafe turn t2 has no right reply",
            ),
            problems(broken),
        )
    }

    @Test
    fun `the content check wants every turn reached, an end, two to four replies and English throughout`() {
        val bad = script.copy(
            turns = listOf(
                ChatTurn("t1", "Cześć!", en("Hi!"), listOf(reply("Cześć!", right = true, next = "t1"))),
                ChatTurn("t2", "Pa!", en("")),
            ),
        )
        val contents = mapOf(
            "course.json" to """{"levels": [], "chats": ["chat.json"]}""",
            "chat.json" to contentJson.encodeToString(ChatScript.serializer(), bad),
        )
        val files = object : ContentFiles {
            override fun read(path: String): String = contents.getValue(path)
            override fun paths(): Set<String> = contents.keys
        }
        val messages = checkContent(files).map { it.message }
        assertTrue("Turn t1 of Chat chat-cafe has 1 replies; it must have 2 to 4" in messages, "$messages")
        assertTrue("Turn t2 of Chat chat-cafe cannot be reached from its first turn" in messages, "$messages")
        assertTrue("Chat chat-cafe never ends" in messages, "$messages")
        assertTrue(messages.any { it.startsWith("The meaning of turn t2 of Chat chat-cafe") }, "$messages")
    }

    @Test
    fun `a chat's lines and right replies are spoken, so each has a Clip`() {
        assertEquals(
            listOf("Cześć!", "Cześć!", "Skąd jesteś?", "Jestem z Anglii.", "A ty?", "Z Polski. A ty?", "Z Anglii.", "Pa!"),
            spoken(script),
        )
    }
}
