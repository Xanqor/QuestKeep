package com.questkeep.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.questkeep.ui.theme.*

// ─── HP Bar ──────────────────────────────────────────────────────────────────

@Composable
fun HpBar(
    current: Int,
    max: Int,
    modifier: Modifier = Modifier
) {
    val fraction = if (max == 0) 0f else (current.toFloat() / max).coerceIn(0f, 1f)
    val barColor = when {
        fraction > 0.6f -> AccentGreen
        fraction > 0.3f -> AccentOrange
        else            -> AccentRed
    }
    val animatedFraction by animateFloatAsState(targetValue = fraction, label = "hp")

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("HP", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Text(
                "$current / $max",
                style = MaterialTheme.typography.labelLarge,
                color = barColor, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        ProgressBar(animatedFraction, barColor, height = 12.dp)
    }
}

// ─── Armor Bar ───────────────────────────────────────────────────────────────

@Composable
fun ArmorBar(
    current: Int,
    max: Int,
    modifier: Modifier = Modifier
) {
    if (max == 0) return  // hide armor bar when max is 0 (not configured)

    val fraction = (current.toFloat() / max).coerceIn(0f, 1f)
    val barColor = Color(0xFF5B8DD9)  // steel blue for armor
    val animatedFraction by animateFloatAsState(targetValue = fraction, label = "armor")

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🛡 Броня", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Text(
                "$current / $max",
                style = MaterialTheme.typography.labelLarge,
                color = barColor, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        ProgressBar(animatedFraction, barColor, height = 10.dp)
    }
}

// ─── Stat Card ───────────────────────────────────────────────────────────────

@Composable
fun StatCard(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(DarkCard, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = StatValueColor, fontWeight = FontWeight.Bold
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = StatColor, maxLines = 1)
    }
}

// ─── Resource Bar ─────────────────────────────────────────────────────────────

@Composable
fun ResourceBar(
    label: String,
    current: Float,
    max: Float = 4f,
    color: Color,
    modifier: Modifier = Modifier
) {
    val safeMax = if (max <= 0f) 4f else max
    val fraction = (current / safeMax).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(targetValue = fraction, label = "resource_$label")

    val displayCurrent = if (current == current.toInt().toFloat()) current.toInt().toString()
                         else String.format("%.1f", current)
    val displayMax = if (safeMax == safeMax.toInt().toFloat()) safeMax.toInt().toString()
                     else String.format("%.1f", safeMax)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Text(
                "$displayCurrent / $displayMax",
                style = MaterialTheme.typography.labelLarge,
                color = color, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(3.dp))
        ProgressBar(animatedFraction, color, height = 8.dp)
    }
}

// ─── Inspiration Dots ────────────────────────────────────────────────────────

@Composable
fun InspirationDots(
    current: Int,
    max: Int = 6,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(max) { index ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        color = if (index < current) InspirationColor else DarkSurfaceVariant,
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}

// ─── Shared progress bar ─────────────────────────────────────────────────────

@Composable
private fun ProgressBar(fraction: Float, color: Color, height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(DarkSurfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .clip(RoundedCornerShape(height / 2))
                .background(color)
        )
    }
}
