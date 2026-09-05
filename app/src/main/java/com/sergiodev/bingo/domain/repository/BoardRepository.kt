package com.sergiodev.bingo.domain.repository

import com.sergiodev.bingo.domain.model.BoardCard
import kotlinx.coroutines.flow.Flow

/**
 * Port for board persistence. Deliberately has no lookup-by-identifier
 * method — board lookup was descoped from this change; identifier
 * uniqueness is enforced by a DB unique index + insert-conflict handling.
 */
interface BoardRepository {
    fun observeBoards(): Flow<List<BoardCard>>

    /**
     * Adds a new board. Returns [Result.failure] when [identifier] already
     * exists (unique-identifier conflict).
     */
    suspend fun addBoard(identifier: String, numbers: List<Int>): Result<Unit>
}
