package com.vidi.droidxadrez

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

object Theme {
    val bg = Color(0xFF0B0A08)
    val bgSoft = Color(0xFF141208)
    val panel = Color(0xFF1B180F)
    val panelBorder = Color(0xFF2F2A1F)
    val ink = Color(0xFFF3EDE1)
    val inkDim = Color(0xFFB8AE98)
    val gold = Color(0xFFD4AF37)
    val goldSoft = Color(0xFFE8C765)
    val danger = Color(0xFFC0503F)
    val squareLight = Color(0xFFEFE1C4)
    val squareLight2 = Color(0xFFE1D0A2)
    val squareDark = Color(0xFF714A36)
    val squareDark2 = Color(0xFF563824)

    val soraFamily = FontFamily(
        Font(R.font.sora_regular, FontWeight.Normal),
        Font(R.font.sora_semibold, FontWeight.SemiBold),
        Font(R.font.sora_bold, FontWeight.Bold),
        Font(R.font.sora_extrabold, FontWeight.ExtraBold),
    )

    val whitePieceBrush = Brush.verticalGradient(listOf(Color(0xFFFDF9EE), Color(0xFFE9C96A)))
    val blackPieceBrush = Brush.verticalGradient(listOf(Color(0xFF3A3020), Color(0xFF0D0B06)))
    val lightSquareBrush = Brush.linearGradient(listOf(squareLight, squareLight2))
    val darkSquareBrush = Brush.linearGradient(listOf(squareDark, squareDark2))
}

@Composable
fun XadrezTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        background = Theme.bg,
        surface = Theme.panel,
        primary = Theme.gold,
        onBackground = Theme.ink,
        onSurface = Theme.ink,
    )
    val baseline = Typography()
    val typography = Typography(
        displayLarge = baseline.displayLarge.copy(fontFamily = Theme.soraFamily),
        displayMedium = baseline.displayMedium.copy(fontFamily = Theme.soraFamily),
        displaySmall = baseline.displaySmall.copy(fontFamily = Theme.soraFamily),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = Theme.soraFamily),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = Theme.soraFamily),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = Theme.soraFamily),
        titleLarge = baseline.titleLarge.copy(fontFamily = Theme.soraFamily),
        titleMedium = baseline.titleMedium.copy(fontFamily = Theme.soraFamily),
        titleSmall = baseline.titleSmall.copy(fontFamily = Theme.soraFamily),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = Theme.soraFamily),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = Theme.soraFamily),
        bodySmall = baseline.bodySmall.copy(fontFamily = Theme.soraFamily),
        labelLarge = baseline.labelLarge.copy(fontFamily = Theme.soraFamily),
        labelMedium = baseline.labelMedium.copy(fontFamily = Theme.soraFamily),
        labelSmall = baseline.labelSmall.copy(fontFamily = Theme.soraFamily),
    )
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
