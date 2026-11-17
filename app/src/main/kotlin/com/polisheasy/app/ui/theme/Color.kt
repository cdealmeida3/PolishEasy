package com.polisheasy.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Every colour of the app, as docs/design.md gives it. Nothing outside this package holds a colour value.

private val Crimson = Color(0xFFC8102E)
private val CrimsonPale = Color(0xFFFFDAD9)
private val CrimsonPink = Color(0xFFFFB3B5)
private val CrimsonDeep = Color(0xFF410008)
private val FlameLight = Color(0xFFE2570B)
private val FlameDark = Color(0xFFFF8A3D)
private val White = Color(0xFFFFFFFF)
private val Black = Color(0xFF000000)

/** Every Material role is set, so none falls back to Material's purple baseline. */
internal val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Crimson,
    onPrimary = White,
    primaryContainer = CrimsonPale,
    onPrimaryContainer = CrimsonDeep,
    inversePrimary = CrimsonPink,
    secondary = Color(0xFF5A4740),
    onSecondary = White,
    secondaryContainer = Color(0xFFF7E6DB),
    onSecondaryContainer = Color(0xFF2B1D17),
    tertiary = FlameLight,
    onTertiary = White,
    tertiaryContainer = Color(0xFFFFDBCB),
    onTertiaryContainer = Color(0xFF351000),
    background = Color(0xFFFFF8F3),
    onBackground = Color(0xFF231A16),
    surface = Color(0xFFFFF8F3),
    onSurface = Color(0xFF231A16),
    surfaceVariant = Color(0xFFF7E6DB),
    onSurfaceVariant = Color(0xFF5A4740),
    surfaceTint = Crimson,
    inverseSurface = Color(0xFF382E2A),
    inverseOnSurface = Color(0xFFFFEDE5),
    error = Color(0xFFBA1A1A),
    onError = White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF8C7369),
    outlineVariant = Color(0xFFDEC8BC),
    scrim = Black,
    surfaceBright = Color(0xFFFFF8F3),
    surfaceDim = Color(0xFFE8D7CC),
    surfaceContainerLowest = White,
    surfaceContainerLow = Color(0xFFFFF1E8),
    surfaceContainer = Color(0xFFFBEADF),
    surfaceContainerHigh = Color(0xFFF7E6DB),
    surfaceContainerHighest = Color(0xFFEFDFD4),
)

internal val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = CrimsonPink,
    onPrimary = Color(0xFF680012),
    primaryContainer = Color(0xFF7A0A1C),
    onPrimaryContainer = CrimsonPale,
    inversePrimary = Crimson,
    secondary = Color(0xFFD9C3B9),
    onSecondary = Color(0xFF2B1D17),
    secondaryContainer = Color(0xFF2B211E),
    onSecondaryContainer = Color(0xFFF2E0D8),
    tertiary = FlameDark,
    onTertiary = Color(0xFF3A1400),
    tertiaryContainer = Color(0xFF7A2E00),
    onTertiaryContainer = Color(0xFFFFDBCB),
    background = Color(0xFF1A1311),
    onBackground = Color(0xFFF2E0D8),
    surface = Color(0xFF1A1311),
    onSurface = Color(0xFFF2E0D8),
    surfaceVariant = Color(0xFF2B211E),
    onSurfaceVariant = Color(0xFFD9C3B9),
    surfaceTint = CrimsonPink,
    inverseSurface = Color(0xFFF2E0D8),
    inverseOnSurface = Color(0xFF382E2A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFFA18C82),
    outlineVariant = Color(0xFF4A3B35),
    scrim = Black,
    surfaceBright = Color(0xFF42362F),
    surfaceDim = Color(0xFF1A1311),
    surfaceContainerLowest = Color(0xFF140E0C),
    surfaceContainerLow = Color(0xFF211917),
    surfaceContainer = Color(0xFF261D1A),
    surfaceContainerHigh = Color(0xFF2B211E),
    surfaceContainerHighest = Color(0xFF362B27),
)

/**
 * The roles Material has none for: the main button's fill, a right answer, a wrong answer, the
 * Streak and a completed stop on the Path. Read through [PolishEasyTheme.colors][com.polisheasy.app.ui.PolishEasyTheme].
 */
@Immutable
data class PolishEasyColors(
    val brand: Color,
    val onBrand: Color,
    val brandEdge: Color,
    val success: Color,
    val onSuccess: Color,
    val successEdge: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val caution: Color,
    val onCaution: Color,
    val cautionEdge: Color,
    val cautionContainer: Color,
    val onCautionContainer: Color,
    val streak: Color,
    val completed: Color,
    val completedEdge: Color,
    /** The eight lesson colours, as the book gives each chapter one. A chapter's accent is `accents[accent % 8]`. */
    val accents: List<AccentColors> = emptyList(),
    /** A sticky note, the book's word bank for gaps: a pale yellow face, a darker edge, and its words. */
    val note: Color = Color.Unspecified,
    val noteEdge: Color = Color.Unspecified,
    val onNote: Color = Color.Unspecified,
)

