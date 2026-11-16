package com.polisheasy.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.polisheasy.app.R

/** Bundled, so the app stays offline. Its licence text is `res/raw/nunito_ofl.txt`. */
private val Nunito = FontFamily(
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

private fun nunito(weight: FontWeight, size: Int, line: Int) =
    TextStyle(fontFamily = Nunito, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

/** The phone's own sans-serif, for body text. */
private fun system(weight: FontWeight, size: Int, line: Int) =
    TextStyle(fontFamily = FontFamily.Default, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

/** The type scale of docs/design.md. Every style is set, so none falls back to Material's own. */
internal val PolishEasyTypography = Typography(
    displayLarge = nunito(FontWeight.ExtraBold, 57, 64),
    displayMedium = nunito(FontWeight.ExtraBold, 45, 52),
    displaySmall = nunito(FontWeight.ExtraBold, 36, 44),
    headlineLarge = nunito(FontWeight.ExtraBold, 32, 40),
    headlineMedium = nunito(FontWeight.Bold, 26, 34),
    headlineSmall = nunito(FontWeight.Bold, 22, 28),
    titleLarge = nunito(FontWeight.Bold, 20, 26),
    titleMedium = nunito(FontWeight.SemiBold, 18, 24),
    titleSmall = nunito(FontWeight.Bold, 14, 20),
    bodyLarge = system(FontWeight.Normal, 16, 24),
    bodyMedium = system(FontWeight.Normal, 14, 20),
    bodySmall = system(FontWeight.Normal, 12, 16),
    labelLarge = nunito(FontWeight.Bold, 16, 24),
    labelMedium = system(FontWeight.Medium, 14, 20),
    labelSmall = system(FontWeight.Medium, 12, 16),
)
