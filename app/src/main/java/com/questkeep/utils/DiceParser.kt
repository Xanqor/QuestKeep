package com.questkeep.utils

import kotlin.random.Random

data class DiceRollResult(
    val expression: String,
    val rolls: List<Int>,
    val modifier: Int,
    val total: Int
)

data class DndRollResult(
    val sides: Int,
    val count: Int,
    val modifier: Int,
    val rolls: List<Int>,
    val total: Int,
    val isNat20: Boolean = false,
    val isNat1: Boolean = false
)

data class NexurDie(
    val face: Int,      // 1..6
    val successes: Int  // 1..3 -> 0, 4..5 -> 1, 6 -> 2
)

data class NexurRollResult(
    val poolSize: Int,
    val dice: List<NexurDie>,
    val totalSuccesses: Int,
    val triumphsCount: Int
)

object DiceParser {

    val DND_STANDARD_DICE = listOf(2, 4, 6, 8, 10, 12, 20, 100)
    val STANDARD_DICE = DND_STANDARD_DICE

    // Supported: "d20", "2d6", "d8+3", "3d10-2", "d100"
    private val DICE_REGEX = Regex("""^(\d+)?d(\d+)([+-]\d+)?$""", RegexOption.IGNORE_CASE)

    fun parse(expression: String): DiceRollResult? {
        val trimmed = expression.trim().lowercase().replace(" ", "")
        val match = DICE_REGEX.matchEntire(trimmed) ?: return null

        val count = match.groupValues[1].toIntOrNull() ?: 1
        val sides = match.groupValues[2].toIntOrNull() ?: return null
        val modStr = match.groupValues[3]
        val modifier = if (modStr.isNotEmpty()) modStr.toIntOrNull() ?: 0 else 0

        if (count < 1 || count > 100 || sides < 2) return null

        val rolls = List(count) { Random.nextInt(1, sides + 1) }
        val total = rolls.sum() + modifier

        return DiceRollResult(
            expression = trimmed,
            rolls = rolls,
            modifier = modifier,
            total = total
        )
    }

    fun rollDnd(sides: Int, count: Int, modifier: Int): DndRollResult {
        val safeCount = count.coerceIn(1, 100)
        val safeSides = if (sides < 2) 2 else sides
        val rolls = List(safeCount) { Random.nextInt(1, safeSides + 1) }
        val total = rolls.sum() + modifier
        // A natural 1/20 is meaningful for a single d20 check. Treating any
        // face in a multi-d20 roll as a critical result produces misleading
        // announcements for advantage-like or damage rolls.
        val isSingleD20Check = safeSides == 20 && safeCount == 1
        return DndRollResult(
            sides = safeSides,
            count = safeCount,
            modifier = modifier,
            rolls = rolls,
            total = total,
            isNat20 = isSingleD20Check && rolls.single() == 20,
            isNat1 = isSingleD20Check && rolls.single() == 1
        )
    }

    /**
     * Nexur D6 pool mechanic:
     * - Faces 1, 2, 3 = 0 successes
     * - Faces 4, 5 = 1 success
     * - Face 6 = 2 successes (Triumph)
     */
    fun rollNexur(poolSize: Int): NexurRollResult {
        val safePool = poolSize.coerceIn(1, 50)
        val dice = List(safePool) {
            val face = Random.nextInt(1, 7)
            val successes = when (face) {
                6 -> 2
                4, 5 -> 1
                else -> 0
            }
            NexurDie(face = face, successes = successes)
        }
        val totalSuccesses = dice.sumOf { it.successes }
        val triumphs = dice.count { it.face == 6 }
        return NexurRollResult(
            poolSize = safePool,
            dice = dice,
            totalSuccesses = totalSuccesses,
            triumphsCount = triumphs
        )
    }

    fun formatResult(result: DiceRollResult): String {
        val rollStr = if (result.rolls.size == 1) {
            "${result.rolls[0]}"
        } else {
            result.rolls.joinToString(" + ") + " = ${result.rolls.sum()}"
        }
        return if (result.modifier != 0) {
            val sign = if (result.modifier > 0) "+" else ""
            "$rollStr $sign${result.modifier} = ${result.total}"
        } else {
            if (result.rolls.size == 1) rollStr else "$rollStr → ${result.total}"
        }
    }
}
