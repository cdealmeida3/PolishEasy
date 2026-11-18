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
            val words = script.items.flatMap { id -> itemsById.getValue(id).spellings.flatMap(::words) }.map { it.lowercase() }.toSet()
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
        assertEquals(setOf(Numbers.TO_10, Numbers.TO_20, Numbers.TO_100, Numbers.RUNS, Numbers.SUMS), offered)
        assertEquals((0..20).toSet() + (30..100 step 10) + (200..1000 step 100), course.pictures.values.mapNotNull { (it as? Picture.Glyph)?.text?.toIntOrNull() }.toSet())
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
    fun `Ten, ta, to opens with chapter 3's ten and ta, and sorts only learned nouns under learned words for this`() {
        val firstOfTen = course.path.indexOfFirst { dailySet -> dailySet.id == "kpk1-l03-c1" }
        assertTrue(firstOfTen > 0)
        assertEquals(GameState.NEEDS_MORE, practiceAfter(firstOfTen).view().games.single { it.game == Game.TEN_TA_TO }.state)
        (firstOfTen + 1..course.path.size).forEach { days ->
            val learned = course.path.take(days).flatMap { it.itemIds() }.toSet()
            val practice = practiceAfter(days)
            assertEquals(GameState.READY, practice.view().games.single { it.game == Game.TEN_TA_TO }.state, "After $days Daily Sets")
            practice.setup(Game.TEN_TA_TO).choices.single().options.forEach { clue ->
                val round = practice.start(practice.setup(Game.TEN_TA_TO, listOf(clue.id))) as GenderRound
                val about = "After $days Daily Sets, ${clue.id}"
                assertEquals(listOf("ten", "ta", "to"), round.buckets.map { it.word }, about)
                round.run.questions.forEach { question ->
                    assertTrue(question.itemId in learned, about)
                    assertEquals(course.genders[question.itemId], question.gender, about)
                }
            }
        }
    }

    @Test
    fun `Label the picture labels only learned words with an image, six that look and read unalike, from every deck`() {
        var opened = false
        (1..course.path.size).forEach { days ->
            val learned = course.path.take(days).flatMap { it.itemIds() }.toSet()
            val practice = practiceAfter(days)
            if (practice.view().games.single { it.game == Game.LABEL_IT }.state != GameState.READY) return@forEach
            opened = true
            practice.setup(Game.LABEL_IT).choices[1].options.forEach { deck ->
                listOf("choose", "type").forEach { way ->
                    val round = practice.start(practice.setup(Game.LABEL_IT, listOf(way, deck.id))) as LabelRound
                    val about = "After $days Daily Sets, ${deck.id}"
                    assertEquals(LabelIt.SPOTS, round.spots.size, about)
                    round.spots.forEach { spot ->
                        assertTrue(spot.itemId in learned, about)
                        assertTrue(spot.picture is Picture.Image, about)
                        assertEquals(course.itemsById.getValue(spot.itemId).text, spot.polish, about)
                    }
                    assertEquals(round.spots.size, round.spots.map { it.picture }.toSet().size, about)
                    assertEquals(round.spots.size, round.spots.map { it.polish.lowercase() }.toSet().size, about)
                    var played = round
                    while (!played.finished) {
                        played = if (played.typed) played.type(checkNotNull(played.spot).polish) else played.choose(played.labels.indexOf(played.run.current))
                        played = played.next()
                    }
                    assertEquals(Score(LabelIt.SPOTS, of = LabelIt.SPOTS), played.score, about)
                }
            }
        }
        assertTrue(opened)
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
                        // Spelled aloud, every letter is named, and every name has a Clip.
                        if (round.aloud) {
                            assertEquals(word.letters.map { course.letterNames.getValue(it) }, word.spelled, about)
                            word.spelled.forEach { name -> assertTrue(course.clipFor(name) != null, "$about: no Clip of $name") }
                        }
                    }
                    // No two words of a Round are given the same way.
                    val given = round.words.map { it.picture ?: it.meaning }
                    assertEquals(given.size, given.toSet().size, about)
                }
            }
        }
        assertEquals(GameState.READY, practiceAfter(course.path.size).view().games.single { it.game == Game.SPELL_IT }.state)
        // Every letter of Polish has a name to be spelled by, so the whole Path can be spelled aloud.
        assertEquals("aąbcćdeęfghijklłmnńoóprsśtuwyzźż".map { it.toString() }.toSet(), course.letterNames.keys)
        assertTrue("aloud" in practiceAfter(course.path.size).setup(Game.SPELL_IT).choices[0].options.map { it.id })
    }

    @Test
    fun `Ankieta's people say only their Items' words, open in chapter 2, and every Round plays out heard and read`() {
        assertTrue(course.ankieta.people.size >= 3)
        val itemsById = course.itemsById
        course.ankieta.people.forEach { person ->
            // Their own name is the one word no Item has.
            val words = (person.items.flatMap { id -> itemsById.getValue(id).spellings.flatMap(::words) } + person.answers["imie"].orEmpty()).map { it.lowercase() }.toSet()
            person.says.map { it.text }.forEach { line ->
                assertEquals(emptySet(), words(line).map { it.lowercase() }.toSet() - words, "Words of \"$line\" of ${person.id} that none of their Items has")
            }
            assertTrue(person.answers.keys.containsAll(course.ankieta.fields.map { it.id }), "${person.id} leaves a field blank")
        }
        val chapter2 = course.levels.flatMap { it.units }.single { it.id == "kpk1-l02" }.dailySets
        val before = course.path.indexOfFirst { it.id == chapter2.first().id }
        val days = course.path.indexOfFirst { it.id == chapter2.last().id } + 1
        assertEquals(GameState.NEEDS_MORE, practiceAfter(before).view().games.single { it.game == Game.ANKIETA }.state)
        val practice = practiceAfter(days)
        assertEquals(GameState.READY, practice.view().games.single { it.game == Game.ANKIETA }.state)
        listOf("listen", "read").forEach { way ->
            repeat(5) {
                var round = practice.start(practice.setup(Game.ANKIETA, listOf(way))) as AnkietaRound
                round.run.questions.forEach { question ->
                    assertTrue(question.options.size >= 3, "$question")
                    assertEquals(question.options.size, question.options.map { it.lowercase() }.distinct().size, "$question")
                }
                while (!round.finished) round = round.choose(checkNotNull(round.question).answer).next()
                assertEquals(round.total, round.score.value)
                assertEquals(15, round.total)
            }
        }
    }

    @Test
    fun `Register sort sorts only learned phrases the book marks, by the register it marks them with`() {
        (1..course.path.size).forEach { days ->
            val learned = course.path.take(days).flatMap { it.itemIds() }.toSet()
            val practice = practiceAfter(days)
            if (practice.view().games.single { it.game == Game.REGISTER_SORT }.state != GameState.READY) return@forEach
            val round = practice.start(practice.setup(Game.REGISTER_SORT, listOf("seconds-60"))) as RegisterRound
            val about = "After $days Daily Sets"
            round.cards.forEach { card ->
                assertTrue(card.itemId in learned, about)
                assertEquals(course.itemsById.getValue(card.itemId).register, card.register, about)
                assertTrue(card.register in round.registers, about)
            }
        }
        val all = practiceAfter(course.path.size).start(practiceAfter(course.path.size).setup(Game.REGISTER_SORT, listOf("seconds-60"))) as RegisterRound
        assertEquals(Register.entries, all.registers)
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
    fun `Opposites snap opens with three learned pairs and shows only learned adjectives, in one gender, opposites only when a pair says so`() {
        val pairs = course.opposites.flatMap { (one, other) -> listOf(one to other, other to one) }.toSet()
        fun idOf(form: String) = course.items.single { form in it.spellings && it.forms.isNotEmpty() }.id
        var opened = false
        (1..course.path.size).forEach { days ->
            val learned = course.path.take(days).flatMap { it.itemIds() }.toSet()
            val known = course.opposites.count { (one, other) -> one in learned && other in learned }
            val practice = practiceAfter(days)
            val state = practice.view().games.single { it.game == Game.OPPOSITES_SNAP }.state
            assertEquals(if (known >= 3) GameState.READY else GameState.NEEDS_MORE, state, "After $days Daily Sets")
            if (state != GameState.READY) return@forEach
            opened = true
            val round = practice.start(practice.setup(Game.OPPOSITES_SNAP, listOf("seconds-60"))) as OppositesRound
            round.cards.forEach { card ->
                val about = "After $days Daily Sets: $card"
                val (word, shown, opposite) = listOf(card.word, card.shown, card.opposite).map(::idOf)
                assertTrue(word in learned && shown in learned && opposite in learned, about)
                assertEquals(card.opposites, (word to shown) in pairs, about)
                assertTrue((word to opposite) in pairs, about)
                val forms = listOf(card.word, card.shown, card.opposite).map { form -> course.itemsById.getValue(idOf(form)).spellings.indexOf(form) }
                assertEquals(1, forms.toSet().size, about)
            }
        }
        assertTrue(opened)
    }

    @Test
    fun `Who's who opens after kim with four learned jobs, and asks each job's instrumental among learned jobs`() {
        val afterKim = course.path.indexOfFirst { dailySet -> "kim" in dailySet.itemIds() } + 1
        assertTrue(afterKim > 0)
        assertEquals(GameState.NEEDS_MORE, practiceAfter(afterKim - 1).view().games.single { it.game == Game.WHOS_WHO }.state)
        (afterKim..course.path.size).forEach { days ->
            val learned = course.path.take(days).flatMap { it.itemIds() }.toSet()
            val practice = practiceAfter(days)
            assertEquals(GameState.READY, practice.view().games.single { it.game == Game.WHOS_WHO }.state, "After $days Daily Sets")
            practice.setup(Game.WHOS_WHO).choices.single().options.forEach { clue ->
                val round = practice.start(practice.setup(Game.WHOS_WHO, listOf(clue.id))) as WhoRound
                round.run.questions.forEach { question ->
                    val about = "After $days Daily Sets, ${clue.id}: $question"
                    val job = course.itemsById.getValue(question.itemId)
                    assertTrue(job.id in learned, about)
                    assertEquals(job.forms.first(), question.answer, about)
                    assertEquals(WhosWho.OPTIONS, question.options.size, about)
                    question.options.forEach { option -> assertTrue(course.items.any { it.id in learned && option in it.spellings }, about) }
                    if (clue.id == WhosWho.PICTURE) assertTrue(question.picture != null, about)
                }
            }
        }
    }

    @Test
    fun `Three groups sorts every learned verb with a table, and after chapter 6 offers all three groups`() {
        val all = practiceAfter(course.path.size)
        assertEquals(GameState.READY, all.view().games.single { it.game == Game.VERB_GROUPS }.state)
        val round = all.start(all.setup(Game.VERB_GROUPS, listOf(VerbGroups.WITH_JA))) as VerbGroupRound
        assertEquals(VerbGroup.entries, round.groups)
        val tables = course.path.flatMap { it.tips }.mapNotNull { it.conjugation }.associateBy { it.verb }
        round.run.questions.forEach { question ->
            val table = tables.getValue(question.verb)
            assertEquals(VerbGroup.of(table.group), question.group, "$question")
            assertEquals(table.forms[Person.I], question.ja)
        }
        val sixth = course.path.filter { it.id.startsWith("kpk1-l06-") }.flatMap { it.tips }.mapNotNull { it.conjugation }
            .associate { it.verb to VerbGroup.of(it.group) }
        assertEquals(VerbGroup.M_SZ, sixth["znać"])
        assertEquals(VerbGroup.M_SZ, sixth["umieć"])
        assertEquals(VerbGroup.E_ESZ, sixth["pisać"])
        assertEquals(VerbGroup.E_ESZ, sixth["gotować"])
        assertEquals(VerbGroup.E_ISZ, sixth["tańczyć"])
        assertEquals(VerbGroup.E_ISZ, sixth["jeździć"])
    }

    @Test
    fun `Hobby charades opens in chapter 6 and asks learned hobbies by a picture, among learned hobbies`() {
        val firstReady = (1..course.path.size).first { days -> practiceAfter(days).view().games.single { it.game == Game.HOBBY_CHARADES }.state == GameState.READY }
        assertTrue(course.path[firstReady - 1].id.startsWith("kpk1-l06-"), course.path[firstReady - 1].id)
        val all = practiceAfter(course.path.size)
        val hobbies = course.hobbies.map { hobby -> hobby.joinToString(" ") { course.itemsById.getValue(it).text } }.toSet()
        listOf(HobbyCharades.CHOOSE, HobbyCharades.TYPE).forEach { answer ->
            val round = all.start(all.setup(Game.HOBBY_CHARADES, listOf(answer))) as HobbyRound
            assertEquals(HobbyCharades.QUESTIONS, round.total)
            round.run.questions.forEach { question ->
                assertTrue(question.answer in hobbies, "$question")
                assertTrue(hobbies.containsAll(question.options), "$question")
            }
            assertEquals(round.total, round.run.questions.map { it.picture }.toSet().size)
        }
    }

    @Test
    fun `Kiosk till opens in chapter 7 and asks prices said in learned words, with the money in the form that goes after the number`() {
        val firstReady = (1..course.path.size).first { days -> practiceAfter(days).view().games.single { it.game == Game.KIOSK_TILL }.state == GameState.READY }
        assertTrue(course.path[firstReady - 1].id.startsWith("kpk1-l07-"), course.path[firstReady - 1].id)
        val learned = course.path.flatMap { it.itemIds() }.mapNotNull(course.itemsById::get).flatMap { listOf(it.text) + it.forms }.toSet()
        val all = practiceAfter(course.path.size)
        val offered = mutableSetOf<String>()
        all.setup(Game.KIOSK_TILL).choices[0].options.forEach { clue ->
            all.setup(Game.KIOSK_TILL, listOf(clue.id)).choices[1].options.forEach { level ->
                offered += "${clue.id}:${level.id}"
                val round = all.start(all.setup(Game.KIOSK_TILL, listOf(clue.id, level.id))) as TillRound
                assertEquals(KioskTill.QUESTIONS, round.total)
                round.run.questions.forEach { question ->
                    assertTrue(learned.containsAll(question.words), "$question")
                    if (round.listen) question.words.forEach { assertTrue(all.clipOf(it) != null, "No Clip for $it") }
                    val zloty = question.price / 100
                    assertTrue(if (level.id == KioskTill.COINS) zloty in 1..9 else zloty in 10..99, "$question")
                    assertEquals(0, question.price % 10)
                    val money = question.words.first { it.startsWith("złot") }
                    assertEquals(
                        when {
                            zloty == 1 -> "złoty"
                            zloty % 10 in 2..4 && zloty % 100 !in 12..14 -> "złote"
                            else -> "złotych"
                        },
                        money,
                        question.polish,
                    )
                    if (question.price % 100 != 0) assertEquals("groszy", question.words.last(), question.polish)
                }
            }
        }
        assertEquals(setOf("listen:coins", "listen:notes", "read:coins", "read:notes"), offered)
    }

    @Test
    fun `Lotto opens in chapter 7 with the hundreds, and calls only learned numbers that are on its card`() {
        val firstReady = (1..course.path.size).first { days -> practiceAfter(days).view().games.single { it.game == Game.LOTTO }.state == GameState.READY }
        assertTrue(course.path[firstReady - 1].id.startsWith("kpk1-l07-"), course.path[firstReady - 1].id)
        val learned = course.path.flatMap { it.itemIds() }.mapNotNull(course.itemsById::get).map { it.text }.toSet()
        val all = practiceAfter(course.path.size)
        listOf(Lotto.LISTEN, Lotto.READ).forEach { clue ->
            assertEquals(listOf(Lotto.HUNDREDS, Lotto.TO_1000), all.setup(Game.LOTTO, listOf(clue)).choices[1].options.map { it.id })
            listOf(Lotto.HUNDREDS, Lotto.TO_1000).forEach { level ->
                val round = all.start(all.setup(Game.LOTTO, listOf(clue, level))) as LottoRound
                assertEquals(Lotto.CARD, round.card.size)
                assertEquals(round.card.sorted(), round.card)
                assertTrue(round.card.all { it in 100..1000 }, "${round.card}")
                assertEquals(Lotto.CALLS, round.total)
                round.run.questions.forEach { question ->
                    assertTrue(question.number in round.card)
                    assertTrue(learned.containsAll(question.words), "$question")
                    if (round.listen) question.words.forEach { assertTrue(all.clipOf(it) != null, "No Clip for $it") }
                    if (level == Lotto.HUNDREDS) assertEquals(0, question.number % 100)
                }
            }
        }
    }

    @Test
    fun `Plate builder opens in chapter 8 and orders learned dishes whose foods are all on the tray`() {
        val firstReady = (1..course.path.size).first { days -> practiceAfter(days).view().games.single { it.game == Game.PLATE_BUILDER }.state == GameState.READY }
        assertTrue(course.path[firstReady - 1].id.startsWith("kpk1-l08-"), course.path[firstReady - 1].id)
        val learned = course.path.flatMap { it.itemIds() }.mapNotNull(course.itemsById::get).map { it.text }.toSet()
        val all = practiceAfter(course.path.size)
        listOf(PlateBuilder.LISTEN, PlateBuilder.READ).forEach { clue ->
            assertEquals(listOf(PlateBuilder.ONE, PlateBuilder.TWO), all.setup(Game.PLATE_BUILDER, listOf(clue)).choices[1].options.map { it.id })
            listOf(PlateBuilder.ONE, PlateBuilder.TWO).forEach { level ->
                val round = all.start(all.setup(Game.PLATE_BUILDER, listOf(clue, level))) as PlateRound
                assertEquals(PlateBuilder.QUESTIONS, round.total)
                round.run.questions.forEach { question ->
                    assertTrue(learned.containsAll(question.words), "$question")
                    if (round.listen) question.words.forEach { assertTrue(all.clipOf(it) != null, "No Clip for $it") }
                    assertEquals(if (level == PlateBuilder.ONE) 2 else 4, question.wanted.size, question.polish)
                    assertTrue(question.tray.map { it.id }.containsAll(question.wanted), question.polish)
                    assertEquals(question.tray.size, question.tray.map { it.picture }.toSet().size, question.polish)
                }
            }
        }
    }

    @Test
    fun `Market list opens in chapter 9 and asks for learned goods, the number agreeing, every one on the stall`() {
        val firstReady = (1..course.path.size).first { days -> practiceAfter(days).view().games.single { it.game == Game.MARKET_LIST }.state == GameState.READY }
        assertTrue(course.path[firstReady - 1].id.startsWith("kpk1-l09-"), course.path[firstReady - 1].id)
        val market = checkNotNull(course.market)
        val all = practiceAfter(course.path.size)
        listOf(MarketList.LISTEN, MarketList.READ).forEach { clue ->
            assertEquals(listOf(MarketList.ONE, MarketList.TWO), all.setup(Game.MARKET_LIST, listOf(clue)).choices[1].options.map { it.id })
            listOf(MarketList.ONE, MarketList.TWO).forEach { level ->
                val round = all.start(all.setup(Game.MARKET_LIST, listOf(clue, level))) as MarketRound
                assertEquals(MarketList.QUESTIONS, round.total)
                round.run.questions.forEach { question ->
                    if (round.listen) question.words.forEach { assertTrue(all.clipOf(it) != null, "No Clip for $it") }
                    question.wanted.forEach { (id, count) ->
                        val good = course.itemsById.getValue(id)
                        val number = course.itemsById.getValue(market.numbers[count - 1].getValue(course.genders.getValue(id))).text
                        val noun = if (count == 1) good.text else good.forms.single()
                        assertTrue(question.polish.lowercase().contains("$number $noun"), question.polish)
                    }
                    assertTrue(question.stall.map { it.id }.containsAll(question.wanted.keys), question.polish)
                    assertEquals(question.stall.size, question.stall.map { it.picture }.toSet().size, question.polish)
                }
            }
        }
    }

    @Test
    fun `Sort the aisles opens in chapter 9 and deals products into their aisles`() {
        val firstReady = (1..course.path.size).first { days -> practiceAfter(days).view().games.single { it.game == Game.AISLE_SORT }.state == GameState.READY }
        assertTrue(course.path[firstReady - 1].id.startsWith("kpk1-l09-"), course.path[firstReady - 1].id)
        val all = practiceAfter(course.path.size)
        repeat(5) {
            val round = all.start(all.setup(Game.AISLE_SORT, listOf("${AisleSort.SECONDS_PREFIX}60"))) as AisleRound
            assertEquals(AisleSort.AISLES, round.aisles.size)
            round.cards.forEach { card -> assertTrue(card.itemId in course.aisles.single { it.names == card.aisle }.products, card.polish) }
        }
    }

    @Test
    fun `Take the order opens in chapter 10 and serves tables of learned dishes, every one on the menu`() {
        val firstReady = (1..course.path.size).first { days -> practiceAfter(days).view().games.single { it.game == Game.TAKE_THE_ORDER }.state == GameState.READY }
        assertTrue(course.path[firstReady - 1].id.startsWith("kpk1-l10-"), course.path[firstReady - 1].id)
        val all = practiceAfter(course.path.size)
        assertEquals(listOf(TakeTheOrder.LISTEN, TakeTheOrder.READ), all.setup(Game.TAKE_THE_ORDER).choices[0].options.map { it.id })
        listOf(TakeTheOrder.LISTEN, TakeTheOrder.READ).forEach { clue ->
            listOf(TakeTheOrder.TWO, TakeTheOrder.THREE).forEach { guests ->
                val round = all.start(all.setup(Game.TAKE_THE_ORDER, listOf(clue, guests))) as TableRound
                assertEquals(TakeTheOrder.QUESTIONS, round.total)
                round.run.questions.forEach { question ->
                    if (round.listen) question.lines.flatten().forEach { assertTrue(all.clipOf(it) != null, "No Clip for $it") }
                    assertEquals(if (guests == TakeTheOrder.TWO) 2 else 3, question.dishes.size)
                    assertEquals(TakeTheOrder.MENU, question.menu.size)
                    assertTrue(question.menu.map { it.id }.containsAll(question.dishes), question.key)
                }
            }
        }
    }

    @Test
    fun `Negation flip opens in chapter 10, and every sentence's wrong option keeps its object`() {
        val firstReady = (1..course.path.size).first { days -> practiceAfter(days).view().games.single { it.game == Game.NEGATION_FLIP }.state == GameState.READY }
        assertTrue(course.path[firstReady - 1].id.startsWith("kpk1-l10-"), course.path[firstReady - 1].id)
        course.negations.forEach { negation ->
            assertTrue(negation.kept != negation.no, negation.yes)
            assertTrue(negation.negatedAt >= 0, negation.no)
            assertTrue(" nie " in " ${negation.kept.lowercase()}", negation.kept)
        }
        val all = practiceAfter(course.path.size)
        val round = all.start(all.setup(Game.NEGATION_FLIP, listOf("${NegationFlip.SECONDS_PREFIX}60"))) as FlipRound
        assertEquals(course.negations.map { it.yes }.toSet(), round.cards.map { it.yes }.toSet())
        round.cards.forEach { card -> assertTrue(all.clipOf(card.no) != null, "No Clip for ${card.no}") }
    }

    @Test
    fun `Family tree opens in chapter 11 with every relative, each with my in its gender and a Clip`() {
        val firstReady = (1..course.path.size).first { days -> practiceAfter(days).view().games.single { it.game == Game.FAMILY_TREE }.state == GameState.READY }
        assertTrue(course.path[firstReady - 1].id.startsWith("kpk1-l11-"), course.path[firstReady - 1].id)
        val all = practiceAfter(course.path.size)
        listOf(FamilyTree.NAME, FamilyTree.FIND).forEach { way ->
            val round = all.start(all.setup(Game.FAMILY_TREE, listOf(way))) as TreeRound
            assertEquals(Relative.entries.size, round.total)
            round.people.forEach { person -> assertTrue(all.clipOf(person.phrase) != null, "No Clip for ${person.phrase}") }
        }
        assertEquals(setOf("mój dziadek", "moja babcia", "mój wujek", "moja ciocia", "mój tata", "moja mama", "mój kuzyn", "moja kuzynka", "mój brat", "moja siostra"), course.familyPhrases().toSet())
    }

    @Test
    fun `Times of day opens in chapter 11 and deals every learned time at its hours`() {
        val firstReady = (1..course.path.size).first { days -> practiceAfter(days).view().games.single { it.game == Game.TIMES_OF_DAY }.state == GameState.READY }
        assertTrue(course.path[firstReady - 1].id.startsWith("kpk1-l11-"), course.path[firstReady - 1].id)
        val all = practiceAfter(course.path.size)
        val round = all.start(all.setup(Game.TIMES_OF_DAY, listOf("${TimesOfDay.SECONDS_PREFIX}60"))) as ClockRound
        assertEquals(course.timesOfDay.map { it.item }.toSet(), round.cards.map { it.key }.toSet())
        round.cards.forEach { card ->
            assertTrue(card.hour in course.timesOfDay.single { it.item == card.key }.hours)
            card.options.forEach { option -> assertTrue(all.clipOf(option) != null, "No Clip for $option") }
        }
    }

    @Test
    fun `Tell the time opens in chapter 12 and says every time on its clock, each with a Clip`() {
        val firstReady = (1..course.path.size).first { days -> practiceAfter(days).view().games.single { it.game == Game.TELL_THE_TIME }.state == GameState.READY }
        assertTrue(course.path[firstReady - 1].id.startsWith("kpk1-l12-"), course.path[firstReady - 1].id)
        val all = practiceAfter(course.path.size)
        listOf(TellTheTime.READ, TellTheTime.FIND).forEach { way ->
            val round = all.start(all.setup(Game.TELL_THE_TIME, listOf(way))) as TimeRound
            assertEquals(TellTheTime.QUESTIONS, round.total)
            round.run.questions.flatMap { it.options }.forEach { time -> assertTrue(all.clipOf(time.polish) != null, "No Clip for ${time.polish}") }
        }
        assertEquals(12 * CLOCK_MINUTES.size, course.clockPhrases().size)
        assertTrue("wpół do dziewiątej" in course.clockPhrases())
        assertTrue("za kwadrans dziewiąta" in course.clockPhrases())
    }

    @Test
    fun `Week planner opens in chapter 12, every plan's words are its Items' and every plan has a Clip`() {
        val firstReady = (1..course.path.size).first { days -> practiceAfter(days).view().games.single { it.game == Game.WEEK_PLANNER }.state == GameState.READY }
        assertTrue(course.path[firstReady - 1].id.startsWith("kpk1-l12-"), course.path[firstReady - 1].id)
        val week = checkNotNull(course.week)
        assertEquals(7, week.days.size)
        week.plans.forEach { plan ->
            val words = plan.items.mapNotNull(course.itemsById::get).flatMap { listOf(it.text) + it.forms }
                .flatMap { it.lowercase().split(" ") }.toSet()
            plan.text.lowercase().split(" ", ":", ".").filter { it.isNotBlank() }.forEach { word -> assertTrue(word in words, "${plan.text}: $word is not one of its Items") }
            assertTrue(course.clipFor(plan.text) != null, "No Clip for ${plan.text}")
        }
        val all = practiceAfter(course.path.size)
        assertEquals(listOf(WeekPlanner.LISTEN, WeekPlanner.READ), all.setup(Game.WEEK_PLANNER).choices[0].options.map { it.id })
        val round = all.start(all.setup(Game.WEEK_PLANNER, listOf(WeekPlanner.LISTEN))) as PlannerRound
        assertEquals(WeekPlanner.PLANS, round.total)
    }

    /** The words of [text], lower case, without its punctuation. */
    private fun wordsOf(text: String): List<String> = text.lowercase().split(Regex("[^\\p{L}]+")).filter { it.isNotEmpty() }

    /** Every word the Items [ids] can say, in their text or a form. */
    private fun wordsOfItems(ids: List<String>): Set<String> =
        ids.mapNotNull(course.itemsById::get).flatMap { listOf(it.text) + it.forms }.flatMap(::wordsOf).toSet()

    @Test
    fun `Story strip opens in chapter 13, every sentence's words are its Items', and every story is told with a Clip`() {
        val firstReady = (1..course.path.size).first { days -> practiceAfter(days).view().games.single { it.game == Game.STORY_STRIP }.state == GameState.READY }
        assertTrue(course.path[firstReady - 1].id.startsWith("kpk1-l13-"), course.path[firstReady - 1].id)
        assertTrue(course.stories.size >= StoryStrip.STORIES)
        course.stories.forEach { story ->
            story.panels.forEach { panel ->
                val words = wordsOfItems(panel.items)
                wordsOf(panel.text).forEach { word -> assertTrue(word in words, "${panel.text}: $word is not one of its Items") }
                assertTrue(panel.picture in course.pictures, "${panel.text} has no Picture")
            }
            assertTrue(course.clipFor(story.text) != null, "No Clip for ${story.text}")
        }
        val all = practiceAfter(course.path.size)
        assertEquals(listOf(StoryStrip.LISTEN, StoryStrip.READ), all.setup(Game.STORY_STRIP).choices[0].options.map { it.id })
        val round = all.start(all.setup(Game.STORY_STRIP, listOf(StoryStrip.LISTEN))) as StoryRound
        assertEquals(StoryStrip.STORIES, round.stories.size)
    }

    @Test
    fun `Directions opens in chapter 14, every route leads to a block of the map, its words its Items', with a Clip`() {
        val firstReady = (1..course.path.size).first { days -> practiceAfter(days).view().games.single { it.game == Game.DIRECTIONS }.state == GameState.READY }
        assertTrue(course.path[firstReady - 1].id.startsWith("kpk1-l14-"), course.path[firstReady - 1].id)
        assertEquals(CityMap.blocks.toSet(), course.routes.mapNotNull { it.end }.toSet())
        course.routes.forEach { route ->
            val words = wordsOfItems(route.items)
            wordsOf(route.text).forEach { word -> assertTrue(word in words, "${route.text}: $word is not one of its Items") }
            assertTrue(course.clipFor(route.text) != null, "No Clip for ${route.text}")
        }
        course.places.forEach { assertTrue(it in course.pictures, "The place $it has no Picture") }
        val all = practiceAfter(course.path.size)
        val round = all.start(all.setup(Game.DIRECTIONS, listOf(Directions.LISTEN))) as DirectionsRound
        assertEquals(Directions.QUESTIONS, round.total)
        assertEquals(CityMap.blocks, round.map.map { it.block })
    }

    @Test
    fun `Packaging match opens in chapter 15, every phrase's words its Items', with a Clip and three containers to offer as wrong`() {
        val firstReady = (1..course.path.size).first { days -> practiceAfter(days).view().games.single { it.game == Game.PACKAGING }.state == GameState.READY }
        assertTrue(course.path[firstReady - 1].id.startsWith("kpk1-l15-"), course.path[firstReady - 1].id)
        course.packaging.forEach { phrase ->
            val words = wordsOfItems(phrase.items)
            wordsOf(phrase.text).forEach { word -> assertTrue(word in words, "${phrase.text}: $word is not one of its Items") }
            assertTrue(course.clipFor(phrase.text) != null, "No Clip for ${phrase.text}")
        }
        val all = practiceAfter(course.path.size)
        val round = all.start(all.setup(Game.PACKAGING, emptyList())) as PackagingRound
        assertEquals(PackagingMatch.QUESTIONS, round.total)
        round.run.questions.forEach { assertEquals(PackagingMatch.OFFERED, it.offered.size, it.text) }
    }

    @Test
    fun `How many opens in chapter 15, every phrase's words its Items', counting a pictured Item, with a Clip`() {
        val firstReady = (1..course.path.size).first { days -> practiceAfter(days).view().games.single { it.game == Game.HOW_MANY }.state == GameState.READY }
        assertTrue(course.path[firstReady - 1].id.startsWith("kpk1-l15-"), course.path[firstReady - 1].id)
        course.counting.forEach { phrase ->
            val words = wordsOfItems(phrase.items)
            wordsOf(phrase.text).forEach { word -> assertTrue(word in words, "${phrase.text}: $word is not one of its Items") }
            assertTrue(phrase.item in course.pictures, "${phrase.text}: ${phrase.item} has no Picture")
            assertTrue(course.clipFor(phrase.text) != null, "No Clip for ${phrase.text}")
        }
        val all = practiceAfter(course.path.size)
        val round = all.start(all.setup(Game.HOW_MANY, emptyList())) as HowManyRound
        assertEquals(HowMany.QUESTIONS, round.total)
        round.run.questions.forEach { assertEquals(3, it.offered.size, it.text) }
    }

    @Test
    fun `Pronoun swap opens in chapter 14, every sentence's words and its shorter one's are its Items', each with a Clip`() {
        val firstReady = (1..course.path.size).first { days -> practiceAfter(days).view().games.single { it.game == Game.PRONOUN_SWAP }.state == GameState.READY }
        assertTrue(course.path[firstReady - 1].id.startsWith("kpk1-l14-"), course.path[firstReady - 1].id)
        assertEquals((Pronouns.ALONE + Pronouns.AFTER_NA).toSet(), course.swaps.map { it.pronoun }.toSet())
        course.swaps.forEach { swap ->
            val words = wordsOfItems(swap.items)
            (wordsOf(swap.text) + wordsOf(swap.short)).forEach { word -> assertTrue(word in words, "${swap.text}: $word is not one of its Items") }
            assertTrue(course.clipFor(swap.text) != null, "No Clip for ${swap.text}")
            assertTrue(course.clipFor(swap.short) != null, "No Clip for ${swap.short}")
        }
        val all = practiceAfter(course.path.size)
        val round = all.start(all.setup(Game.PRONOUN_SWAP, listOf(PronounSwap.LISTEN))) as SwapRound
        assertEquals(PronounSwap.QUESTIONS, round.total)
    }

    @Test
    fun `Who said it opens in chapter 13, with every speaker, every line's words its Items' and a Clip`() {
        val firstReady = (1..course.path.size).first { days -> practiceAfter(days).view().games.single { it.game == Game.WHO_SAID_IT }.state == GameState.READY }
        assertTrue(course.path[firstReady - 1].id.startsWith("kpk1-l13-"), course.path[firstReady - 1].id)
        assertEquals(Speaker.entries.toSet(), course.speakers.map { it.who }.toSet())
        course.speakers.forEach { line ->
            val words = wordsOfItems(line.items)
            wordsOf(line.text).forEach { word -> assertTrue(word in words, "${line.text}: $word is not one of its Items") }
            assertEquals(setOf(line.who), Speaker.heardIn(line.text), line.text)
            assertTrue(course.clipFor(line.text) != null, "No Clip for ${line.text}")
        }
        val all = practiceAfter(course.path.size)
        val round = all.start(all.setup(Game.WHO_SAID_IT, listOf(WhoSaidIt.LISTEN))) as SpeakerRound
        assertEquals(WhoSaidIt.QUESTIONS, round.total)
        assertEquals(Speaker.entries, round.speakers)
    }

    @Test
    fun `the Verb challenge offers chapter 4's conjugation once its verbs are learned`() {
        val afterB = course.path.indexOfFirst { it.id == "kpk1-l04-b3" } + 1
        assertTrue(afterB > 0)
        val verbs = practiceAfter(afterB).setup(Game.VERB_CHALLENGE).choices.first().options
        val group = verbs.single { it.id == "group:-ę, -isz/-ysz" }
        val round = practiceAfter(afterB).verbRound(group.id)
        val members = course.path.take(afterB).flatMap { it.tips }.mapNotNull { it.conjugation }.filter { it.group == "-ę, -isz/-ysz" }.map { it.verb }.toSet()
        assertTrue(setOf("chodzić", "myśleć", "uczyć się", "kończyć", "słyszeć", "robić").all { it in members }, "$members")
        assertTrue(round.run.questions.all { it.verb in members })
    }

    @Test
    fun `every picture is of a word that can be pictured without knowing Polish, once`() {
        // A numeral stands for its number and nothing else, and a sign for its sum: plus, minus, equals.
        val numerals = course.pictures.filterValues { it is Picture.Glyph }
        numerals.forEach { (item, picture) ->
            val text = (picture as Picture.Glyph).text
            assertTrue(text in setOf("+", "−", "=") || text.toIntOrNull() != null, "$item is drawn as \"$text\"")
        }
        assertEquals(numerals.size, numerals.values.toSet().size)
    }

    @Test
    fun `Forecaster opens in chapter 16, every forecast's words its Items', naming pictured weather, with a Clip`() {
        val firstReady = (1..course.path.size).first { days -> practiceAfter(days).view().games.single { it.game == Game.FORECASTER }.state == GameState.READY }
        assertTrue(course.path[firstReady - 1].id.startsWith("kpk1-l16-"), course.path[firstReady - 1].id)
        course.forecasts.forEach { forecast ->
            val words = wordsOfItems(forecast.items)
            wordsOf(forecast.text).forEach { word -> assertTrue(word in words, "${forecast.text}: $word is not one of its Items") }
            forecast.weather.forEach { assertTrue(it in course.pictures, "${forecast.text}: $it has no Picture") }
            assertTrue(course.clipFor(forecast.text) != null, "No Clip for ${forecast.text}")
        }
        val all = practiceAfter(course.path.size)
        val round = all.start(all.setup(Game.FORECASTER, listOf(Forecaster.READ))) as ForecasterRound
        assertEquals(Forecaster.QUESTIONS, round.total)
        round.run.questions.forEach { assertEquals(Forecaster.BOARD, it.board.size, it.text) }
    }

    @Test
    fun `Word search opens in chapter 16 and hides its words in every Round`() {
        val firstReady = (1..course.path.size).first { days -> practiceAfter(days).view().games.single { it.game == Game.WORD_SEARCH }.state == GameState.READY }
        assertTrue(course.path[firstReady - 1].id.startsWith("kpk1-l16-"), course.path[firstReady - 1].id)
        val all = practiceAfter(course.path.size)
        repeat(5) {
            val round = all.start(all.setup(Game.WORD_SEARCH, emptyList())) as WordSearchRound
            assertEquals(WordSearch.WORDS, round.total)
            round.words.forEach { word ->
                assertEquals(word.word.uppercase(), word.letters.joinToString("") { round.grid[it.row][it.column].toString() })
                assertTrue(course.clipFor(word.word) != null, "No Clip for ${word.word}")
            }
        }
    }
}
