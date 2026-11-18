package com.polisheasy.engine

/** Where a Polish text is written: the content file and the Item, Exercise or Tip. */
data class PolishUse(val file: String, val id: String, val role: PolishRole)

enum class PolishRole { ITEM, PROMPT, ANSWER, DISTRACTOR, TABLE, HEADING }

/** One Polish text with the English it stands for, or null where it stands for none (a distractor, a bare form). */
data class PolishSentence(val text: String, val meaning: String?, val uses: List<PolishUse>)

/** The content's claim that [form] is the form of [verb] for [person]. */
data class VerbForm(val verb: String, val person: Person, val form: String, val uses: List<PolishUse>)

/**
 * A Tip as the Learner reads it. [text] is its title, its body without its marks and its table's
 * forms, one per line. [polish] is each stretch of the body that is marked as Polish.
 */
data class TipText(val file: String, val id: String, val text: String, val polish: List<String> = emptyList())

/** Every Polish text in the Authored Content, for the Polish check. */
class PolishContent(
    /** One for each distinct pair of text and meaning, in the order the content first writes them. */
    val sentences: List<PolishSentence>,
    /** One for each distinct verb, person and form. */
    val verbForms: List<VerbForm>,
    val tips: List<TipText>,
    /** A listed file that could not be read. The Polish check does not run on half a course. */
    val unreadable: List<ContentProblem>,
    /** Every gap Sound gaps can ask, with the file of its Item. No wrong option of one may spell a real word. */
    val soundGaps: List<Pair<String, SoundGap>> = emptyList(),
    /** The words the content says a wrong option of Sound gaps would spell, and the file that lists them. */
    val realWords: List<String> = emptyList(),
    val practiceFile: String? = null,
    /** Each noun the content gives a gender, for the dictionary to confirm. */
    val nouns: List<GenderedWord> = emptyList(),
    /** Each word for "my" and the gender the content gives it. */
    val possessives: List<GenderedWord> = emptyList(),
    /** Each word for "this" and the gender the content gives it. */
    val demonstratives: List<GenderedWord> = emptyList(),
)

/** A word of Item [itemId] and the gender the practice file gives it. */
data class GenderedWord(val itemId: String, val text: String, val gender: Gender)

/**
 * Lists the Polish in the content: every text with the English it stands for and each place it is
 * written, every claim that a form is a verb's form for a person, and every Tip.
 */
