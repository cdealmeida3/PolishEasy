package com.polisheasy.engine

import java.time.LocalDate

/** How many Review Exercises end a Daily Set, at most. */
const val REVIEW_SIZE = 8

/** How many of the Exercises last shown for an Item are remembered, so Review can vary them. */
const val RECENT_EXERCISES = 5

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
    val met = course.path.filter { it.id in completed }
    val inNewMaterial = newMaterial.itemIds().toSet()
    val due = met.flatMap { it.itemIds() }.distinct()
        .filter { it in course.itemsById && it !in inNewMaterial }
        .mapNotNull { id -> scheduleOf(id, course, completed, items)?.let { id to it.due } }
        .filter { (_, dueOn) -> !dueOn.isAfter(today) }
        // A stable sort, so Items due the same day stay in Path order.
        .sortedBy { (_, dueOn) -> dueOn }
        .map { (id, _) -> id }

    val chosen = mutableListOf<String>()
    for (itemId in due) {
        if (chosen.size == limit) break
        val recent = items[itemId]?.recentExercises.orEmpty()
        val candidates = met.flatMap { it.exercises }.filter { itemId in it.items && it.id !in chosen }.map { it.id }
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
