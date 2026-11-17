package com.polisheasy.engine

import java.time.LocalDate

/** Small courses with Items, built directly. Every Exercise has its own prompt, so tests name Exercises by prompt. */

fun item(id: String, text: String = id, meaning: String = "$id in English"): Item =
    Item(id, text, mapOf("en" to meaning))

/** A choice Exercise with two options, the first one correct. */
fun choose(id: String, prompt: String, vararg items: String): ChooseTranslation = ChooseTranslation(
    id = id,
    prompt = prompt,
    options = listOf(mapOf("en" to "right"), mapOf("en" to "wrong")),
    correctOption = 0,
    items = items.toList(),
)

fun typed(id: String, prompt: String, answer: String, vararg items: String): TypeAnswer =
    TypeAnswer(id, mapOf("en" to prompt), listOf(answer), items.toList())

/** A word tiles Exercise with one Accepted Answer and no distractors. */
fun tiles(id: String, prompt: String, answer: String, vararg items: String): WordTiles =
    WordTiles(id, mapOf("en" to prompt), listOf(answer), items = items.toList())

/** A conjugation Exercise with one Accepted Answer and no sentence. Tests name it by verb and pronouns, "być my". */
fun conjugate(id: String, verb: String, person: Person, answer: String, vararg items: String): Conjugation =
    Conjugation(id, verb, mapOf("en" to "to $verb"), person, acceptedAnswers = listOf(answer), items = items.toList())

/**
 * A listen-and-choose Exercise with one distractor, "inne". Tests name it by its Clip,
 * "audio/tak.ogg", or by its meaning, "tak in English", when it is shown in its reading form.
 */
fun listenChoose(id: String, text: String, vararg items: String): ListenChoose =
    ListenChoose(id, text, mapOf("en" to "$text in English"), listOf("inne"), items.toList())

/**
 * A listen-and-type Exercise with one Accepted Answer. Tests name it by its Clip, "audio/tak.ogg",
 * or by its meaning, "tak in English", when it is shown in its reading form.
 */
fun listenType(id: String, text: String, vararg items: String): ListenType =
    ListenType(id, listOf(text), mapOf("en" to "$text in English"), items = items.toList())

/** The Clip of [text], named after it: "dzień dobry" is `dzień-dobry`. */
fun clip(text: String): Clip = Clip(text.replace(' ', '-'), text)

fun day(id: String, vararg exercises: Exercise): DailySet = DailySet(id, exercises = exercises.toList())

private val title = mapOf("en" to "Title")

fun levelOf(id: String, vararg dailySets: DailySet): Level =
    Level(id, title, listOf(CourseUnit("$id-u01", title, dailySets.toList())))

fun unitOf(id: String, title: String, vararg dailySets: DailySet): CourseUnit =
    CourseUnit(id, mapOf("en" to title), dailySets.toList())

fun levelOf(id: String, title: String, vararg units: CourseUnit): Level = Level(id, mapOf("en" to title), units.toList())

/** A one-Level course of these Daily Sets, in Path order. */
fun courseOf(items: List<Item>, vararg dailySets: DailySet): Course = Course(listOf(levelOf("a1", *dailySets)), items)

/** The same, with [clips] as its audio. */
fun courseOf(items: List<Item>, clips: List<Clip>, vararg dailySets: DailySet): Course =
    Course(listOf(levelOf("a1", *dailySets)), items, clips)

private val Step.ShowExercise.prompt: String
    get() = when (val view = exercise) {
        is ExerciseView.ChooseTranslation -> view.prompt
        is ExerciseView.TypeAnswer -> view.prompt
        is ExerciseView.WordTiles -> view.prompt
        is ExerciseView.Conjugation -> "${view.verb} ${view.person.pronouns}"
        is ExerciseView.ListenChoose -> view.audio.path
        is ExerciseView.ListenType -> view.audio.path
        is ExerciseView.ChoosePolish -> view.prompt
    }

