package com.polisheasy.engine

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/**
 * One file of saved state, written whole each time (see ADR-0003). A write goes through a
 * temporary file and an atomic move, so a crash mid-write leaves the previous version intact.
 */
internal class SavedFile(private val file: File) {
    /**
     * What [decode] makes of the file, or [empty] when there is no file. A file that [decode]
     * cannot read is moved aside to `<name>.corrupt` and [empty] is returned, so a bad file can
     * never stop the app opening.
     */
    fun <T> read(empty: T, decode: (String) -> T): T {
        if (!file.exists()) return empty
        return try {
            decode(file.readText())
        } catch (e: Exception) {
            runCatching {
                Files.move(file.toPath(), File(file.path + ".corrupt").toPath(), REPLACE_EXISTING)
            }
            empty
        }
    }

    fun write(text: String) {
        val temporary = File(file.path + ".tmp")
        file.absoluteFile.parentFile?.mkdirs()
        temporary.writeText(text)
        try {
            Files.move(temporary.toPath(), file.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), file.toPath(), REPLACE_EXISTING)
        }
    }
}
