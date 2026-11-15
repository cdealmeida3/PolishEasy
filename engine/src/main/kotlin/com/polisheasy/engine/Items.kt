package com.polisheasy.engine

/**
 * The Items [dailySet] introduces: those its Exercises practise that no earlier Daily Set on the
 * Path does, in order of first appearance. Derived from content alone, not from the Learner.
 */
fun Course.introducedIn(dailySet: DailySet): List<Item> {
    val earlier = path.takeWhile { it.id != dailySet.id }.flatMap { it.itemIds() }.toSet()
    return dailySet.itemIds().distinct().filter { it !in earlier }.mapNotNull(itemsById::get)
}

/** The id of every Item this Daily Set's Exercises practise, in order, repeats included. */
internal fun DailySet.itemIds(): List<String> = exercises.flatMap { it.items }