/** Whether this Step belongs to a Replay. */
val Step.isReplay: Boolean
    get() = when (this) {
        is Step.ShowTip -> replay
        is Step.ShowExercise -> replay
        is Step.ShowSummary -> replay
    }

/** The prompts of the Exercises among [steps], in the order they were shown. */
fun shown(steps: List<Step>): List<String> = steps.filterIsInstance<Step.ShowExercise>().map { it.prompt }

/** The prompts of the Review Exercises among [steps], in the order they were shown. */
fun reviewed(steps: List<Step>): List<String> =
    steps.filterIsInstance<Step.ShowExercise>().filter { it.review }.map { it.prompt }

/** One Learner's phone playing [course]: their saved progress and their clock. */
class ItemLearner(
    val course: Course,
    val progress: LearnerProgress = InMemoryLearnerProgress(),
    val clock: FakeClock = FakeClock(),
    private val scheduler: ReviewScheduler? = null,
    /** The One a day setting. On for the tests written before it existed, which are about its rules. */
    var oneADay: Boolean = true,
) {
    /** Opening the app, or reopening it after it was closed: a new engine over what was saved. */
    fun openApp(): CourseEngine =
        if (scheduler == null) CourseEngine(course, progress, clock, oneADay = { oneADay })
        else CourseEngine(course, progress, clock, scheduler = scheduler, oneADay = { oneADay })

    /**
     * Opens the app on [date] and plays that day's Daily Set, started or resumed, to its Summary.
     * Every Exercise is answered correctly, except that one whose prompt is in [wrongFirst] is
     * answered wrongly the first time it is shown. Returns every Step seen.
     */
    fun playDay(date: LocalDate, wrongFirst: Set<String> = emptySet()): List<Step> {
        clock.moveTo(date)
        val engine = openApp()
        return play(engine, open(engine), wrongFirst = wrongFirst)
    }

    /** The first Step of today's Daily Set: started if none is in progress, resumed otherwise. */
    fun open(engine: CourseEngine): Step =
        if (engine.today() is Today.InProgress) engine.resumeDailySet() else engine.startDailySet()

    /**
     * Plays from [first], answering at most [answers] Exercises. Returns every Step seen, ending
     * with the Step that is due next.
     */
    fun play(
        engine: CourseEngine,
        first: Step,
        answers: Int = Int.MAX_VALUE,
        wrongFirst: Set<String> = emptySet(),
    ): List<Step> {
        val seen = mutableListOf(first)
        val missed = mutableSetOf<String>()
        var given = 0
        while (true) {
            when (val step = seen.last()) {
                is Step.ShowTip -> Unit
                is Step.ShowExercise -> {
                    if (given == answers) return seen
                    given++
                    val wrong = step.prompt in wrongFirst && missed.add(step.prompt)
                    engine.answer(answerTo(step, correct = !wrong))
                }
                is Step.ShowSummary -> return seen
            }
            seen += engine.next()
        }
    }

    private fun answerTo(step: Step.ShowExercise, correct: Boolean): Answer {
        val view = step.exercise
        return when (val exercise = course.exercises.getValue(step.exerciseId)) {
            is ChooseTranslation -> Answer.Option(if (correct) 0 else 1)
            is TypeAnswer -> Answer.Typed(if (correct) exercise.acceptedAnswers.first() else "wrong")
            is WordTiles -> {
                val words = words(exercise.acceptedAnswers.first())
                Answer.Tiles(if (correct) words else words.reversed())
            }
            is Conjugation -> Answer.Typed(if (correct) exercise.acceptedAnswers.first() else "wrong")
            is ListenChoose -> {
                val options = when (view) {
                    is ExerciseView.ListenChoose -> view.options
                    is ExerciseView.ChoosePolish -> view.options
                    else -> error("Exercise ${exercise.id} is shown as $view")
                }
                Answer.Option(options.indexOf(if (correct) exercise.text else exercise.distractors.first()))
            }
            is ListenType -> Answer.Typed(if (correct) exercise.acceptedAnswers.first() else "wrong")
        }
    }
}
