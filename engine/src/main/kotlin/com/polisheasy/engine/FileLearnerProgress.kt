package com.polisheasy.engine

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The stored document. Content is referred to only by stable ids; dates are ISO strings. */
@Serializable
private data class StoredProgress(
    val version: Int = 1,
    val completed: Map<String, String> = emptyMap(),
    val inProgress: DailySetInProgress? = null,
)

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
class FileLearnerProgress(private val file: File) : LearnerProgress {
    private var completed: Map<String, LocalDate>
    private var inProgress: DailySetInProgress?

    init {
        val stored = read()
        completed = stored.completed.mapValues { LocalDate.parse(it.value) }
        inProgress = stored.inProgress
    }

    override fun completedDailySets(): Map<String, LocalDate> = completed

    override fun recordCompleted(dailySetId: String, on: LocalDate) {
        write(completed + (dailySetId to on), inProgress = null)
    }

    override fun dailySetInProgress(): DailySetInProgress? = inProgress

    override fun saveInProgress(state: DailySetInProgress) {
        write(completed, inProgress = state)
    }

    /** A file that cannot be read is moved aside, so a bad file can never stop the app opening. */
    private fun read(): StoredProgress {
        if (!file.exists()) return StoredProgress()
        return try {
            json.decodeFromString<StoredProgress>(file.readText()).also { stored ->
                stored.completed.values.forEach(LocalDate::parse)
            }
        } catch (e: Exception) {
            runCatching {
                Files.move(file.toPath(), File(file.path + ".corrupt").toPath(), REPLACE_EXISTING)
            }
            StoredProgress()
        }
    }

    private fun write(completed: Map<String, LocalDate>, inProgress: DailySetInProgress?) {
        val stored = StoredProgress(completed = completed.mapValues { it.value.toString() }, inProgress = inProgress)
        val temporary = File(file.path + ".tmp")
        file.absoluteFile.parentFile?.mkdirs()
        temporary.writeText(json.encodeToString(stored))
        try {
            Files.move(temporary.toPath(), file.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), file.toPath(), REPLACE_EXISTING)
        }
        this.completed = completed
        this.inProgress = inProgress
    }
}
