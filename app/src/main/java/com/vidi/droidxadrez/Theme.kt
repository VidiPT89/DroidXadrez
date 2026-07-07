package com.vidi.droidxadrez

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    val squareLight = Color(0xFFECDFC4)
    val squareDark = Color(0xFF6C4A34)
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
    MaterialTheme(colorScheme = colors, content = content)
}
