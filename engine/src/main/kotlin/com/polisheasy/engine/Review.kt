package com.polisheasy.engine

import java.time.LocalDate

/** How many Review Exercises end a Daily Set, at most. */
const val REVIEW_SIZE = 8

/** How many of the Exercises last shown for an Item are remembered, so Review can vary them. */
const val RECENT_EXERCISES = 5

/** How many Exercises a Review-only Daily Set has, at most. */
const val REVIEW_ONLY_SIZE = 20

/**
 * What a Review-only Daily Set in progress is saved under. No Daily Set on the Path can have
 * this id: the content checker requires a Daily Set's id to start with its Unit's.
 */
const val REVIEW_ONLY_ID = "review-only"

/**
 * The Review for a Daily Set started on [today]: the ids of up to [limit] Exercises, one for each
 * Item that is due, earliest due first and ties in Path order.
 *
 * Only Items met in [completed] Daily Sets are reviewed, and never one that [newMaterial] already
 * practises. Each Item is reviewed with an Exercise from a completed Daily Set that was not shown
 * recently, or else the one shown longest ago. An Exercise is given once even if several of its
 * Items are due. Nothing is random, so the same progress always gives the same Review.
 */
internal fun chooseReview(
    course: Course,
    completed: Map<String, LocalDate>,
    items: Map<String, ItemProgress>,
    newMaterial: DailySet,
    today: LocalDate,
    limit: Int = REVIEW_SIZE,
): List<String> {
    val inNewMaterial = newMaterial.itemIds().toSet()
    val due = metItems(course, completed, items)
        .filter { (id, schedule) -> id !in inNewMaterial && !schedule.due.isAfter(today) }
        // A stable sort, so Items due the same day stay in Path order.
        .sortedBy { (_, schedule) -> schedule.due }
    return exercisesFor(due.map { (id, _) -> id }, course, completed, items, limit)
}

/**
 * A Review-only Daily Set started on [today]: the ids of up to [limit] Exercises, one for each
 * Item, the Items due first (earliest due first), then those answered longest ago. Ties in Path order.
 *
 * Every Item met in a [completed] Daily Set can be given, due or not, so a Review-only Daily Set
 * is full whenever the course has enough Items. The Exercise for each Item is chosen as in
 * [chooseReview], and nothing is random.
 */
internal fun chooseReviewOnly(
    course: Course,
    completed: Map<String, LocalDate>,
    items: Map<String, ItemProgress>,
    today: LocalDate,
    limit: Int = REVIEW_ONLY_SIZE,
): List<String> {
    val (due, notDue) = metItems(course, completed, items).partition { (_, schedule) -> !schedule.due.isAfter(today) }
    // Stable sorts, so ties stay in Path order.
    val order = due.sortedBy { (_, schedule) -> schedule.due } + notDue.sortedBy { (_, schedule) -> schedule.lastAnswered }
    return exercisesFor(order.map { (id, _) -> id }, course, completed, items, limit)
}

/** Every Item met in a [completed] Daily Set, in Path order, with its schedule. */
private fun metItems(
    course: Course,
    completed: Map<String, LocalDate>,
    items: Map<String, ItemProgress>,
): List<Pair<String, ItemSchedule>> =
    course.path.filter { it.id in completed }.flatMap { it.itemIds() }.distinct()
        .filter { it in course.itemsById }
        .mapNotNull { id -> scheduleOf(id, course, completed, items)?.let { id to it } }

/**
 * One Exercise for each of [itemIds], in that order, until there are [limit]: the first in Path
 * order, from a completed Daily Set, that was not shown recently for the Item, or else the one
 * shown longest ago. An Exercise already chosen is not chosen again, and an Item left with none
 * is skipped.
 */
private fun exercisesFor(
    itemIds: List<String>,
    course: Course,
    completed: Map<String, LocalDate>,
    items: Map<String, ItemProgress>,
    limit: Int,
): List<String> {
    val met = course.path.filter { it.id in completed }.flatMap { it.exercises }
    val chosen = mutableListOf<String>()
    for (itemId in itemIds) {
        if (chosen.size == limit) break
        val recent = items[itemId]?.recentExercises.orEmpty()
        val candidates = met.filter { itemId in it.items && it.id !in chosen }.map { it.id }
        val exercise = candidates.firstOrNull { it !in recent } ?: candidates.minByOrNull(recent::indexOf)
        if (exercise != null) chosen += exercise
    }
    return chosen
}

/**
 * The stored schedule of an Item. One met in a completed Daily Set but never recorded, because
 * that Daily Set was completed before Items were tracked, counts as box one, last answered the
 * day that Daily Set was completed. Null for an Item the Learner has not met.
 */
internal fun scheduleOf(
    itemId: String,
    course: Course,
    completed: Map<String, LocalDate>,
    items: Map<String, ItemProgress>,
): ItemSchedule? {
    items[itemId]?.let { return it.schedule }
    val metIn = course.path.firstOrNull { it.id in completed && itemId in it.itemIds() } ?: return null
    val metOn = completed.getValue(metIn.id)
    return ItemSchedule(box = 1, due = metOn.plusDays(1), lastAnswered = metOn)
}
