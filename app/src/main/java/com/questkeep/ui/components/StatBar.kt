package com.questkeep.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.questkeep.ui.theme.*

// ─── HP Card with Quick Adjust Controls ──────────────────────────────────────

@Composable
fun HpCard(
    current: Int,
    max: Int,
    onDamage: (Int) -> Unit,
    onHeal: (Int) -> Unit,
    onOpenDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val t = LocalXanteya.current
    val haptic = LocalHapticFeedback.current
    val healthColor = t.colors.hot

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = t.colors.panel),
        shape = RoundedCornerShape(t.corner.coerceAtLeast(6.dp)),
        border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Themed gauge drawn via ThemeKit (segmented for Falloran, organic for Nirventhal, astrolabe for Xanteya)
            SegmentGauge(
                label = "ЗДОРОВЬЕ",
                value = current,
                max = max,
                color = healthColor
            )

            Spacer(Modifier.height(12.dp))

            // Quick adjustment buttons (>= 48dp touch targets)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickActionButton("-5", t.colors.hot) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDamage(5)
                }
                QuickActionButton("-1", t.colors.hot) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDamage(1)
                }
                QuickActionButton("+1", t.colors.accent) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onHeal(1)
                }
                QuickActionButton("+5", t.colors.accent) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onHeal(5)
                }

                Spacer(modifier = Modifier.weight(1f))

                OutlinedButton(
                    onClick = onOpenDialog,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = t.colors.text),
                    border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line),
                    shape = RoundedCornerShape(t.corner),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("± Изменить", fontSize = 12.sp, fontFamily = t.body)
                }
            }
        }
    }
}

// ─── Armor Card with Quick Adjust Controls ────────────────────────────────────

@Composable
fun ArmorCard(
    current: Int,
    max: Int,
    onDamage: (Int) -> Unit,
    onRepair: (Int) -> Unit,
    onOpenDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (max <= 0) return

    val t = LocalXanteya.current
    val haptic = LocalHapticFeedback.current
    val armorColor = t.colors.accent

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = t.colors.panel),
        shape = RoundedCornerShape(t.corner.coerceAtLeast(6.dp)),
        border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            SegmentGauge(
                label = "БРОНЯ",
                value = current,
                max = max,
                color = armorColor
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickActionButton("-1", t.colors.hot) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDamage(1)
                }
                QuickActionButton("+1", armorColor) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onRepair(1)
                }

                Spacer(modifier = Modifier.weight(1f))

                OutlinedButton(
                    onClick = onOpenDialog,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = t.colors.text),
                    border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line),
                    shape = RoundedCornerShape(t.corner),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("± Изменить", fontSize = 12.sp, fontFamily = t.body)
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(label: String, color: Color, onClick: () -> Unit) {
    val t = LocalXanteya.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(t.corner),
        color = t.colors.panel,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.8f)),
        modifier = Modifier
            .defaultMinSize(minWidth = 46.dp, minHeight = 44.dp)
            .height(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(label, color = color, fontWeight = FontWeight.Bold, fontFamily = t.display, fontSize = 14.sp)
        }
    }
}

// ─── HP Bar (legacy compatibility) ──────────────────────────────────────────

@Composable
fun HpBar(
    current: Int,
    max: Int,
    modifier: Modifier = Modifier
) {
    val t = LocalXanteya.current
    SegmentGauge(
        label = "HP",
        value = current,
        max = max,
        color = t.colors.hot,
        modifier = modifier
    )
}

// ─── Armor Bar (legacy compatibility) ───────────────────────────────────────

@Composable
fun ArmorBar(
    current: Int,
    max: Int,
    modifier: Modifier = Modifier
) {
    if (max == 0) return
    val t = LocalXanteya.current
    SegmentGauge(
        label = "БРОНЯ",
        value = current,
        max = max,
        color = t.colors.accent,
        modifier = modifier
    )
}

// ─── Stat Card (Atmospheric Riveted / Styled StatPlate) ───────────────────────
@Composable
fun StatCard(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    StatPlate(
        label = label,
        value = value,
        modifier = modifier
    )
}

// ─── Resource Bar with Quick Controls ─────────────────────────────────────────

@Composable
fun ResourceBar(
    label: String,
    current: Float,
    max: Float = 4f,
    color: Color,
    onDecrease: (() -> Unit)? = null,
    onIncrease: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val t = LocalXanteya.current
    val safeMax = if (max <= 0f) 4f else max
    val haptic = LocalHapticFeedback.current

    val displayCurrent = if (current == current.toInt().toFloat()) current.toInt().toString()
                         else String.format("%.1f", current)
    val displayMax = if (safeMax == safeMax.toInt().toFloat()) safeMax.toInt().toString()
                     else String.format("%.1f", safeMax)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = t.colors.panel),
        shape = RoundedCornerShape(t.corner.coerceAtLeast(6.dp)),
        border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = t.colors.muted,
                    fontFamily = t.display,
                    fontWeight = FontWeight.SemiBold
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "$displayCurrent / $displayMax",
                        style = MaterialTheme.typography.labelLarge,
                        color = color,
                        fontFamily = t.display,
                        fontWeight = FontWeight.Bold
                    )

                    if (onDecrease != null && onIncrease != null) {
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDecrease()
                            },
                            shape = CircleShape,
                            color = t.colors.bg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("−", color = t.colors.hot, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onIncrease()
                            },
                            shape = CircleShape,
                            color = t.colors.bg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("+", color = t.colors.accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            SegmentGauge(
                label = "",
                value = current.toInt(),
                max = safeMax.toInt(),
                color = color
            )
        }
    }
}

// ─── Inspiration Dots (Interactive & Accessible) ─────────────────────────────

@Composable
fun InspirationDots(
    current: Int,
    max: Int = 6,
    onSelect: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val t = LocalXanteya.current
    val haptic = LocalHapticFeedback.current
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(max) { index ->
            val isFilled = index < current
            Box(
                modifier = Modifier
                    .size(40.dp) // accessible touch target >= 40-48dp
                    .clip(CircleShape)
                    .clickable(enabled = onSelect != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val newCount = if (index + 1 == current) index else index + 1
                        onSelect?.invoke(newCount)
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(
                            color = if (isFilled) t.colors.accent else t.colors.panel,
                            shape = CircleShape
                        )
                        .border(
                            1.dp,
                            if (isFilled) t.colors.accent else t.colors.line,
                            CircleShape
                        )
                )
            }
        }
    }
}
