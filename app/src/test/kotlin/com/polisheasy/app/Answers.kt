package com.polisheasy.app

import com.polisheasy.engine.Answer
import com.polisheasy.engine.ChooseTranslation
import com.polisheasy.engine.Conjugation
import com.polisheasy.engine.Course
import com.polisheasy.engine.ExerciseView
import com.polisheasy.engine.FillGaps
import com.polisheasy.engine.MatchPairs
import com.polisheasy.engine.OrderLines
import com.polisheasy.engine.TrueFalse
import com.polisheasy.engine.ListenChoose
import com.polisheasy.engine.ListenType
import com.polisheasy.engine.TypeAnswer
import com.polisheasy.engine.WordTiles

/** The right answer to the Exercise [exerciseId] of [course], as it shows in [view]: for a test to play through content it does not know. */
fun rightAnswer(course: Course, exerciseId: String, view: ExerciseView): Answer = when (val exercise = course.exercises.getValue(exerciseId)) {
    is ChooseTranslation -> Answer.Option(exercise.correctOption)
    is TypeAnswer -> Answer.Typed(exercise.acceptedAnswers.first())
    is WordTiles -> Answer.Tiles(exercise.acceptedAnswers.first().replace(Regex("[.,!?;:]"), "").split(Regex("\\s+")).filter { it.isNotEmpty() })
    is Conjugation -> Answer.Typed(exercise.acceptedAnswers.first())
    is ListenChoose -> Answer.Option(
        when (view) {
            is ExerciseView.ListenChoose -> view.options
            is ExerciseView.ChoosePolish -> view.options
            else -> error("A listening choice shows as $view")
        }.indexOf(exercise.text),
    )
    is ListenType -> Answer.Typed(exercise.acceptedAnswers.first())
    is FillGaps -> Answer.Gaps(exercise.gaps.map { it.first() })
    is MatchPairs -> Answer.Pairs(wrong = 0)
    is TrueFalse -> Answer.Marks(exercise.statements.map { it.`true` })
    is OrderLines -> Answer.Order(exercise.lines)
}

/** An answer of the same kind as [right] that is wrong. */
fun wrongAnswer(right: Answer): Answer = when (right) {
    is Answer.Option -> Answer.Option(if (right.index == 0) 1 else 0)
    is Answer.Typed -> Answer.Typed("xyz")
    is Answer.Tiles -> Answer.Tiles(listOf("xyz"))
    is Answer.Gaps -> Answer.Gaps(right.words.map { "xyz" })
    is Answer.Pairs -> Answer.Pairs(wrong = 1)
    is Answer.Marks -> Answer.Marks(right.values.map { !it })
    is Answer.Order -> Answer.Order(right.lines.reversed())
}
