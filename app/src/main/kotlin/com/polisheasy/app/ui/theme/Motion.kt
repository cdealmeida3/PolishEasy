package com.polisheasy.app.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Named durations and springs (docs/design.md, Motion and touch). Components ask here; screens do not. */
object Motion {
    /** Pressing a button, card or tile. */
    const val PRESS_MILLIS = 80

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
}
