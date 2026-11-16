package com.polisheasy.app.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * What the Learner feels (docs/design.md, Haptics). It goes through the view, so the phone's own
 * touch setting is respected and no permission is needed. Nothing else in the app vibrates.
 */
class AnswerHaptics internal constructor(private val view: View) {
    /** A right answer. */
    fun right() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.KEYBOARD_TAP,
    )

    /** A wrong answer. */
    fun wrong() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS,
    )

    /** A Daily Set finished, felt once as the Summary appears. */
    fun finished() = right()

    /** A word tile placed. */
    fun tick() = perform(HapticFeedbackConstants.CLOCK_TICK)

    private fun perform(feedback: Int) {
        view.performHapticFeedback(feedback)
    }
}

@Composable
fun rememberAnswerHaptics(): AnswerHaptics {
    val view = LocalView.current
    return remember(view) { AnswerHaptics(view) }
}
