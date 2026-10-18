package com.questkeep.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.questkeep.data.local.SessionManager

@Composable
fun QuestKeepTheme(
    sessionManager: SessionManager? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val manager = sessionManager ?: remember { SessionManager(context.applicationContext) }
    val currentThemeKey by manager.selectedTheme.collectAsState()
    val activeTheme = getThemeByKey(currentThemeKey)

    val colorScheme = darkColorScheme(
        primary = activeTheme.colors.accent,
        onPrimary = activeTheme.colors.bg,
        primaryContainer = activeTheme.colors.panel,
        onPrimaryContainer = activeTheme.colors.text,
        secondary = activeTheme.colors.hot,
        onSecondary = Color.White,
        secondaryContainer = activeTheme.colors.panel,
        onSecondaryContainer = activeTheme.colors.hot,
        tertiary = activeTheme.colors.chaos,
        background = activeTheme.colors.bg,
        onBackground = activeTheme.colors.text,
        surface = activeTheme.colors.panel,
        onSurface = activeTheme.colors.text,
        surfaceVariant = activeTheme.colors.panel,
        onSurfaceVariant = activeTheme.colors.muted,
        error = activeTheme.colors.hot,
        onError = Color.White,
        outline = activeTheme.colors.line,
        outlineVariant = activeTheme.colors.line.copy(alpha = 0.5f)
    )

    CompositionLocalProvider(LocalXanteya provides activeTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = QuestKeepTypography,
            content = content
        )
    }
}
