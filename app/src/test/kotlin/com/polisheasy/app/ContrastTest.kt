package com.polisheasy.app

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.polisheasy.app.ui.theme.DarkColorScheme
import com.polisheasy.app.ui.theme.DarkPolishEasyColors
import com.polisheasy.app.ui.theme.LightColorScheme
import com.polisheasy.app.ui.theme.LightPolishEasyColors
import com.polisheasy.app.ui.theme.PolishEasyColors
import kotlin.test.assertTrue
import org.junit.Test

/**
 * The contrast docs/design.md promises, for every pair of colours a screen puts together, in both
 * themes: 4.5 to 1 for text and for icons that carry meaning, 3 to 1 for shapes and large numbers
 * (WCAG 2.1 AA). A screen that puts a new pair together adds it here.
 */
class ContrastTest {
    @Test
    fun `text and icons stand out from what they sit on in the light theme`() = check("light", LightColorScheme, LightPolishEasyColors)

    @Test
    fun `text and icons stand out from what they sit on in the dark theme`() = check("dark", DarkColorScheme, DarkPolishEasyColors)

    private class Pair(val what: String, val on: String, val colour: Color, val behind: Color, val least: Double)

    private fun check(theme: String, scheme: ColorScheme, colors: PolishEasyColors) {
        val surfaces = listOf(
            "background" to scheme.background,
            "a card" to scheme.surfaceContainerLowest,
            "the navigation bar" to scheme.surfaceContainer,
        )
        val pairs = buildList {
            fun text(what: String, colour: Color, on: String, behind: Color) = add(Pair(what, on, colour, behind, TEXT))
            fun shape(what: String, colour: Color, on: String, behind: Color) = add(Pair(what, on, colour, behind, SHAPE))

            surfaces.forEach { (name, surface) ->
                text("onSurface", scheme.onSurface, name, surface)
                text("onSurfaceVariant", scheme.onSurfaceVariant, name, surface)
                text("primary", scheme.primary, name, surface)
            }
            text("onSurfaceVariant", scheme.onSurfaceVariant, "a disabled button", scheme.surfaceContainerHighest)
            text("a prompt", scheme.onSurface, "its card", scheme.surfaceContainerHigh)
            shape("the slot of a sentence", scheme.outline, "secondaryContainer", scheme.secondaryContainer)
            text("onPrimaryContainer", scheme.onPrimaryContainer, "primaryContainer", scheme.primaryContainer)
            text("onSecondaryContainer", scheme.onSecondaryContainer, "secondaryContainer", scheme.secondaryContainer)
            text("onBrand", colors.onBrand, "brand", colors.brand)
            text("onSuccess", colors.onSuccess, "success", colors.success)
            text("onCaution", colors.onCaution, "caution", colors.caution)
            text("success", colors.success, "successContainer", colors.successContainer)
            text("onSuccessContainer", colors.onSuccessContainer, "successContainer", colors.successContainer)
            text("caution", colors.caution, "cautionContainer", colors.cautionContainer)
            text("onCautionContainer", colors.onCautionContainer, "cautionContainer", colors.cautionContainer)

            // Icons and shapes that say something, and numbers at display size.
            shape("primary", scheme.primary, "primaryContainer", scheme.primaryContainer)
            shape("the progress bar", colors.brand, "its track", scheme.surfaceContainer)
            shape("a completed stop", colors.completed, "a card", scheme.surfaceContainerLowest)
            shape("the tick on a completed stop", colors.onSuccess, "a completed stop", colors.completed)
            shape("the outline of an empty slot", scheme.outline, "its fill", scheme.surfaceContainerHigh)
            text("onPrimaryContainer", scheme.onPrimaryContainer, "a tonal button", scheme.primaryContainer)
            shape("the ring of the score", colors.success, "its track", scheme.surfaceContainerHigh)
            shape("a text field's border", scheme.outline, "background", scheme.background)
            shape("a switch that is off", scheme.outline, "a card", scheme.surfaceContainerLowest)
            shape("the tick on a completed day", scheme.onTertiary, "streak", colors.streak)
            // A verb's table being filled in a Game.
            text("the pronouns of a table", scheme.onSurfaceVariant, "secondaryContainer", scheme.secondaryContainer)
            shape("the tick of a row", colors.success, "secondaryContainer", scheme.secondaryContainer)
            shape("the cross of a row", colors.caution, "secondaryContainer", scheme.secondaryContainer)
            shape("the outline of the row asked for", scheme.primary, "secondaryContainer", scheme.secondaryContainer)
            listOf("background" to scheme.background, "a card" to scheme.surfaceContainerLowest).forEach { (name, surface) ->
                shape("brand", colors.brand, name, surface)
                shape("success", colors.success, name, surface)
                shape("streak", colors.streak, name, surface)
            }
        }
        val low = pairs.mapNotNull { pair ->
            val ratio = contrast(pair.colour, pair.behind)
            if (ratio >= pair.least) null else "${pair.what} on ${pair.on}: %.2f to 1, needs ${pair.least}".format(ratio)
        }
        assertTrue(low.isEmpty(), "Too little contrast in the $theme theme:\n" + low.joinToString("\n"))
    }

    private fun contrast(one: Color, other: Color): Double {
        val lighter = maxOf(one.luminance(), other.luminance())
        val darker = minOf(one.luminance(), other.luminance())
        return (lighter + 0.05) / (darker + 0.05)
    }

    private companion object {
        const val TEXT = 4.5
        const val SHAPE = 3.0
    }
}
