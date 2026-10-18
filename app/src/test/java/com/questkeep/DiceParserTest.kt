package com.questkeep

import com.questkeep.utils.DiceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DiceParserTest {

    @Test
    fun `parses normalized dice expression and keeps totals consistent`() {
        val result = DiceParser.parse(" 2D6 + 3 ")

        assertNotNull(result)
        result!!
        assertEquals("2d6+3", result.expression)
        assertEquals(2, result.rolls.size)
        assertEquals(3, result.modifier)
        assertEquals(result.rolls.sum() + result.modifier, result.total)
        assertFalse(result.rolls.any { it !in 1..6 })
    }

    @Test
    fun `rejects invalid or unsafe dice expressions`() {
        assertNull(DiceParser.parse(""))
        assertNull(DiceParser.parse("2d1"))
        assertNull(DiceParser.parse("101d6"))
        assertNull(DiceParser.parse("2d6+bad"))
    }

    @Test
    fun `dnd multiroll never reports a natural critical`() {
        repeat(20) {
            val result = DiceParser.rollDnd(sides = 20, count = 2, modifier = 0)
            assertFalse(result.isNat20)
            assertFalse(result.isNat1)
        }
    }

    @Test
    fun `nexur roll stays inside configured bounds`() {
        val result = DiceParser.rollNexur(poolSize = 100)

        assertEquals(50, result.poolSize)
        assertEquals(50, result.dice.size)
        assertEquals(result.dice.sumOf { it.successes }, result.totalSuccesses)
    }
}
