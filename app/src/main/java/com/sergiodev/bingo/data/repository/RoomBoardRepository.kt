package com.sergiodev.bingo.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.sergiodev.bingo.data.local.dao.BoardDao
import com.sergiodev.bingo.data.local.entity.BoardEntity
import com.sergiodev.bingo.domain.model.BoardCard
import com.sergiodev.bingo.domain.repository.BoardRepository
import com.sergiodev.bingo.domain.repository.ImportResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomBoardRepository @Inject constructor(
    private val dao: BoardDao,
) : BoardRepository {

    override fun observeBoards(): Flow<List<BoardCard>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun addBoard(identifier: String, numbers: List<Int>): Result<Unit> =
        try {
            dao.insert(BoardEntity(identifier = identifier, numbers = numbers))
            Result.success(Unit)
        } catch (e: SQLiteConstraintException) {
            Result.failure(e)
        }

    /**
     * Prefetches existing ids/identifiers in 2 queries (no N+1), then dedups
     * both against the DB and within [boards] itself — a payload containing
     * two entries with the same id or identifier only keeps the first.
     */
    override suspend fun importBoards(boards: List<BoardCard>): ImportResult {
        val existingIds = dao.getAllIds().toMutableSet()
        val existingIdentifiers = dao.getAllIdentifiers().toMutableSet()

        val toInsert = mutableListOf<BoardEntity>()
        var skipped = 0

        boards.forEach { board ->
            if (board.id in existingIds || board.identifier in existingIdentifiers) {
                skipped++
            } else {
                existingIds += board.id
                existingIdentifiers += board.identifier
                toInsert += BoardEntity(id = board.id, identifier = board.identifier, numbers = board.numbers)
            }
        }

        if (toInsert.isNotEmpty()) {
            dao.insertAll(toInsert)
        }

        return ImportResult(imported = toInsert.size, skipped = skipped)
    }
}