/**
 * One lesson colour: a strong [fill] for the chapter's banner and number with [onFill] on it, and a
 * soft [container] for its chips and cards with [onContainer] on it.
 */
@Immutable
data class AccentColors(val fill: Color, val onFill: Color, val container: Color, val onContainer: Color)

// Red, teal, purple, green, slate, olive, brown and lavender, as the book's chapters are coloured.
private val LightAccents = listOf(
    AccentColors(Color(0xFFC8102E), White, Color(0xFFFFDAD9), Color(0xFF410008)),
    AccentColors(Color(0xFF00696F), White, Color(0xFFC9F0F0), Color(0xFF002022)),
    AccentColors(Color(0xFF6B3FA0), White, Color(0xFFEBDDFF), Color(0xFF250059)),
    AccentColors(Color(0xFF2E7D32), White, Color(0xFFD3F2D3), Color(0xFF07390A)),
    AccentColors(Color(0xFF455A75), White, Color(0xFFD6E3F7), Color(0xFF0F1D2E)),
    AccentColors(Color(0xFF5E6816), White, Color(0xFFE5EDB8), Color(0xFF1B1F00)),
    AccentColors(Color(0xFF8D4E2A), White, Color(0xFFFFDBC9), Color(0xFF341100)),
    AccentColors(Color(0xFF5B4FB5), White, Color(0xFFE4DFFF), Color(0xFF170A57)),
)

private val DarkAccents = listOf(
    AccentColors(Color(0xFFB0102B), White, Color(0xFF5C0A17), Color(0xFFFFDAD9)),
    AccentColors(Color(0xFF00666C), White, Color(0xFF00363A), Color(0xFFC9F0F0)),
    AccentColors(Color(0xFF65409A), White, Color(0xFF3A2266), Color(0xFFEBDDFF)),
    AccentColors(Color(0xFF2E7032), White, Color(0xFF173C19), Color(0xFFD3F2D3)),
    AccentColors(Color(0xFF475B74), White, Color(0xFF253244), Color(0xFFD6E3F7)),
    AccentColors(Color(0xFF5A6318), White, Color(0xFF30360A), Color(0xFFE5EDB8)),
    AccentColors(Color(0xFF85492A), White, Color(0xFF4A2512), Color(0xFFFFDBC9)),
    AccentColors(Color(0xFF574BAA), White, Color(0xFF2F276A), Color(0xFFE4DFFF)),
)

internal val LightPolishEasyColors = PolishEasyColors(
    brand = Crimson,
    onBrand = White,
    brandEdge = Color(0xFF9A0A22),
    success = Color(0xFF17733A),
    onSuccess = White,
    successEdge = Color(0xFF0E5428),
    successContainer = Color(0xFFD3F2DA),
    onSuccessContainer = Color(0xFF0A3A1B),
    caution = Color(0xFF8A5200),
    onCaution = White,
    cautionEdge = Color(0xFF643B00),
    cautionContainer = Color(0xFFFFE3B3),
    onCautionContainer = Color(0xFF3A2200),
    streak = FlameLight,
    completed = Color(0xFF17733A),
    completedEdge = Color(0xFF0E5428),
    accents = LightAccents,
    note = Color(0xFFFFF1A6),
    noteEdge = Color(0xFFD9B93A),
    onNote = Color(0xFF332800),
)

internal val DarkPolishEasyColors = PolishEasyColors(
    brand = Color(0xFFD11F3B),
    onBrand = White,
    brandEdge = Color(0xFF8E0B22),
    success = Color(0xFF6FDB8C),
    onSuccess = Color(0xFF00390F),
    successEdge = Color(0xFF3FA65C),
    successContainer = Color(0xFF0F3D1F),
    onSuccessContainer = Color(0xFFBFF5CB),
    caution = Color(0xFFFFBA4A),
    onCaution = Color(0xFF3A2200),
    cautionEdge = Color(0xFFC98A1E),
    cautionContainer = Color(0xFF4A2C00),
    onCautionContainer = Color(0xFFFFE3B3),
    streak = FlameDark,
    completed = Color(0xFF3FA65C),
    completedEdge = Color(0xFF2B7A42),
    accents = DarkAccents,
    note = Color(0xFF4A4015),
    noteEdge = Color(0xFF7A6A22),
    onNote = Color(0xFFFFF1A6),
)

internal val LocalPolishEasyColors = staticCompositionLocalOf { LightPolishEasyColors }
