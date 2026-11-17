package com.polisheasy.engine

import kotlin.random.Random

/** A small course for Practice, built directly: three days, each showing a verb's table, with gap sentences to use it in. */

private fun english(text: String) = mapOf("en" to text)

fun table(verb: String, meaning: String, vararg forms: String): ConjugationTable =
    ConjugationTable(verb, english(meaning), Person.entries.zip(forms.toList()).toMap())

val byc = table("być", "to be", "jestem", "jesteś", "jest", "jesteśmy", "jesteście", "są")
val miec = table("mieć", "to have", "mam", "masz", "ma", "mamy", "macie", "mają")
val mowic = table("mówić", "to speak", "mówię", "mówisz", "mówi", "mówimy", "mówicie", "mówią")

/** A conjugation Exercise with a gap sentence. Its meaning is the sentence followed by "in English". */
fun gap(id: String, table: ConjugationTable, person: Person, sentence: String, vararg items: String): Conjugation = Conjugation(
    id,
    table.verb,
    table.meaning,
    person,
    sentence,
    english("$sentence in English"),
    listOf(table.forms.getValue(person)),
    items.toList(),
)

/** A Daily Set whose one Tip shows [table] before its first Exercise. */
fun dayWithTable(id: String, table: ConjugationTable, vararg exercises: Exercise): DailySet = DailySet(
    id,
    listOf(Tip("$id-t01", exercises.first().id, english("Title"), english("Body"), table)),
    exercises.toList(),
)

val practiceItems = listOf(item("jestem"), item("jest"), item("mam"), item("mówię"))

/** Day 1 teaches być with five sentences, day 2 mieć with two, day 3 mówić with one. */
val practiceCourse = Course(
    listOf(
        levelOf(
            "a1",
            dayWithTable(
                "d1",
                byc,
                gap("d1-e1", byc, Person.I, "___ Adam.", "jestem"),
                gap("d1-e2", byc, Person.YOU, "Ty ___ Ewa."),
                gap("d1-e3", byc, Person.HE_SHE_IT, "To ___ Adam.", "jest"),
                gap("d1-e4", byc, Person.WE, "My ___ tutaj."),
                gap("d1-e5", byc, Person.THEY, "Oni ___ tutaj."),
                choose("d1-e6", "jestem", "jestem"),
            ),
            dayWithTable(
                "d2",
                miec,
                gap("d2-e1", miec, Person.I, "___ czas.", "mam"),
                gap("d2-e2", miec, Person.WE, "My ___ czas."),
            ),
            dayWithTable("d3", mowic, gap("d3-e1", mowic, Person.I, "___ po polsku.", "mówię")),
        ),
    ),
    practiceItems,
)

/** One Learner's phone with Practice on it: their Path progress, their Bests and their clock. */
class Player(
    val course: Course = practiceCourse,
    val progress: LearnerProgress = InMemoryLearnerProgress(),
    val bests: PracticeProgress = InMemoryPracticeProgress(),
    val clock: FakeClock = FakeClock(),
    private val seed: Int = 1,
) {
    val engine: CourseEngine get() = CourseEngine(course, progress, clock)

    /** Opening the app: a new engine and a new Practice over what was saved. */
    fun practice(): Practice = engine.practice(bests, Random(seed))

    /** The Learner completed these Daily Sets, the last of them today and the others on the days before. */
    fun completed(vararg dailySets: String): Player {
        val today = java.time.LocalDate.now(clock)
        dailySets.forEachIndexed { index, id ->
            progress.recordCompleted(id, today.minusDays((dailySets.size - 1 - index).toLong()))
        }
        return this
    }
}

/** A Round of the Verb challenge on [verb] at [level], started from a fresh look at the Game. */
fun Practice.verbRound(verb: String, level: String = "choose"): VerbRound =
    start(setup(Game.VERB_CHALLENGE, listOf(verb, level))) as VerbRound

/** The option or tile that answers the question being shown. */
fun VerbRound.rightOption(): Int {
    val asked = checkNotNull(question)
    val used = tilesUsed
    return asked.options.indices.first { asked.options[it] in asked.answers && used.getOrNull(it) != true }
}

/** An option or tile that does not answer the question being shown. */
fun VerbRound.wrongOption(): Int {
    val asked = checkNotNull(question)
    val used = tilesUsed
    return asked.options.indices.first { asked.options[it] !in asked.answers && used.getOrNull(it) != true }
}

/** Answers the question being shown, rightly or not, and moves on. */
fun VerbRound.answerAndMoveOn(correctly: Boolean = true): VerbRound = when {
    typing -> type(if (correctly) checkNotNull(question).answers.first() else "nie wiem")
    else -> choose(if (correctly) rightOption() else wrongOption())
}.next()

/** Plays the Round to its end, answering the first [wrong] questions wrongly and the rest rightly. */
fun VerbRound.playOut(wrong: Int = 0): VerbRound {
    var round = this
    var missed = 0
    while (!round.finished) round = round.answerAndMoveOn(correctly = missed++ >= wrong)
    return round
}

private fun word(id: String) = item(id, id, "$id in English")

/** A Daily Set with one Exercise for each of [items], which it introduces. */
private fun dayOfWords(id: String, vararg items: String): DailySet =
    DailySet(id, exercises = items.mapIndexed { index, item -> choose("$id-e${index + 1}", item, item) })

/** The picture of each pictured Item of [wordsCourse]: an image named after it, or for the numbers a numeral. */
val wordPictures: Map<String, Picture> =
    listOf("kot", "pies", "dom", "tak", "ryba", "ptak", "mama", "tata", "brat", "siostra")
        .associateWith<String, Picture> { Picture.Image("pictures/$it.webp") } +
        mapOf("jeden" to Picture.Glyph("1"), "dwa" to Picture.Glyph("2"))

/**
 * A course of words for the Games that use pictures. Unit "Animals": w1 teaches kot, pies, dom,
 * tak and bardzo, w2 ryba and ptak. Unit "Family": w3 teaches jeden and dwa, w4 mama and tata,
 * w5 brat, w6 siostra and bo. Every word has a picture but bardzo and bo.
 */
val wordsCourse = Course(
    listOf(
        levelOf(
            "a1",
            "A1",
            unitOf("u1", "Animals", dayOfWords("w1", "kot", "pies", "dom", "tak", "bardzo"), dayOfWords("w2", "ryba", "ptak")),
            unitOf(
                "u2",
                "Family",
                dayOfWords("w3", "jeden", "dwa"),
                dayOfWords("w4", "mama", "tata"),
                dayOfWords("w5", "brat"),
                dayOfWords("w6", "siostra", "bo"),
            ),
        ),
    ),
    listOf("kot", "pies", "dom", "tak", "bardzo", "ryba", "ptak", "jeden", "dwa", "mama", "tata", "brat", "siostra", "bo").map(::word),
    pictures = wordPictures,
)
