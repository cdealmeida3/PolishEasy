package com.polisheasy.engine

import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Practice over the course shipped with the app, for a Learner at each point along the Path. */
class PracticeOverAuthoredContentTest {
    private val contentDir = File(System.getProperty("contentDir"))
    private val course = loadCourse { path -> File(contentDir, path).readText() }

    /** A Learner who completed the first [days] Daily Sets, the last of them today. */
    private fun practiceAfter(days: Int, seed: Int = 1): Practice {
        val clock = FakeClock()
        val progress = InMemoryLearnerProgress()
        course.path.take(days).forEachIndexed { index, dailySet ->
            progress.recordCompleted(dailySet.id, march(2).minusDays((days - 1 - index).toLong()))
        }
        return CourseEngine(course, progress, clock, oneADay = { true }).practice(InMemoryPracticeProgress(), Random(seed))
    }

    @Test
    fun `every verb table gives four forms to pick from, each a form of that verb`() {
        val tables = course.path.flatMap { it.tips }.mapNotNull { it.conjugation }
        assertTrue(tables.isNotEmpty())
        tables.forEach { table ->
            assertEquals(Person.entries.toSet(), table.forms.keys, "The table of ${table.verb}")
            assertTrue(table.forms.values.toSet().size >= 4, "The table of ${table.verb} has fewer than four different forms")
        }
        val forms = tables.groupBy { it.verb }.mapValues { (_, same) -> same.flatMap { it.forms.values }.toSet() }
        val practice = practiceAfter(course.path.size)
        practice.setup(Game.VERB_CHALLENGE).choices.first().options.forEach { verb ->
            repeat(5) {
                val round = practice.verbRound(verb.id)
                round.run.questions.forEach { question ->
                    val wanted = if (question.stage == VerbStage.TABLE) 6 else 4
                    assertEquals(wanted, question.options.toSet().size, "${question.verb} ${question.person} ${question.sentence}")
                    assertTrue(forms.getValue(question.verb).containsAll(question.options), "$question")
                    assertEquals(1, question.options.count { it in question.answers }, "$question")
                }
            }
        }
    }

    @Test
    fun `every chat opens once its Items are learned, says only their words, and plays to its end every way`() {
        assertTrue(course.chats.size >= 2)
        val itemsById = course.itemsById
        course.chats.forEach { script ->
            val words = script.items.flatMap { words(itemsById.getValue(it).text) }.map { it.lowercase() }.toSet()
            script.turns.forEach { turn ->
                val said = listOf(turn.says) + listOfNotNull(turn.replies.firstOrNull { it.right }?.text)
                said.forEach { line ->
                    assertEquals(emptySet(), words(line).map { it.lowercase() }.toSet() - words, "Words of \"$line\" in ${script.id} that none of its Items has")
                }
            }
        }
        val all = practiceAfter(course.path.size)
        val chats = all.setup(Game.CHAT).choices[0].options.map { it.id }
        assertEquals(course.chats.map { it.id }, chats)
        chats.forEach { chat ->
            listOf("choose", "type", "say").forEach { way ->
                var round = all.start(all.setup(Game.CHAT, listOf(chat, way), speech = true), speech = true) as ChatRound
                while (!round.finished) {
                    val turn = course.chats.single { it.id == chat }.turnsById.getValue(checkNotNull(round.turnId))
                    val right = round.options.indexOfFirst { option -> turn.replies.any { it.right && it.text == option.text } }
                    round = when (way) {
                        "type" -> round.type(round.options[right].text)
                        "say" -> round.say(listOf(round.options[right].text.lowercase()))
                        else -> round.choose(right)
                    }
                    assertEquals(true, round.given?.correct, "$chat $way")
                    round = round.next()
                }
                assertEquals(round.done, round.score.value)
                assertTrue(round.done >= 4, "$chat is too short")
            }
        }
        // Before any Daily Set, nothing to talk about.
        assertEquals(GameState.NEEDS_MORE, practiceAfter(1).view().games.single { it.game == Game.CHAT }.state)
    }

