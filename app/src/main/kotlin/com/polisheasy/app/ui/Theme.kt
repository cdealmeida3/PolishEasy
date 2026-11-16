package com.polisheasy.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.polisheasy.app.ui.theme.DarkColorScheme
import com.polisheasy.app.ui.theme.DarkPolishEasyColors
import com.polisheasy.app.ui.theme.LightColorScheme
import com.polisheasy.app.ui.theme.LightPolishEasyColors
import com.polisheasy.app.ui.theme.LocalPolishEasyColors
import com.polisheasy.app.ui.theme.PolishEasyColors
import com.polisheasy.app.ui.theme.PolishEasyShapes
import com.polisheasy.app.ui.theme.PolishEasyTypography

/**
 * The look of the app (docs/design.md). The values are in `ui/theme`. Dynamic colour stays off, so
 * the app looks the same on every phone.
 */
@Composable
fun PolishEasyTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalPolishEasyColors provides if (dark) DarkPolishEasyColors else LightPolishEasyColors) {
        MaterialTheme(
            colorScheme = if (dark) DarkColorScheme else LightColorScheme,
            typography = PolishEasyTypography,
            shapes = PolishEasyShapes,
            content = content,
        )
    }
}

/** The PolishEasy roles, next to `MaterialTheme.colorScheme`: `PolishEasyTheme.colors.success`. */
object PolishEasyTheme {
    val colors: PolishEasyColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPolishEasyColors.current
}
