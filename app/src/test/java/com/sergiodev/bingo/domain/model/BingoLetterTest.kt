package com.sergiodev.bingo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class BingoLetterTest(
    private val number: Int,
    private val expected: BingoLetter?,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "fromNumber({0}) == {1}")
        fun data(): List<Array<Any?>> = listOf(
            arrayOf(0, null),
            arrayOf(1, BingoLetter.B),
            arrayOf(15, BingoLetter.B),
            arrayOf(16, BingoLetter.I),
            arrayOf(30, BingoLetter.I),
            arrayOf(31, BingoLetter.N),
            arrayOf(45, BingoLetter.N),
            arrayOf(46, BingoLetter.G),
            arrayOf(60, BingoLetter.G),
            arrayOf(61, BingoLetter.O),
            arrayOf(75, BingoLetter.O),
            arrayOf(76, null),
        )
    }

    @Test
    fun fromNumber_returnsExpectedLetter() {
        assertEquals(expected, BingoLetter.fromNumber(number))
    }
}

class BingoLetterDirectTest {
    @Test
    fun fromNumber_negativeIsNull() {
        assertNull(BingoLetter.fromNumber(-1))
    }
}
