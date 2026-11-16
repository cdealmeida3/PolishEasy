package com.polisheasy.engine

/** Where a Polish text is written: the content file and the Item, Exercise or Tip. */
data class PolishUse(val file: String, val id: String, val role: PolishRole)

enum class PolishRole { ITEM, PROMPT, ANSWER, DISTRACTOR, TABLE }

/** One Polish text with the English it stands for, or null where it stands for none (a distractor, a bare form). */
data class PolishSentence(val text: String, val meaning: String?, val uses: List<PolishUse>)

/** The content's claim that [form] is the form of [verb] for [person]. */
data class VerbForm(val verb: String, val person: Person, val form: String, val uses: List<PolishUse>)

/** A Tip as the Learner reads it. [text] is its title, its body and its table's forms, one per line. */
data class TipText(val file: String, val id: String, val text: String)

/** Every Polish text in the Authored Content, for the Polish check. */
class PolishContent(
    /** One for each distinct pair of text and meaning, in the order the content first writes them. */
    val sentences: List<PolishSentence>,
    /** One for each distinct verb, person and form. */
    val verbForms: List<VerbForm>,
    val tips: List<TipText>,
    /** A listed file that could not be read. The Polish check does not run on half a course. */
    val unreadable: List<ContentProblem>,
)

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

    content.dailySets.forEach { (file, dailySet) ->
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
                val lines = listOf(tip.title[language].orEmpty(), tip.body[language].orEmpty()) +
                    tip.conjugation?.let { table -> Person.entries.mapNotNull { table.forms[it] } }.orEmpty()
                TipText(file, tip.id, lines.joinToString("\n") { it.trim() })
            }
        },
        unreadable = content.unreadable,
    )
}

private fun normalizeForm(text: String): String = text.trim().lowercase(java.util.Locale.ROOT)
