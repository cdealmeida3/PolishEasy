package com.polisheasy.app.ui.screens.lesson

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.CardNote
import com.polisheasy.app.ui.components.ContentCard
import com.polisheasy.app.ui.components.DetailFrame
import com.polisheasy.app.ui.components.DictionaryRow
import com.polisheasy.app.ui.components.RegisterChip
import com.polisheasy.app.ui.components.SearchField
import com.polisheasy.app.ui.components.SpeakerButton
import com.polisheasy.app.ui.components.TopBar
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.ClipView
import com.polisheasy.engine.LessonView
import com.polisheasy.engine.WordView
import com.polisheasy.engine.matching

/**
 * A chapter's Słownik: every word it teaches, in the order it does, each with its meaning, its
 * register when the book marks one, and a speaker. A field at the top finds a word by its Polish,
 * with or without its marks, or by its meaning.
 */
@Composable
internal fun DictionaryScreen(lesson: LessonView, words: List<WordView>, sound: Boolean, onPlay: (ClipView) -> Unit, onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    DetailFrame(backLabel = stringResource(R.string.dictionary_back), onBack = onBack) {
        TopBar(stringResource(R.string.dictionary_title), note = "${lessonName(lesson)} · ${lesson.title}")
        SearchField(query, onQueryChange = { query = it }, label = stringResource(R.string.dictionary_search))
        Spacer(Modifier.height(Space.m))
        val found = words.matching(query)
        if (found.isEmpty()) {
            CardNote(stringResource(R.string.dictionary_none, query.trim()))
            return@DetailFrame
        }
        ContentCard {
            found.forEachIndexed { index, word ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DictionaryRow(
                    text = word.text,
                    meaning = word.meaning,
                    register = word.register?.let { register ->
                        { RegisterChip(register.mark, description = registerWords(register)) }
                    },
                    speaker = word.audio?.takeIf { sound }?.let { clip ->
                        { SpeakerButton(stringResource(R.string.audio_play), onClick = { onPlay(clip) }) }
                    },
                )
            }
        }
    }
}
