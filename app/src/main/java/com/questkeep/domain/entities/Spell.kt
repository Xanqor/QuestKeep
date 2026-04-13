package com.questkeep.domain.entities

import java.util.UUID

/**
 * Represents a saved spell created via the Xantheia magic calculator.
 *
 * Resource mapping (from the calculator):
 *   +п → quarCost   (Куар)
 *   -п → ombraCost  (Омбра)
 *   +э → verunaCost (Веруна)
 *   -э → sumeniCost (Сумени)
 */
data class Spell(
    val id: String = UUID.randomUUID().toString(),
    val characterId: String = "",
    val name: String = "",

    // ── Xantheia calculator recipe (for display purposes) ──────────────────
    val coreId: String = "",
    val coreName: String = "",
    val modId: String = "none",
    val modName: String = "",
    val expressionId: String = "",
    val expressionName: String = "",
    /** Serialised accent selections, format: "power=0;range=1;size=2" */
    val accentsJson: String = "",
    val powerLevel: Int = 1,
    val effectText: String = "",

    // ── Calculated resource costs (positive values) ─────────────────────────
    val quarCost: Float = 0f,    // +п  → spend Куар
    val ombraCost: Float = 0f,   // -п  → spend Омбра
    val verunaCost: Float = 0f,  // +э  → spend Веруна
    val sumeniCost: Float = 0f,  // -э  → spend Сумени

    val createdAt: Long = System.currentTimeMillis()
) {
    /** Short cost summary for list display, e.g. "+п 2 -э 1.5" */
    val costSummary: String
        get() {
            val parts = mutableListOf<String>()
            if (quarCost > 0f)   parts += "+п ${fmt(quarCost)}"
            if (ombraCost > 0f)  parts += "-п ${fmt(ombraCost)}"
            if (verunaCost > 0f) parts += "+э ${fmt(verunaCost)}"
            if (sumeniCost > 0f) parts += "-э ${fmt(sumeniCost)}"
            return if (parts.isEmpty()) "Бесплатно" else parts.joinToString(" ")
        }

    private fun fmt(v: Float) =
        if (v == v.toInt().toFloat()) v.toInt().toString() else String.format("%.1f", v)
}
