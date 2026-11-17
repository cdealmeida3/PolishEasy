package com.polisheasy.engine

import java.time.Clock
import java.time.LocalDate

/** A word as the Słownik, the Fiszki and "Sprawdź w słowniku" show it. [audio] is its Clip, if it has one. */
data class WordView(
    val itemId: String,
    val text: String,
    val meaning: String,
    val register: Register? = null,
    val audio: ClipView? = null,
)

/** The Path as shelves: Starter, then the book. Each is a Level, with a card for each of its Units. */
data class ShelfView(val id: String, val title: String, val lessons: List<LessonCardView>)

/**
 * A Unit on the Path. [number] is the book's lesson number, or null for a Starter Unit, whose place
 * on its shelf is [place]. [subtitle] is the English under a Polish [title]. [sectionsDone] of
 * [sections] are completed; [sealed] once its Checkpoint is passed. [coming] while it has no
 * content. [current] for the Unit the Learner is at: the first with a section not completed.
 */
data class LessonCardView(
    val unitId: String,
    val number: Int?,
    val place: Int,
    val title: String,
    val subtitle: String?,
    val accent: Int,
    val sectionsDone: Int,
    val sections: Int,
    val sealed: Boolean,
    val coming: Boolean,
    val current: Boolean,
)

/** One strand of a lesson: what the contents say it teaches, and how many of its sections are completed. */
data class StrandView(val strand: Strand, val topics: List<String>, val done: Int, val total: Int)

/**
 * A section of a lesson, which is a Daily Set: its [letter] (or its number in a Starter Unit), its
 * [heading] in Polish when it has one, else its title, or null when it has neither (a Daily Set
 * with no Tip), the English [meaning] of the heading, how
 * many [newWords] it introduces, and its [state] on the Path. [inProgress] when it is the one being
 * played and not finished.
 */
data class SectionView(
    val dailySetId: String,
    val letter: String,
    val heading: String?,
    val meaning: String?,
    val newWords: Int,
    val state: DailySetState,
    val inProgress: Boolean,
)

/** A chapter's Checkpoint: its [best] in percent, or null, how many [questions] it asks, and whether it is [sealed] (passed). */
data class CheckpointView(val best: Int?, val questions: Int, val sealed: Boolean)

/** A chapter, as its Lesson screen shows it, like the book's opening page and its Powtórzenie. */
data class LessonView(
    val unitId: String,
    val number: Int?,
    val place: Int,
    val title: String,
    val subtitle: String?,
    val accent: Int,
    val strands: List<StrandView>,
    val lookUp: List<WordView>,
    val sections: List<SectionView>,
    /** How many words its Słownik has. */
    val words: Int,
    val checkpoint: CheckpointView,
    val coming: Boolean,
)

/**
 * One flashcard of a chapter's Fiszki: a word, whether it is [due] for Review today, and whether
 * the Learner has [met] it in a completed Daily Set.
 */
data class FlashcardView(val word: WordView, val due: Boolean, val met: Boolean)

/**
 * The Lesson screens, the Słownik, the Fiszki and what a Checkpoint asks. Everything is read from
 * the course and the Learner's progress; only a flashcard answer saves anything, the Item's
 * Review schedule, as an Exercise's first attempt would.
 */
