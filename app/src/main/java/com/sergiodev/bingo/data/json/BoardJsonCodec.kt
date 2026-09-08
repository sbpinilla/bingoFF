package com.sergiodev.bingo.data.json

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sergiodev.bingo.domain.model.BoardCard

/**
 * JSON representation of a board, mirroring [BoardCard]'s property names
 * exactly so Gson's default reflection needs no `@SerializedName`.
 */
data class BoardExportDto(
    val id: Long,
    val identifier: String,
    val numbers: List<Int>,
)

/**
 * Encodes/decodes boards as a bare top-level JSON array (no wrapper object,
 * no version field — versioning is explicitly out of scope, see design).
 */
object BoardJsonCodec {

    private val gson = Gson()
    private val listType = object : TypeToken<List<BoardExportDto>>() {}.type

    fun encode(boards: List<BoardCard>): String {
        val dtos = boards.map { BoardExportDto(id = it.id, identifier = it.identifier, numbers = it.numbers) }
        return gson.toJson(dtos, listType)
    }

    /**
     * Decodes [json] into a list of [BoardCard]. Throws on any structural or
     * type error. Gson bypasses Kotlin constructor validation via reflection,
     * so a missing/wrong-typed field can produce `null` in a non-null Kotlin
     * property instead of failing at parse time; that later surfaces as an
     * [NullPointerException] rather than a `JsonSyntaxException` — callers
     * must catch broadly (`Exception`), not just `JsonSyntaxException`.
     */
    fun decode(json: String): List<BoardCard> {
        val dtos: List<BoardExportDto> = gson.fromJson(json, listType)
        return dtos.map { BoardCard(id = it.id, identifier = it.identifier, numbers = it.numbers) }
    }
}
