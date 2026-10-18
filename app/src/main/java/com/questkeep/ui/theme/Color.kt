package com.questkeep.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ─── Deep Obsidian Fantasy Background & Surface Hierarchy ────────────────────
val DarkBackground = Color(0xFF0C0D14)       // Deepest obsidian night
val DarkSurface = Color(0xFF131524)          // Primary surfaces, top/bottom bars
val DarkCard = Color(0xFF1B1E32)             // Interactive cards and containers
val DarkSurfaceVariant = Color(0xFF262A45)   // Elevated chips, inputs, item slots
val DarkCardElevated = Color(0xFF22263E)     // Dialogs, modals, popups

// ─── Aesthetic RPG Borders & Rims ────────────────────────────────────────────
val BorderSubtle = Color(0xFF2E3352)         // Standard card outline
val BorderGlow = Color(0xFF48507D)           // Focused/hovered outline
val BorderGold = Color(0xFF8C6D23)           // Runic / GM / Wealth border
val BorderPurple = Color(0xFF7C3AED)         // Arcane / Magic border

// ─── Arcane Violet (Primary) ─────────────────────────────────────────────────
val PrimaryPurple = Color(0xFF8B5CF6)        // Vibrant violet
val PrimaryPurpleLight = Color(0xFFA78BFA)   // Bright lavender highlight
val PrimaryPurpleDark = Color(0xFF6D28D9)    // Deep mystical violet
val OnPrimary = Color(0xFFFFFFFF)

// ─── Dragon Gold (Secondary / GM / Wealth) ───────────────────────────────────
val SecondaryGold = Color(0xFFF59E0B)        // Rich warm amber gold
val SecondaryGoldLight = Color(0xFFFDE68A)   // Shimmering pale gold
val SecondaryGoldDark = Color(0xFFB45309)    // Antique bronze gold
val OnSecondary = Color(0xFF1A1200)

// ─── Combat & Status Accents ─────────────────────────────────────────────────
val AccentCyan = Color(0xFF06B6D4)           // Mana / dodge / celestial
val AccentGreen = Color(0xFF10B981)          // Healing / success / active buffs
val AccentRed = Color(0xFFEF4444)            // Damage / danger / critical fail
val AccentOrange = Color(0xFFF97316)         // Warning / whispers / torches

// ─── Stat Colors ─────────────────────────────────────────────────────────────
val StatColor = Color(0xFF94A3B8)            // Slate label
val StatValueColor = Color(0xFFF1F5F9)       // Crisp white stat value

// ─── Typography Text Colors ──────────────────────────────────────────────────
val TextPrimary = Color(0xFFF8FAFC)          // Crisp reading white
val TextSecondary = Color(0xFF94A3B8)        // Slate gray supporting
val TextDisabled = Color(0xFF475569)         // Muted / placeholder

// ─── Magic Resource Colors (Exact Xantheia Palette) ──────────────────────────
val ResourceQuarColor = Color(0xFF3B82F6)    // Куар — pure arcane azure
val ResourceOmbraColor = Color(0xFFA855F7)   // Омбра — shadow purple
val ResourceVerunaColor = Color(0xFF10B981)  // Веруна — nature emerald
val ResourceSumeniColor = Color(0xFFEF4444)  // Сумени — blood flame
val InspirationColor = Color(0xFFF59E0B)     // Inspiration — radiant star gold

// ─── Fantasy Gradient Brushes ────────────────────────────────────────────────
val ArcaneGradient = Brush.horizontalGradient(
    listOf(Color(0xFF7C3AED), Color(0xFF9333EA))
)
val GoldGradient = Brush.horizontalGradient(
    listOf(Color(0xFFD97706), Color(0xFFF59E0B), Color(0xFFFBBF24))
)
val CardGradient = Brush.verticalGradient(
    listOf(Color(0xFF1E2238), Color(0xFF161829))
)
val HealthGradient = Brush.horizontalGradient(
    listOf(Color(0xFFDC2626), Color(0xFFEF4444))
)
