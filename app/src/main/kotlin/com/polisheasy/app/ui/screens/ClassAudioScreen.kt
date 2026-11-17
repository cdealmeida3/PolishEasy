package com.polisheasy.app.ui.screens

import android.content.ActivityNotFoundException
import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.CardNote
import com.polisheasy.app.ui.components.DetailFrame
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.Point
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.QuietButton
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.SecondaryButton
import com.polisheasy.app.ui.components.StatRow
import com.polisheasy.app.ui.components.StatTile
import com.polisheasy.app.ui.components.TopBar
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.ClassAudioView
import com.polisheasy.engine.ImportResult

/** The kinds of file the picker offers: the class's zips, and single recordings. */
private val pickable = arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream", "audio/mpeg")

/**
 * Class audio: how many of the book's 271 recordings are on the phone and the room they take, and
 * adding them from the class's zips (several at once) or from a folder, through Android's own
 * picker, or removing them. They are copied into the app and never leave the phone (ADR-0014).
 */
@Composable
internal fun ClassAudioScreen(
    view: ClassAudioView,
    importing: Boolean,
    result: ImportResult?,
    failed: Boolean,
    onFiles: (List<Uri>) -> Unit,
    onFolder: (Uri) -> Unit,
    onRemove: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var removing by rememberSaveable { mutableStateOf(false) }
    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { picked ->
        if (picked.isNotEmpty()) onFiles(picked)
    }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { picked ->
        picked?.let(onFolder)
    }
    val size = Formatter.formatShortFileSize(context, view.bytes)
    DetailFrame(
        backLabel = stringResource(R.string.class_audio_back),
        onBack = onBack,
        action = {
            PrimaryButton(
                stringResource(if (importing) R.string.class_audio_importing else R.string.class_audio_add_zips),
                onClick = { if (!importing) launch { pickFiles.launch(pickable) } },
            )
            Spacer(Modifier.height(Space.xs))
            SecondaryButton(
                stringResource(R.string.class_audio_add_folder),
                onClick = { if (!importing) launch { pickFolder.launch(null) } },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        TopBar(stringResource(R.string.class_audio_title))
        CardNote(stringResource(R.string.class_audio_note))
        Spacer(Modifier.height(Space.l))
        StatRow(
            tiles = listOf(
                { modifier ->
                    StatTile(
                        stringResource(R.string.class_audio_count, view.imported, view.total),
                        stringResource(R.string.class_audio_count_label),
                        stringResource(R.string.class_audio_count_sentence, view.imported, view.total, size),
                        modifier,
                    )
                },
                { modifier ->
                    val label = stringResource(R.string.class_audio_size_label)
                    StatTile(size, label, "$size $label", modifier)
                },
            ),
        )
        Spacer(Modifier.height(Space.m))
        when {
            failed -> RoundNote(Mark.Missed, stringResource(R.string.class_audio_failed))
            result != null -> {
                RoundNote(
                    if (result.matched > 0) Mark.Right else Mark.Missed,
                    pluralStringResource(R.plurals.class_audio_added, result.matched, result.matched),
                )
                if (result.unmatched.isNotEmpty()) {
                    Spacer(Modifier.height(Space.xs))
                    CardNote(pluralStringResource(R.plurals.class_audio_unmatched, result.unmatched.size, result.unmatched.size))
                }
            }
        }
        Spacer(Modifier.height(Space.m))
        Point(R.drawable.ic_library_add, stringResource(R.string.class_audio_how), stringResource(R.string.class_audio_add_note))
        if (view.imported > 0) {
            Spacer(Modifier.height(Space.m))
            QuietButton(stringResource(R.string.class_audio_remove), onClick = { if (!importing) removing = true })
        }
    }
    if (removing) {
        AlertDialog(
            onDismissRequest = { removing = false },
            confirmButton = {
                QuietButton(stringResource(R.string.class_audio_remove_confirm), onClick = {
                    removing = false
                    onRemove()
                })
            },
            dismissButton = { QuietButton(stringResource(R.string.action_cancel), { removing = false }) },
            title = { Text(stringResource(R.string.class_audio_remove_title)) },
            text = { Text(stringResource(R.string.class_audio_remove_note)) },
        )
    }
}

/** Opens Android's picker; a phone without one has nothing to open. */
private fun launch(open: () -> Unit) {
    try {
        open()
    } catch (_: ActivityNotFoundException) {
        // Nothing to pick with.
    }
}
