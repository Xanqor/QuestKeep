package com.questkeep.utils

import kotlin.random.Random

data class DiceRollResult(
    val expression: String,
    val rolls: List<Int>,
    val modifier: Int,
    val total: Int
)

object DiceParser {

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

    val STANDARD_DICE = listOf(4, 6, 8, 10, 12, 20, 100)
}