    @Test
    fun `at every point on the Path a Round asks only what has been learned`() {
        (1..course.path.size).forEach { days ->
            val learned = course.path.take(days)
            val tables = learned.flatMap { it.tips }.mapNotNull { it.conjugation?.verb }.toSet()
            val sentences = learned.flatMap { it.exercises }.filterIsInstance<Conjugation>().mapNotNull { it.sentence }.toSet()
            val practice = practiceAfter(days)
            val game = practice.view().games.single { it.game == Game.VERB_CHALLENGE }
            assertEquals(if (tables.isEmpty()) GameState.NEEDS_MORE else GameState.READY, game.state, "After $days Daily Sets")
            if (game.state != GameState.READY) return@forEach
            practice.setup(Game.VERB_CHALLENGE).choices.first().options.forEach { verb ->
                val round = practice.verbRound(verb.id).playOut()
                assertTrue(round.finished)
                round.run.questions.forEach { question ->
                    assertTrue(question.verb in tables, "After $days Daily Sets: ${question.verb}")
                    question.sentence?.let { assertTrue(it.before + GAP + it.after in sentences, "After $days Daily Sets: $it") }
                }
            }
        }
    }

    @Test
    fun `Picture cards opens after the first Daily Set and shows only pictures of what has been learned`() {
        assertTrue(course.pictures.size >= 50, "The course has ${course.pictures.size} pictures")
        (1..course.path.size).forEach { days ->
            val learned = course.path.take(days).flatMap { it.itemIds() }.toSet()
            val practice = practiceAfter(days)
            assertEquals(GameState.READY, practice.view().games.single { it.game == Game.PICTURE_CARDS }.state, "After $days Daily Sets")
            val cards = practice.pictureCards()
            assertEquals(learned.filter { it in course.pictures }.toSet(), cards.cards.map { it.itemId }.toSet(), "After $days Daily Sets")
            cards.decks.options.forEach { deck ->
                val inDeck = practice.pictureCards(deck.id)
                assertEquals(deck.id, inDeck.decks.selected)
                assertTrue(inDeck.cards.size >= PictureCards.LEAST, "After $days Daily Sets the deck ${deck.id} has ${inDeck.cards.size} cards")
            }
        }
    }

    @Test
    fun `Memory pairs lays a board of what has been learned in every way it offers, and no two pairs look alike`() {
        (1..course.path.size).forEach { days ->
            val learned = course.path.take(days).flatMap { it.itemIds() }.toSet()
            val practice = practiceAfter(days)
            val game = practice.view().games.single { it.game == Game.MEMORY_PAIRS }
            assertEquals(if (learned.size < 6) GameState.NEEDS_MORE else GameState.READY, game.state, "After $days Daily Sets")
            if (game.state != GameState.READY) return@forEach
            practice.setup(Game.MEMORY_PAIRS).choices.first().options.forEach { kind ->
                practice.setup(Game.MEMORY_PAIRS, listOf(kind.id)).choices[1].options.forEach { deck ->
                    practice.setup(Game.MEMORY_PAIRS, listOf(kind.id, deck.id)).choices[2].options.forEach { size ->
                        val way = listOf(kind.id, deck.id, size.id)
                        val round = practice.start(practice.setup(Game.MEMORY_PAIRS, way)) as MemoryRound
                        val about = "After $days Daily Sets, $way"
                        assertEquals(way, round.played, about)
                        assertEquals(2 * size.id.removePrefix("pairs-").toInt(), round.tiles.size, about)
                        assertTrue(learned.containsAll(round.asked), about)
                        // A word and the sound of it are one pair; any other two tiles alike would be two.
                        val faces = round.tiles.map { it.face }.filter { it !is MemoryFace.Sound }
                        assertEquals(faces.size, faces.toSet().size, about)
                        assertEquals(faces.size, faces.map { if (it is MemoryFace.Image) it.picture else it }.toSet().size, about)
                    }
                }
            }
        }
    }

