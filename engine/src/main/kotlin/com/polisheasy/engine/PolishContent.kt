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
    content.items.forEach { (file, item) -> sentence(item.text, item.meaning, PolishUse(file, item.id, PolishRole.ITEM)) }

    // A chapter's title and its K, L and G lists are quoted from the book's contents, abbreviations
    // and all, so they are not the app's Polish to check.
    content.instructions.forEach { (kind, instruction) ->
        val file = content.instructionsFile.orEmpty()
        sentence(instruction.polish, instruction.meaning, PolishUse(file, kind.name.lowercase(java.util.Locale.ROOT), PolishRole.PROMPT))
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
            }
        }
        dailySet.tips.forEach { tip ->
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
                    tip.conjugation?.let { table -> Person.entries.mapNotNull { table.forms[it] } }.orEmpty()
                TipText(file, tip.id, lines.joinToString("\n") { it.trim() }, polish = body.polish.map { body.text.substring(it) })
            }
        },
        unreadable = content.unreadable,
        soundGaps = content.course().soundGapRealWords.let { real -> content.items.flatMap { (file, item) -> soundGaps(item, real).map { file to it } } },
        realWords = content.practice?.realWords.orEmpty(),
        practiceFile = content.practiceFile,
        nouns = content.course().let { course -> course.genders.mapNotNull { (id, gender) -> course.itemsById[id]?.let { GenderedWord(id, it.text, gender) } } },
        possessives = content.course().let { course -> course.possessives.mapNotNull { (gender, id) -> course.itemsById[id]?.let { GenderedWord(id, it.text, gender) } } },
    )
}

private fun normalizeForm(text: String): String = text.trim().lowercase(java.util.Locale.ROOT)
