package com.sergiodev.bingo.data.json

import com.sergiodev.bingo.domain.model.BoardCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BoardJsonCodecTest {

    private val board = BoardCard(
        id = 7,
        identifier = "Casa1",
        numbers = listOf(
            3, 7, 12, 14, 15,
            16, 17, 18, 19, 20,
            31, 32, 34, 35,
            46, 47, 48, 49, 50,
            61, 62, 63, 64, 65,
        ),
    )

    @Test
    fun encodeThenDecode_roundTripsIdIdentifierAndNumbers() {
        val json = BoardJsonCodec.encode(listOf(board))

        val decoded = BoardJsonCodec.decode(json)

        assertEquals(listOf(board), decoded)
    }

    @Test
    fun encode_emptyList_producesValidEmptyArray() {
        val json = BoardJsonCodec.encode(emptyList())

        assertEquals(emptyList<BoardCard>(), BoardJsonCodec.decode(json))
    }

    @Test
    fun decode_malformedJson_throws() {
        assertThrows(Exception::class.java) {
            BoardJsonCodec.decode("not valid json {{{")
        }
    }

    @Test
    fun decode_missingRequiredField_throws() {
        // "identifier" is missing entirely; Gson bypasses Kotlin's non-null
        // constructor validation via reflection, so this surfaces as an NPE
        // rather than a JsonSyntaxException — decode() must still throw.
        val json = """[{"id": 1, "numbers": [1,2,3]}]"""

        assertThrows(Exception::class.java) {
            BoardJsonCodec.decode(json)
        }
    }
}