fun polishContent(files: ContentFiles): PolishContent {
    val content = readContent(files::read)
    val language = BASE_LANGUAGES.first()

    val sentences = LinkedHashMap<Pair<String, String?>, MutableList<PolishUse>>()
    fun sentence(text: String, meaning: Localized?, use: PolishUse) {
        val polish = text.trim()
        if (polish.isEmpty()) return
        val english = meaning?.get(language)?.trim()?.takeIf { it.isNotEmpty() }
        sentences.getOrPut(polish to english) { mutableListOf() }.let { if (use !in it) it += use }
    }

    val claims = LinkedHashMap<Triple<String, Person, String>, MutableList<PolishUse>>()
    fun claim(verb: String, person: Person, form: String, use: PolishUse) {
        if (verb.isBlank() || form.isBlank()) return
        claims.getOrPut(Triple(verb.trim(), person, form.trim())) { mutableListOf() }.let { if (use !in it) it += use }
    }

    val itemsById = content.items.reversed().associateBy { it.value.id }
    content.items.forEach { (file, item) -> item.spellings.forEach { sentence(it, item.meaning, PolishUse(file, item.id, PolishRole.ITEM)) } }

    // A chapter's title and its K, L and G lists are quoted from the book's contents, abbreviations
    // and all, so they are not the app's Polish to check.
    content.instructions.forEach { (kind, instruction) ->
        val file = content.instructionsFile.orEmpty()
        sentence(instruction.polish, instruction.meaning, PolishUse(file, kind.name.lowercase(java.util.Locale.ROOT), PolishRole.PROMPT))
    }

    // The listening tasks are written for the app, so their Polish is checked like an Exercise's.
    content.listening.forEach { task ->
        val file = content.listeningFile.orEmpty()
        fun use(role: PolishRole) = PolishUse(file, task.id, role)
        when (task) {
            is TrueFalseTask -> sentence(task.statement, task.meaning, use(PolishRole.PROMPT))
            is ChooseTask -> {
                sentence(task.question, task.meaning, use(PolishRole.PROMPT))
                task.options.forEach { sentence(it, null, use(PolishRole.ANSWER)) }
            }
            is DictationTask -> {
                sentence(task.question, task.meaning, use(PolishRole.PROMPT))
                task.acceptedAnswers.forEach { sentence(it, null, use(PolishRole.ANSWER)) }
            }
        }
    }

    // A chat's lines and replies, each with what it means; a right reply's other answers mean the same.
    content.chats.forEach { (file, script) ->
        script.turns.forEach { turn ->
            val use = { role: PolishRole -> PolishUse(file, "${script.id}/${turn.id}", role) }
            sentence(turn.says, turn.meaning, use(PolishRole.PROMPT))
            turn.replies.forEach { reply ->
                sentence(reply.text, reply.meaning, use(if (reply.right) PolishRole.ANSWER else PolishRole.DISTRACTOR))
                if (reply.right) reply.answers.forEach { sentence(it, reply.meaning, use(PolishRole.ANSWER)) }
            }
        }
    }

    // Ankieta's fields and what its people say, each with what it means; the answers on the form are bare Polish.
    content.ankietaFile?.let { file ->
        content.ankieta.fields.forEach { field -> sentence(field.label, field.meaning, PolishUse(file, field.id, PolishRole.HEADING)) }
        content.ankieta.people.forEach { person ->
            person.says.forEach { sentence(it.text, it.meaning, PolishUse(file, person.id, PolishRole.PROMPT)) }
            person.answers.values.forEach { sentence(it, null, PolishUse(file, person.id, PolishRole.ANSWER)) }
            person.wrong.values.flatten().forEach { sentence(it, null, PolishUse(file, person.id, PolishRole.DISTRACTOR)) }
        }
    }

    // Negation flip's sentences and their negations, each with what it means. Its wrong option is
    // made from the sentence, so it is never written to be checked.
    content.practiceFile?.let { file ->
        content.practice?.negations.orEmpty().forEachIndexed { index, negation ->
            val use = { role: PolishRole -> PolishUse(file, "negations/${index + 1}", role) }
            sentence(negation.yes, negation.yesMeaning, use(PolishRole.PROMPT))
            sentence(negation.no, negation.noMeaning, use(PolishRole.ANSWER))
        }
        // Week planner's plans, and every time Tell the time says, with the time in digits as what it means.
        content.practice?.week?.plans.orEmpty().forEachIndexed { index, plan ->
            sentence(plan.text, plan.meaning, PolishUse(file, "week/plans/${index + 1}", PolishRole.PROMPT))
        }
        // Story strip's sentences, each with what it means, and each story told whole; Who said it?'s lines.
        content.practice?.stories.orEmpty().forEachIndexed { index, story ->
            story.panels.forEachIndexed { panel, it -> sentence(it.text, it.meaning, PolishUse(file, "stories/${index + 1}/${panel + 1}", PolishRole.PROMPT)) }
            sentence(
                story.panels.joinToString(" ") { it.text },
                mapOf(language to story.panels.joinToString(" ") { it.meaning[language].orEmpty() }),
                PolishUse(file, "stories/${index + 1}", PolishRole.PROMPT),
            )
        }
        content.practice?.speakers.orEmpty().forEachIndexed { index, line ->
            sentence(line.text, line.meaning, PolishUse(file, "speakers/${index + 1}", PolishRole.PROMPT))
        }
        // Directions' routes; Pronoun swap's sentences, each also with its noun swapped for the pronoun.
        content.practice?.routes.orEmpty().forEachIndexed { index, route ->
            sentence(route.text, route.meaning, PolishUse(file, "routes/${index + 1}", PolishRole.PROMPT))
        }
        content.practice?.swaps.orEmpty().forEachIndexed { index, swap ->
            sentence(swap.text, swap.meaning, PolishUse(file, "swaps/${index + 1}", PolishRole.PROMPT))
            sentence(swap.text.replaceFirst(swap.noun, swap.pronoun), swap.shortMeaning, PolishUse(file, "swaps/${index + 1}", PolishRole.ANSWER))
        }
        // Packaging match's phrases and How many?'s.
        content.practice?.packaging.orEmpty().forEachIndexed { index, phrase ->
            sentence(phrase.text, phrase.meaning, PolishUse(file, "packaging/${index + 1}", PolishRole.ANSWER))
        }
        content.practice?.counting.orEmpty().forEachIndexed { index, phrase ->
            sentence(phrase.text, phrase.meaning, PolishUse(file, "counting/${index + 1}", PolishRole.ANSWER))
        }
        // Forecaster's forecasts.
        content.practice?.forecasts.orEmpty().forEachIndexed { index, forecast ->
            sentence(forecast.text, forecast.meaning, PolishUse(file, "forecasts/${index + 1}", PolishRole.ANSWER))
        }
        // Pronoun grid's questions, at the head of its columns.
        content.practice?.pronounGrid?.columns.orEmpty().forEach { column ->
            sentence(column.question, column.meaning, PolishUse(file, "pronounGrid/${column.id}", PolishRole.PROMPT))
        }
        content.course().allClockTimes().forEach { time ->
            sentence(time.polish, mapOf(language to "${time.hour}:${time.minute.toString().padStart(2, '0')}"), PolishUse(file, "clock", PolishRole.ANSWER))
        }
    }

    content.dailySets.forEach { (file, dailySet) ->
        dailySet.section?.let { section ->
            sentence(section.heading, section.meaning, PolishUse(file, dailySet.id, PolishRole.HEADING))
        }
        dailySet.exercises.forEach { exercise ->
            fun use(role: PolishRole) = PolishUse(file, exercise.id, role)
            when (exercise) {
                is ChooseTranslation ->
                    sentence(exercise.prompt, exercise.options.getOrNull(exercise.correctOption), use(PolishRole.PROMPT))
                is TypeAnswer -> exercise.acceptedAnswers.forEach { sentence(it, exercise.prompt, use(PolishRole.ANSWER)) }
                is WordTiles -> {
                    exercise.acceptedAnswers.forEach { sentence(it, exercise.prompt, use(PolishRole.ANSWER)) }
                    exercise.distractors.forEach { sentence(it, null, use(PolishRole.DISTRACTOR)) }
                }
                is Conjugation -> {
                    exercise.acceptedAnswers.forEach { answer ->
                        val whole = exercise.sentence
                        if (whole == null) {
                            sentence(answer, null, use(PolishRole.ANSWER))
                        } else {
                            sentence(filled(whole, answer), exercise.sentenceMeaning, use(PolishRole.ANSWER))
                        }
                        claim(exercise.verb, exercise.person, answer, use(PolishRole.ANSWER))
                    }
                    // An Item this Exercise practises whose text is one of its answers is a verb form Item.
                    val answers = exercise.acceptedAnswers.map(::normalizeForm).toSet()
                    exercise.items.mapNotNull(itemsById::get).filter { normalizeForm(it.value.text) in answers }.forEach { (itemFile, item) ->
                        claim(exercise.verb, exercise.person, item.text, PolishUse(itemFile, item.id, PolishRole.ITEM))
                    }
                }
                is ListenChoose -> {
                    sentence(exercise.text, exercise.meaning, use(PolishRole.ANSWER))
                    exercise.distractors.forEach { sentence(it, null, use(PolishRole.DISTRACTOR)) }
                }
                is ListenType -> (exercise.acceptedAnswers + exercise.readingAnswers).forEach {
                    sentence(it, exercise.meaning, use(PolishRole.ANSWER))
                }
                is FillGaps -> {
                    // Every answer of a gap in the text, the other gaps filled with their first.
                    val firsts = exercise.gaps.map { it.first() }
                    sentence(filledGaps(exercise.text, firsts), exercise.meaning, use(PolishRole.ANSWER))
                    exercise.gaps.forEachIndexed { index, accepted ->
                        accepted.drop(1).forEach { other ->
                            sentence(filledGaps(exercise.text, firsts.toMutableList().also { it[index] = other }), exercise.meaning, use(PolishRole.ANSWER))
                        }
                    }
                    exercise.distractors.forEach { sentence(it, null, use(PolishRole.DISTRACTOR)) }
                }
                is MatchPairs -> exercise.pairs.forEach { sentence(it.polish, it.meaning, use(PolishRole.PROMPT)) }
                is TrueFalse -> {
                    exercise.lines.forEach { sentence(it, null, use(PolishRole.PROMPT)) }
                    exercise.statements.forEach { sentence(it.text, it.meaning, use(PolishRole.PROMPT)) }
                }
                is OrderLines -> exercise.lines.forEach { sentence(it, null, use(PolishRole.ANSWER)) }
            }
        }
        dailySet.tips.forEach { tip ->
            tip.table?.let { table ->
                val use = PolishUse(file, tip.id, PolishRole.TABLE)
                table.rows.forEach { row ->
                    row.header?.let { sentence(it, null, use) }
                    row.cells.forEach { sentence(cellText(it), null, use) }
                }
            }
            tip.conjugation?.let { table ->
                val use = PolishUse(file, tip.id, PolishRole.TABLE)
                Person.entries.forEach { person ->
                    table.forms[person]?.let { form ->
                        sentence(form, null, use)
                        claim(table.verb, person, form, use)
                    }
                }
            }
        }
    }

    // A text that stands for some English is reviewed with it, not once more on its own.
    val withMeaning = sentences.keys.filter { it.second != null }.groupBy { it.first }
    val listed = sentences.entries.mapNotNull { (key, uses) ->
        val (text, meaning) = key
        when {
            meaning != null -> PolishSentence(text, meaning, (uses + sentences[text to null].orEmpty()).distinct())
            text in withMeaning -> null
            else -> PolishSentence(text, null, uses)
        }
    }

    return PolishContent(
        sentences = listed,
        verbForms = claims.map { (key, uses) -> VerbForm(key.first, key.second, key.third, uses) },
        tips = content.dailySets.flatMap { (file, dailySet) ->
            dailySet.tips.map { tip ->
                // The marks are not part of what is read, so marking a Tip does not send it back to review.
                val body = readPolishMarks(tip.body[language].orEmpty())
                val lines = listOf(tip.title[language].orEmpty(), body.text) +
                    tip.conjugation?.let { table -> Person.entries.mapNotNull { table.forms[it] } }.orEmpty() +
                    tip.table?.let { tableLines(it, language) }.orEmpty() +
                    listOfNotNull(tip.kind.takeIf { it != TipKind.NOTE }?.name)
                TipText(file, tip.id, lines.joinToString("\n") { it.trim() }, polish = body.polish.map { body.text.substring(it) })
            }
        } + content.chats.flatMap { (file, script) ->
            // A wrong reply's note is read like a Tip: English, with its Polish marked.
            script.turns.flatMap { turn ->
                turn.replies.mapIndexedNotNull { index, reply ->
                    reply.note?.get(language)?.let { note ->
                        val body = readPolishMarks(note)
                        TipText(file, "${script.id}/${turn.id}/${index + 1}", body.text.trim(), polish = body.polish.map { body.text.substring(it) })
                    }
                }
            }
        },
        unreadable = content.unreadable,
        soundGaps = content.course().soundGapRealWords.let { real -> content.items.flatMap { (file, item) -> soundGaps(item, real).map { file to it } } },
        realWords = content.practice?.realWords.orEmpty(),
        practiceFile = content.practiceFile,
        nouns = content.course().let { course -> course.genders.mapNotNull { (id, gender) -> course.itemsById[id]?.let { GenderedWord(id, it.text, gender) } } },
        possessives = content.course().let { course -> course.possessives.mapNotNull { (gender, id) -> course.itemsById[id]?.let { GenderedWord(id, it.text, gender) } } },
        demonstratives = content.course().let { course -> course.demonstratives.mapNotNull { (gender, id) -> course.itemsById[id]?.let { GenderedWord(id, it.text, gender) } } },
    )
}

/** A grammar table as a reviewer reads it: its title, the column heads, then a line a row, the head first. */
private fun tableLines(table: GrammarTable, language: String): List<String> =
    listOf(table.title[language].orEmpty(), table.columns.joinToString(" | ") { listOfNotNull(it.gender?.mark, it.header).joinToString(" ") }) +
        table.rows.map { row -> (listOfNotNull(row.case?.let { "${it.mark} (${it.question})" } ?: row.header) + row.cells.map(::cellText)).joinToString(" | ") }

private fun normalizeForm(text: String): String = text.trim().lowercase(java.util.Locale.ROOT)
