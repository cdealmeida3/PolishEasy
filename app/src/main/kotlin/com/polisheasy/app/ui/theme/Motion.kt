package com.polisheasy.app.ui.theme

import android.provider.Settings
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset

/** Named durations and springs (docs/design.md, Motion and touch). Components ask here; screens do not. */
object Motion {
    /** Pressing a button, card or tile. */
    const val PRESS_MILLIS = 80

    /** From one Tip or Exercise to the next, and the feedback panel rising. */
    const val STEP_MILLIS = 250

    /** A word tile travelling to its slot or back. */
    const val TILE_MILLIS = 200

    /** The one shake of the option that was wrong. */
    const val SHAKE_MILLIS = 250

    /** Half a pulse of the play button while a Clip plays, and how far it grows. */
    const val PULSE_MILLIS = 700
    const val PULSE_SCALE = 1.06f

    /** The size a tick or a cross starts from as it appears. */
    const val MARK_FROM_SCALE = 0.8f

    /** How far an option that was not chosen fades once the Exercise is answered. */
    const val FADED_ALPHA = 0.6f

    /**
     * Whether Android's "remove animations" is on (the animator duration scale is 0). Then every
     * change is instant and the final state shows at once.
     */
    @Composable
    fun reduced(): Boolean {
        val resolver = LocalContext.current.contentResolver
        return remember(resolver) {
            Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }
    }

    /** A control giving way under the finger. */
    @Composable
    fun <T> press(): AnimationSpec<T> = if (reduced()) snap() else tween(PRESS_MILLIS)

    /** The progress bar reaching its new value, with a little bounce. */
    @Composable
    fun progress(): AnimationSpec<Float> = if (reduced()) {
        snap()
    } else {
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
    }

    /** The old Tip or Exercise slides out to the left and fades, the new one slides in from the right. */
    @Composable
    fun step(): ContentTransform = if (reduced()) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        val slide = tween<IntOffset>(STEP_MILLIS, easing = LinearOutSlowInEasing)
        (slideInHorizontally(slide) { it } + fadeIn(tween(STEP_MILLIS))) togetherWith
            (slideOutHorizontally(slide) { -it } + fadeOut(tween(STEP_MILLIS)))
    }

    /** The feedback panel rising from the bottom. */
    @Composable
    fun panelEnter(): EnterTransition = if (reduced()) {
        EnterTransition.None
    } else {
        slideInVertically(tween(STEP_MILLIS, easing = LinearOutSlowInEasing)) { it }
    }

    /** A word tile reaching its place, a little past it and back. */
    @Composable
    fun tile(): AnimationSpec<Float> = if (reduced()) snap() else tween(TILE_MILLIS, easing = Overshoot)

    /** A tick or a cross appearing, with a little bounce. */
    @Composable
    fun mark(): AnimationSpec<Float> = if (reduced()) {
        snap()
    } else {
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    }

    /**
     * One short side-to-side shake, as a share of the distance: run it from 0 to 0. Null when
     * animations are removed, and then nothing shakes.
     */
    @Composable
    fun shake(): AnimationSpec<Float>? = if (reduced()) {
        null
    } else {
        keyframes {
            durationMillis = SHAKE_MILLIS
            -1f at SHAKE_MILLIS / 5
            1f at SHAKE_MILLIS * 2 / 5
            -0.6f at SHAKE_MILLIS * 3 / 5
            0.4f at SHAKE_MILLIS * 4 / 5
        }
    }

    /** One way of the play button's slow pulse. Null when animations are removed, and then it is still. */
    @Composable
    fun pulse(): DurationBasedAnimationSpec<Float>? = if (reduced()) null else tween(PULSE_MILLIS)

    private val Overshoot = Easing { OvershootInterpolator(1.5f).getInterpolation(it) }
}
