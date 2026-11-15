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

fun day(id: String, vararg exercises: Exercise): DailySet = DailySet(id, exercises = exercises.toList())

private val title = mapOf("en" to "Title")

fun levelOf(id: String, vararg dailySets: DailySet): Level =
    Level(id, title, listOf(CourseUnit("$id-u01", title, dailySets.toList())))

/** A one-Level course of these Daily Sets, in Path order. */
fun courseOf(items: List<Item>, vararg dailySets: DailySet): Course = Course(listOf(levelOf("a1", *dailySets)), items)

private val Step.ShowExercise.prompt: String
    get() = when (val view = exercise) {
        is ExerciseView.ChooseTranslation -> view.prompt
        is ExerciseView.TypeAnswer -> view.prompt
        is ExerciseView.WordTiles -> view.prompt
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
) {
    /** Opening the app, or reopening it after it was closed: a new engine over what was saved. */
    fun openApp(): CourseEngine =
        if (scheduler == null) CourseEngine(course, progress, clock)
        else CourseEngine(course, progress, clock, scheduler = scheduler)

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

    private fun answerTo(step: Step.ShowExercise, correct: Boolean): Answer = when (val view = step.exercise) {
        is ExerciseView.ChooseTranslation -> Answer.Option(if (correct) 0 else 1)
        is ExerciseView.TypeAnswer -> {
            val exercise = course.exercises.values.filterIsInstance<TypeAnswer>()
                .first { it.prompt.getValue("en") == view.prompt }
            Answer.Typed(if (correct) exercise.acceptedAnswers.first() else "wrong")
        }
        is ExerciseView.WordTiles -> {
            val exercise = course.exercises.values.filterIsInstance<WordTiles>()
                .first { it.prompt.getValue("en") == view.prompt }
            val words = words(exercise.acceptedAnswers.first())
            Answer.Tiles(if (correct) words else words.reversed())
        }
    }
}
