package com.polisheasy.app.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.CardNote
import com.polisheasy.app.ui.components.ContentCard
import com.polisheasy.app.ui.components.DetailFrame
import com.polisheasy.app.ui.components.LessonBanner
import com.polisheasy.app.ui.components.SectionHeading
import com.polisheasy.app.ui.components.TopBar
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.ClipView
import com.polisheasy.engine.GrammarChapterView

/**
 * The Grammar shelf, the book's *Komentarze gramatyczne*: by chapter, every table from the sections
 * the Learner has completed, each as its Tip showed it. With [one] it is a chapter's own, opened
 * from its Lesson screen; else Settings opened it, and it has every chapter with a table so far.
 */
@Composable
internal fun GrammarScreen(
    chapters: List<GrammarChapterView>,
    one: Boolean,
    clipOf: (String) -> ClipView?,
    onPlay: (ClipView) -> Unit,
    onBack: () -> Unit,
) {
    DetailFrame(backLabel = stringResource(R.string.grammar_back), onBack = onBack) {
        val chapter = chapters.singleOrNull()?.takeIf { one }
        if (chapter != null) {
            LessonBanner(
                label = chapter.number?.let { stringResource(R.string.lesson_label, it) } ?: stringResource(R.string.grammar_title),
                number = chapter.number?.toString() ?: "",
                title = stringResource(R.string.grammar_title),
                subtitle = chapter.title,
                accent = chapter.accent,
            )
        } else {
            TopBar(stringResource(R.string.grammar_title), note = stringResource(R.string.grammar_note))
        }
        if (chapters.all { it.tips.isEmpty() }) {
            Spacer(Modifier.height(Space.m))
            CardNote(stringResource(R.string.grammar_none))
        }
        chapters.forEach { shelf ->
            if (chapter == null) {
                Spacer(Modifier.height(Space.l))
                SectionHeading(shelf.number?.let { stringResource(R.string.grammar_chapter, it, shelf.title) } ?: shelf.title)
            }
            shelf.tips.forEach { tip ->
                Spacer(Modifier.height(Space.m))
                ContentCard { TipContent(tip, clipOf, onPlay) }
            }
            if (shelf.toCome > 0) {
                Spacer(Modifier.height(Space.s))
                CardNote(pluralStringResource(R.plurals.grammar_to_come, shelf.toCome, shelf.toCome))
            }
        }
    }
}
