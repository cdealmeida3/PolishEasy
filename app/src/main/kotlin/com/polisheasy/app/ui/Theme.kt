package com.polisheasy.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PolishRed = Color(0xFFC8102E)

private val LightColors = lightColorScheme(
    primary = PolishRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD9),
    onPrimaryContainer = Color(0xFF410008),
    secondary = Color(0xFF775656),
    background = Color(0xFFFFFBFF),
    surface = Color(0xFFFFFBFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB3B3),
    onPrimary = Color(0xFF680012),
    primaryContainer = Color(0xFF92001E),
    onPrimaryContainer = Color(0xFFFFDAD9),
    secondary = Color(0xFFE6BDBC),
    background = Color(0xFF201A1A),
    surface = Color(0xFF201A1A),
)

@Composable
fun PolishEasyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
