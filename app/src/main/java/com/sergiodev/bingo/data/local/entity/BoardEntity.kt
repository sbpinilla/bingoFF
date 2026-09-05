package com.sergiodev.bingo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.sergiodev.bingo.data.local.converter.IntListConverter
import com.sergiodev.bingo.domain.model.BoardCard

/**
 * [id] doubles as the app-assigned sequential number shown to the operator
 * (Room AUTOINCREMENT, so numbers never repeat). [numbers] holds exactly 24
 * ints in fixed column-major order: B1-B5, I1-I5, N1,N2,N4,N5 (FREE at N
 * row 3 omitted, never a sentinel 0), G1-G5, O1-O5.
 */
@Entity(
    tableName = "board",
    indices = [Index(value = ["identifier"], unique = true)],
)
@TypeConverters(IntListConverter::class)
data class BoardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val identifier: String,
    val numbers: List<Int>,
) {
    fun toDomain(): BoardCard = BoardCard(id = id, identifier = identifier, numbers = numbers)
}