internal class Lessons(
    private val course: Course,
    private val progress: LearnerProgress,
    private val checkpoints: CheckpointBests,
    private val clock: Clock,
    private val scheduler: ReviewScheduler,
    private val localize: (Localized) -> String,
    /** The state of each Daily Set on the Path, as [CourseEngine.path] gives it. */
    private val states: () -> Map<String, DailySetState>,
) {
    private val units: List<Pair<Level, CourseUnit>> = course.levels.flatMap { level -> level.units.map { level to it } }

    fun unit(unitId: String): CourseUnit =
        checkNotNull(units.firstOrNull { it.second.id == unitId }?.second) { "There is no Unit $unitId" }

    fun shelves(): List<ShelfView> {
        val completed = progress.completedDailySets().keys
        val current = units.firstOrNull { (_, unit) -> unit.dailySets.any { it.id !in completed } }?.second?.id
        return course.levels.map { level ->
            ShelfView(
                level.id,
                localize(level.title),
                level.units.mapIndexed { index, unit ->
                    LessonCardView(
                        unitId = unit.id,
                        number = unit.number,
                        place = index + 1,
                        title = titleOf(unit),
                        subtitle = subtitleOf(unit),
                        accent = unit.accent,
                        sectionsDone = unit.dailySets.count { it.id in completed },
                        sections = unit.dailySets.size,
                        sealed = sealed(unit.id),
                        coming = unit.coming || unit.dailySets.isEmpty(),
                        current = unit.id == current,
                    )
                },
            )
        }
    }

    fun lesson(unitId: String): LessonView {
        val unit = unit(unitId)
        val place = units.filter { it.first == units.first { (_, u) -> u.id == unitId }.first }.indexOfFirst { it.second.id == unitId } + 1
        val completed = progress.completedDailySets().keys
        val saved = progress.dailySetInProgress()?.dailySetId
        val pathStates = states()
        return LessonView(
            unitId = unit.id,
            number = unit.number,
            place = place,
            title = titleOf(unit),
            subtitle = subtitleOf(unit),
            accent = unit.accent,
            strands = Strand.entries.mapNotNull { strand ->
                val topics = unit.strands[strand].orEmpty()
                if (topics.isEmpty()) return@mapNotNull null
                val covering = unit.dailySets.filter { strand in it.section?.strands.orEmpty() }
                StrandView(strand, topics, done = covering.count { it.id in completed }, total = covering.size)
            },
            lookUp = unit.lookUp.mapNotNull(course.itemsById::get).map(::word),
            sections = unit.dailySets.mapIndexed { index, set ->
                val section = set.section
                SectionView(
                    dailySetId = set.id,
                    letter = section?.letter ?: (index + 1).toString(),
                    heading = section?.heading ?: set.tips.firstOrNull()?.let { localize(it.title) },
                    meaning = section?.meaning?.takeIf { it.isNotEmpty() }?.let(localize),
                    newWords = course.introducedIn(set).size,
                    state = pathStates[set.id] ?: if (set.id in completed) DailySetState.COMPLETED else DailySetState.LOCKED,
                    inProgress = set.id == saved,
                )
            },
            words = wordsOf(unit).size,
            checkpoint = CheckpointView(
                best = checkpoints.best(unit.id),
                questions = minOf(CHECKPOINT_QUESTIONS, unit.dailySets.sumOf { it.exercises.size }),
                sealed = sealed(unit.id),
            ),
            coming = unit.coming || unit.dailySets.isEmpty(),
        )
    }

    /** The chapter's Słownik: every word it introduces, in the order it does. */
    fun dictionary(unitId: String): List<WordView> = wordsOf(unit(unitId)).map(::word)

    /**
     * The chapter's Fiszki: its words, those due for Review first (earliest due first), then the
     * rest of those met, then those not met yet, each group in the chapter's order.
     */
    fun flashcards(unitId: String): List<FlashcardView> {
        val today = LocalDate.now(clock)
        val completed = progress.completedDailySets()
        val stored = progress.itemProgress()
        val cards = wordsOf(unit(unitId)).map { item ->
            val schedule = scheduleOf(item.id, course, completed, stored)
            Triple(item, schedule, schedule != null && !schedule.due.isAfter(today))
        }
        val due = cards.filter { it.third }.sortedBy { it.second?.due }
        val met = cards.filter { !it.third && it.second != null }
        val unmet = cards.filter { it.second == null }
        return (due + met + unmet).map { (item, schedule, isDue) -> FlashcardView(word(item), due = isDue, met = schedule != null) }
    }

    /**
     * The Learner turned the flashcard of [itemId] and said whether they [knew] it. The Item is
     * rescheduled as an answer to an Exercise would reschedule it: a "knew it" moves it up at most
     * one box a day, a "not yet" sends it back to the first.
     */
    fun answerFlashcard(itemId: String, knew: Boolean) {
        check(itemId in course.itemsById) { "There is no Item $itemId" }
        val today = LocalDate.now(clock)
        val stored = progress.itemProgress()
        val previous = scheduleOf(itemId, course, progress.completedDailySets(), stored)
        val schedule = scheduler.afterAnswer(previous, knew, today)
        progress.recordItems(mapOf(itemId to ItemProgress(schedule, stored[itemId]?.recentExercises.orEmpty())))
    }

    /** Every Exercise of the chapter, for its Checkpoint. */
    fun exercisesOf(unitId: String): List<Exercise> = unit(unitId).dailySets.flatMap { it.exercises }

    fun titleOf(unit: CourseUnit): String = unit.polishTitle ?: localize(unit.title)

    private fun subtitleOf(unit: CourseUnit): String? = if (unit.polishTitle != null) localize(unit.title) else null

    private fun sealed(unitId: String): Boolean = (checkpoints.best(unitId) ?: 0) >= CHECKPOINT_PASS_PERCENT

    /**
     * The words a chapter teaches: each Item one of its Exercises introduces as rule 1 of
     * authoring does, a choose_translation of the Item's text that lists only it. A chapter of the
     * book introduces again a word met earlier, so its Słownik has every word it uses.
     */
    private fun wordsOf(unit: CourseUnit): List<Item> =
        unit.dailySets.flatMap { it.exercises }.filterIsInstance<ChooseTranslation>()
            .mapNotNull { exercise -> exercise.items.singleOrNull()?.let(course.itemsById::get)?.takeIf { audioKey(it.text) == audioKey(exercise.prompt) } }
            .distinctBy { it.id }

    private fun word(item: Item) = WordView(item.id, item.text, localize(item.meaning), item.register, course.clipFor(item.text))
}

/**
 * The words of a Słownik that [query] finds: those whose Polish or meaning contains it, ignoring
 * case and Polish letters' marks, so "dziekuje" finds "dziękuję". A blank query finds every word.
 */
fun List<WordView>.matching(query: String): List<WordView> {
    val wanted = folded(query.trim())
    if (wanted.isEmpty()) return this
    return filter { folded(it.text).contains(wanted) || folded(it.meaning).contains(wanted) }
}

private fun folded(text: String): String =
    java.text.Normalizer.normalize(text.lowercase().replace('ł', 'l'), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