    @Test
    fun `Phrase chains chains only sentences that have been built, and every accepted order the cards can make completes one`() {
        (1..course.path.size).forEach { days ->
            val built = course.path.take(days).flatMap { it.exercises }.filterIsInstance<WordTiles>()
            val practice = practiceAfter(days)
            val game = practice.view().games.single { it.game == Game.PHRASE_CHAINS }
            if (game.state != GameState.READY) {
                assertTrue(built.size < 6, "After $days Daily Sets, with ${built.size} sentences built")
                return@forEach
            }
            practice.setup(Game.PHRASE_CHAINS).choices.first().options.forEach { clue ->
                practice.setup(Game.PHRASE_CHAINS, listOf(clue.id)).choices[1].options.forEach { level ->
                    val way = listOf(clue.id, level.id)
                    var round = practice.start(practice.setup(Game.PHRASE_CHAINS, way)) as ChainRound
                    val about = "After $days Daily Sets, $way"
                    assertEquals(way, round.played, about)
                    assertTrue(round.total in PhraseChains.LEAST..PhraseChains.SENTENCES, about)
                    while (!round.finished) {
                        val sentence = checkNotNull(round.sentence)
                        val exercise = built.single { it.id == sentence.id }
                        assertTrue(sentence.accepted.isNotEmpty() && exercise.acceptedAnswers.containsAll(sentence.accepted), "$about: $sentence")
                        if (round.listen) assertTrue(practice.clipOf(sentence.polish) != null, "$about: no Clip for ${sentence.polish}")
                        val whole = sentence.accepted.map { answer ->
                            words(answer).fold(round) { chain, word ->
                                chain.tap(sentence.cards.indices.first { normalizeAnswer(sentence.cards[it]) == normalizeAnswer(word) && it !in chain.chain })
                            }
                        }
                        whole.forEach { chain ->
                            assertTrue(chain.complete, "$about: ${sentence.cards} cannot be chained into ${sentence.accepted}")
                            assertEquals(0, chain.slips, about)
                        }
                        round = whole.first().next()
                    }
                }
            }
        }
    }

    @Test
    fun `Listen and tap says only words that have been learned, each among four that can be told apart`() {
        (1..course.path.size).forEach { days ->
            val learned = course.path.take(days).flatMap { it.itemIds() }.toSet()
            val practice = practiceAfter(days)
            assertEquals(GameState.READY, practice.view().games.single { it.game == Game.LISTEN_AND_TAP }.state, "After $days Daily Sets")
            assertEquals(GameState.NEEDS_SOUND, practice.view(sound = false).games.single { it.game == Game.LISTEN_AND_TAP }.state)
            practice.setup(Game.LISTEN_AND_TAP).choices.first().options.forEach { mode ->
                practice.setup(Game.LISTEN_AND_TAP, listOf(mode.id)).choices[1].options.forEach { deck ->
                    val way = listOf(mode.id, deck.id)
                    val round = practice.start(practice.setup(Game.LISTEN_AND_TAP, way)) as ListenRound
                    val about = "After $days Daily Sets, $way"
                    assertEquals(way, round.played, about)
                    assertTrue(round.total in ListenAndTap.OPTIONS..ListenAndTap.QUESTIONS, about)
                    round.run.questions.forEach { question ->
                        assertTrue(practice.clipOf(question.polish) != null, "$about: no Clip for ${question.polish}")
                        assertTrue(learned.containsAll(question.options.map { it.itemId }), about)
                        val shown = question.options.map { if (round.pictures) checkNotNull(it.picture) else it.meaning }
                        assertEquals(4, shown.toSet().size, "$about: $question")
                    }
                }
            }
        }
    }

