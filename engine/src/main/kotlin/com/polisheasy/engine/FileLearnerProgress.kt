package com.polisheasy.engine

import java.io.File
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The stored document. Content is referred to only by stable ids; dates are ISO strings. */
@Serializable
private data class StoredProgress(
    val version: Int = 1,
    val completed: Map<String, String> = emptyMap(),
    val inProgress: DailySetInProgress? = null,
    val items: Map<String, StoredItem> = emptyMap(),
    /** The days a Review-only Daily Set was completed, in order. */
    val reviewOnlyDays: List<String> = emptyList(),
)

@Serializable
private data class StoredItem(
    val box: Int,
    val due: String,
    val lastAnswered: String,
    val recent: List<String> = emptyList(),
) {
    constructor(item: ItemProgress) :
        this(item.schedule.box, item.schedule.due.toString(), item.schedule.lastAnswered.toString(), item.recentExercises)

    fun toItemProgress() =
        ItemProgress(ItemSchedule(box, LocalDate.parse(due), LocalDate.parse(lastAnswered)), recent)
}

// Unknown keys are ignored so a later version can add fields without breaking this one.
private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Learner progress saved in one JSON file (see ADR-0003). The file is read once, when this is
 * created; every change rewrites it whole, through a temporary file and an atomic move, so a
 * crash mid-write leaves the previous version intact.
 */
class FileLearnerProgress(file: File) : LearnerProgress {
    private val saved = SavedFile(file)
    private var completed: Map<String, LocalDate>
    private var inProgress: DailySetInProgress?
    private var items: Map<String, ItemProgress>
    private var reviewOnly: Set<LocalDate>

    init {
        val stored = read()
        completed = stored.completed.mapValues { LocalDate.parse(it.value) }
        inProgress = stored.inProgress
        items = stored.items.mapValues { it.value.toItemProgress() }
        reviewOnly = stored.reviewOnlyDays.map(LocalDate::parse).toSet()
    }

    override fun completedDailySets(): Map<String, LocalDate> = completed

    override fun recordCompleted(dailySetId: String, on: LocalDate, items: Map<String, ItemProgress>) {
        write(completed + (dailySetId to on), inProgress = null, this.items + items, reviewOnly)
    }

    override fun reviewOnlyDays(): Set<LocalDate> = reviewOnly

    override fun recordReviewOnlyCompleted(on: LocalDate, items: Map<String, ItemProgress>) {
        write(completed, inProgress = null, this.items + items, reviewOnly + on)
    }

    override fun dailySetInProgress(): DailySetInProgress? = inProgress

    override fun saveInProgress(state: DailySetInProgress, items: Map<String, ItemProgress>) {
        write(completed, inProgress = state, this.items + items, reviewOnly)
    }

    override fun itemProgress(): Map<String, ItemProgress> = items

    override fun recordItems(items: Map<String, ItemProgress>) {
        write(completed, inProgress, this.items + items, reviewOnly)
    }

    /** A file that cannot be read is moved aside, so a bad file can never stop the app opening. */
    private fun read(): StoredProgress = saved.read(StoredProgress()) { text ->
        json.decodeFromString<StoredProgress>(text).also { stored ->
            stored.completed.values.forEach(LocalDate::parse)
            stored.items.values.forEach { it.toItemProgress() }
            stored.reviewOnlyDays.forEach(LocalDate::parse)
        }
    }

    private fun write(
        completed: Map<String, LocalDate>,
        inProgress: DailySetInProgress?,
        items: Map<String, ItemProgress>,
        reviewOnly: Set<LocalDate>,
    ) {
        val stored = StoredProgress(
            completed = completed.mapValues { it.value.toString() },
            inProgress = inProgress,
            items = items.mapValues { StoredItem(it.value) },
            reviewOnlyDays = reviewOnly.sorted().map { it.toString() },
        )
        saved.write(json.encodeToString(stored))
        this.completed = completed
        this.inProgress = inProgress
        this.items = items
        this.reviewOnly = reviewOnly
    }
}
