package com.polisheasy.app.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.Journey
import com.polisheasy.app.ui.components.LessonCard
import com.polisheasy.app.ui.components.ScreenFrame
import com.polisheasy.app.ui.components.TopBar
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.LessonCardView
import com.polisheasy.engine.ShelfView

/**
 * The Path as shelves: Starter, the Units the app began with, then the book, *Polski krok po
 * kroku 1*, a card for each of its chapters in the book's order and colours. Each card says how
 * far through the chapter the Learner is, and wears the seal once its Test is passed. A chapter
 * still being written is "coming soon". Tapping a card opens its Lesson screen with [onOpen].
 *
 * It opens at the chapter the Learner is at, the first with a section not completed.
 */
@Composable
internal fun PathScreen(shelves: List<ShelfView>, onOpen: (String) -> Unit) {
    val current = shelves.flatMap { it.lessons }.firstOrNull { it.current }
    ScreenFrame {
        Journey(modifier = Modifier.fillMaxSize(), opensAtTop = current == null) { marks ->
            shelves.forEachIndexed { index, shelf ->
                if (index > 0) Spacer(Modifier.height(Space.l))
                val sections = shelf.lessons.sumOf { it.sections }
                TopBar(
                    shelf.title,
                    note = stringResource(R.string.path_shelf_progress, shelf.lessons.sumOf { it.sectionsDone }, sections),
                )
                shelf.lessons.forEach { lesson ->
                    PathLesson(lesson, modifier = if (lesson.current) marks.stop else Modifier, onOpen = onOpen)
                    Spacer(Modifier.height(Space.xs))
                }
            }
            Spacer(Modifier.height(Space.m))
        }
    }
}

/** One chapter's card: a book chapter by its lesson number, a Starter Unit by its place on the shelf. */
@Composable
private fun PathLesson(lesson: LessonCardView, modifier: Modifier, onOpen: (String) -> Unit) {
    LessonCard(
        number = (lesson.number ?: lesson.place).toString(),
        title = lesson.title,
        subtitle = lesson.subtitle,
        progress = when {
            lesson.coming -> stringResource(R.string.lesson_card_coming)
            else -> stringResource(R.string.lesson_card_progress, lesson.sectionsDone, lesson.sections)
        },
        accent = lesson.accent,
        onClick = { onOpen(lesson.unitId) },
        onClickLabel = stringResource(R.string.lesson_open),
        modifier = modifier,
        done = lesson.sections > 0 && lesson.sectionsDone == lesson.sections,
        sealed = lesson.sealed,
        sealedLabel = stringResource(R.string.lesson_sealed),
        coming = lesson.coming,
        currentLabel = if (lesson.current) stringResource(R.string.lesson_card_current) else null,
    )
}