    @Test
    fun `Sound gaps asks only words that have been learned and can be heard, in every sound it offers`() {
        val seen = mutableSetOf<String>()
        (1..course.path.size).forEach { days ->
            val learned = course.path.take(days).flatMap { it.itemIds() }.toSet()
            val practice = practiceAfter(days)
            assertEquals(GameState.READY, practice.view().games.single { it.game == Game.SOUND_GAPS }.state, "After $days Daily Sets")
            practice.setup(Game.SOUND_GAPS).choices.single().options.forEach { sounds ->
                seen += sounds.id
                val round = practice.start(practice.setup(Game.SOUND_GAPS, listOf(sounds.id))) as GapRound
                val about = "After $days Daily Sets, ${sounds.id}"
                assertTrue(round.total in SoundGaps.LEAST..SoundGaps.QUESTIONS, about)
                round.run.questions.forEach { question ->
                    val gap = question.gap
                    assertTrue(gap.itemId in learned, about)
                    assertTrue(practice.clipOf(gap.polish) != null, "$about: no Clip for ${gap.polish}")
                    assertEquals(course.itemsById.getValue(gap.itemId).text, gap.before + gap.answer + gap.after, about)
                    assertTrue(sounds.id == SoundGaps.MIXED || gap.group.id == sounds.id, about)
                    assertTrue(gap.wrongWords.none { it.lowercase() in course.soundGapRealWords }, "$about: $gap")
                }
            }
        }
        // By the end of the course most of the sounds can be drilled.
        assertTrue(seen.size >= 6, "Only $seen are ever offered")
    }

    @Test
    fun `Numbers asks only numbers that have been learned, at every level it offers`() {
        val offered = mutableSetOf<String>()
        (1..course.path.size).forEach { days ->
            val learned = course.path.take(days).flatMap { it.itemIds() }.mapNotNull(course.itemsById::get).map { it.text }.toSet()
            val practice = practiceAfter(days)
            if (practice.view().games.single { it.game == Game.NUMBERS }.state != GameState.READY) return@forEach
            practice.setup(Game.NUMBERS).choices[0].options.forEach { clue ->
                practice.setup(Game.NUMBERS, listOf(clue.id)).choices[1].options.forEach { level ->
                    offered += level.id
                    val round = practice.start(practice.setup(Game.NUMBERS, listOf(clue.id, level.id))) as NumberRound
                    val about = "After $days Daily Sets, ${clue.id} ${level.id}"
                    assertTrue(round.total >= Numbers.LEAST, about)
                    round.run.questions.forEach { question ->
                        assertTrue(learned.containsAll(question.words), "$about: $question")
                        if (round.listen) question.words.forEach { assertTrue(practice.clipOf(it) != null, "$about: no Clip for $it") }
                        assertTrue(question.answer.length <= Numbers.MOST_DIGITS && question.answer.all(Char::isDigit), "$about: $question")
                    }
                }
            }
        }
        assertEquals(setOf(Numbers.TO_10, Numbers.TO_20, Numbers.RUNS, Numbers.SUMS), offered)
        assertEquals((0..20).toSet(), course.pictures.values.mapNotNull { (it as? Picture.Glyph)?.text?.toIntOrNull() }.toSet())
    }

    @Test
    fun `Mój, moja, moje asks only nouns that have been learned, under words for my that have been`() {
        var opened = false
        (1..course.path.size).forEach { days ->
            val learned = course.path.take(days).flatMap { it.itemIds() }.toSet()
            val practice = practiceAfter(days)
            if (practice.view().games.single { it.game == Game.MOJ_MOJA_MOJE }.state != GameState.READY) return@forEach
            opened = true
            practice.setup(Game.MOJ_MOJA_MOJE).choices.single().options.forEach { clue ->
                val round = practice.start(practice.setup(Game.MOJ_MOJA_MOJE, listOf(clue.id))) as GenderRound
                val about = "After $days Daily Sets, ${clue.id}"
                assertTrue(round.buckets.size >= 2, about)
                assertTrue(round.buckets.all { bucket -> course.possessives.getValue(bucket.gender) in learned }, about)
                round.run.questions.forEach { question ->
                    assertTrue(question.itemId in learned, about)
                    assertEquals(course.genders[question.itemId], question.gender, about)
                    assertTrue(round.buckets.any { it.gender == question.gender }, "$about: $question")
                    if (round.clue == NounClue.LISTEN) assertTrue(practice.clipOf(question.polish) != null, "$about: no Clip for ${question.polish}")
                }
            }
        }
        assertTrue(opened)
        // By the end all three words for "my" are there to sort under.
        val last = practiceAfter(course.path.size)
        assertEquals(3, (last.start(last.setup(Game.MOJ_MOJA_MOJE)) as GenderRound).buckets.size)
        assertEquals(Gender.entries.toSet(), course.genders.values.toSet())
    }

