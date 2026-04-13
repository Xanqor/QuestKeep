package com.questkeep.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val QuestKeepDarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryPurpleDark,
    onPrimaryContainer = PrimaryPurpleLight,
    secondary = SecondaryGold,
    onSecondary = OnSecondary,
    secondaryContainer = Color(0xFF3A2A00),
    onSecondaryContainer = SecondaryGoldLight,
    tertiary = AccentCyan,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = AccentRed,
    onError = OnPrimary,
    outline = Color(0xFF44445A),
    outlineVariant = Color(0xFF2A2A40)
)

@Composable
fun QuestKeepTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = QuestKeepDarkColorScheme,
        typography = QuestKeepTypography,
        content = content
    )
}
