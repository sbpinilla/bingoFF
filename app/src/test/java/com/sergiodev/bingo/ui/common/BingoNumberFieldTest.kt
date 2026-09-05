package com.sergiodev.bingo.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class BingoNumberFieldTest(
    private val raw: String,
    private val expected: String,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "sanitizeNumberInput({0}) == {1}")
        fun data(): List<Array<Any?>> = listOf(
            arrayOf("", ""),
            arrayOf("abc", ""),
            arrayOf("1a2", "12"),
            arrayOf("123", "12"),
            arrayOf("07", "07"),
            arrayOf("-3", "3"),
            arrayOf(" 7 ", "7"),
            arrayOf("5.5", "55"),
        )
    }

    @Test
    fun sanitizeNumberInput_returnsExpectedResult() {
        assertEquals(expected, sanitizeNumberInput(raw))
    }
}

class BingoNumberFieldDirectTest {
    @Test
    fun sanitizeNumberInput_pastedMultiCharacterStringIsFilteredAndTruncated() {
        assertEquals("12", sanitizeNumberInput("a1b2c3d4"))
    }

    @Test
    fun sanitizeNumberInput_isIdempotent() {
        val once = sanitizeNumberInput("1a2b3")
        assertEquals(once, sanitizeNumberInput(once))
    }
}