    @Test
    fun `Spell it spells only words that have been learned, from every deck it offers`() {
        (1..course.path.size).forEach { days ->
            val learned = course.path.take(days).flatMap { it.itemIds() }.toSet()
            val practice = practiceAfter(days)
            if (practice.view().games.single { it.game == Game.SPELL_IT }.state != GameState.READY) return@forEach
            practice.setup(Game.SPELL_IT).choices[0].options.forEach { from ->
                practice.setup(Game.SPELL_IT, listOf(from.id)).choices[1].options.forEach { deck ->
                    val round = practice.start(practice.setup(Game.SPELL_IT, listOf(from.id, deck.id))) as SpellRound
                    val about = "After $days Daily Sets, ${from.id} ${deck.id}"
                    assertTrue(round.total in SpellIt.LEAST..SpellIt.WORDS, about)
                    round.words.forEach { word ->
                        assertTrue(word.itemId in learned, about)
                        assertEquals(course.itemsById.getValue(word.itemId).text, word.polish, about)
                        assertEquals(word.letters.sorted(), word.tiles.sorted(), about)
                        assertTrue(word.polish.length in SpellIt.SHORTEST..SpellIt.LONGEST && ' ' !in word.polish, "$about: ${word.polish}")
                        assertEquals(round.pictures, word.picture != null, "$about: ${word.polish}")
                    }
                    // No two words of a Round are given the same way.
                    val given = round.words.map { it.picture ?: it.meaning }
                    assertEquals(given.size, given.toSet().size, about)
                }
            }
        }
        assertEquals(GameState.READY, practiceAfter(course.path.size).view().games.single { it.game == Game.SPELL_IT }.state)
    }

    @Test
    fun `the Quick round shows only words that have been learned, and a card that does not match shows another word's side`() {
        (1..course.path.size).forEach { days ->
            val learned = course.path.take(days).flatMap { it.itemIds() }.toSet()
            val practice = practiceAfter(days)
            if (practice.view().games.single { it.game == Game.QUICK_ROUND }.state != GameState.READY) return@forEach
            practice.setup(Game.QUICK_ROUND).choices[0].options.forEach { show ->
                val round = practice.start(practice.setup(Game.QUICK_ROUND, listOf(show.id, "seconds-60"))) as QuickRound
                val about = "After $days Daily Sets, ${show.id}"
                round.cards.forEach { card ->
                    assertTrue(card.itemId in learned, about)
                    val own = if (round.pictures) course.pictures[card.itemId] else card.meaning
                    val shown = if (round.pictures) checkNotNull(card.shownPicture) else card.shownMeaning
                    // A card that says "no match" never shows the word's own side under another name.
                    assertEquals(card.matches, shown == own, "$about: $card")
                }
            }
        }
        assertEquals(GameState.READY, practiceAfter(course.path.size).view().games.single { it.game == Game.QUICK_ROUND }.state)
    }

    @Test
    fun `every picture is of a word that can be pictured without knowing Polish, once`() {
        // A numeral stands for its number and nothing else.
        val numerals = course.pictures.filterValues { it is Picture.Glyph }
        numerals.forEach { (item, picture) ->
            val text = (picture as Picture.Glyph).text
            assertTrue(text == "+" || text.toIntOrNull() != null, "$item is drawn as \"$text\"")
        }
        assertEquals(numerals.size, numerals.values.toSet().size)
    }
}
