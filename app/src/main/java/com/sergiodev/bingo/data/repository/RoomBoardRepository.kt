package com.sergiodev.bingo.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.sergiodev.bingo.data.local.dao.BoardDao
import com.sergiodev.bingo.data.local.entity.BoardEntity
import com.sergiodev.bingo.domain.model.BoardCard
import com.sergiodev.bingo.domain.repository.BoardRepository
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
}
