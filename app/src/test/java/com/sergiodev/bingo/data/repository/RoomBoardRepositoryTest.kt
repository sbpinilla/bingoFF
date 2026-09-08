package com.sergiodev.bingo.data.repository

import com.sergiodev.bingo.data.local.dao.BoardDao
import com.sergiodev.bingo.data.local.entity.BoardEntity
import com.sergiodev.bingo.domain.model.BoardCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomBoardRepositoryTest {

    private class FakeBoardDao(
        seedBoards: List<BoardEntity> = emptyList(),
    ) : BoardDao {
        private val boards = MutableStateFlow(seedBoards)
        val inserted = mutableListOf<BoardEntity>()

        override fun observeAll(): Flow<List<BoardEntity>> = boards

        override suspend fun insert(board: BoardEntity): Long {
            inserted += board
            boards.value = boards.value + board
            return board.id
        }

        override suspend fun getAllIds(): List<Long> = boards.value.map { it.id }

        override suspend fun getAllIdentifiers(): List<String> = boards.value.map { it.identifier }

        override suspend fun insertAll(boards: List<BoardEntity>) {
            inserted += boards
            this.boards.value = this.boards.value + boards
        }
    }

    private fun board(id: Long, identifier: String) =
        BoardCard(id = id, identifier = identifier, numbers = List(24) { it + 1 })

    @Test
    fun importBoards_allNew_importsEveryEntry() = runTest {
        val dao = FakeBoardDao()
        val repository = RoomBoardRepository(dao)

        val result = repository.importBoards(listOf(board(1, "Casa1"), board(2, "Casa2")))

        assertEquals(2, result.imported)
        assertEquals(0, result.skipped)
        assertEquals(2, dao.inserted.size)
    }

    @Test
    fun importBoards_existingId_isSkipped() = runTest {
        val dao = FakeBoardDao(seedBoards = listOf(BoardEntity(id = 1, identifier = "Casa1", numbers = List(24) { it })))
        val repository = RoomBoardRepository(dao)

        val result = repository.importBoards(listOf(board(1, "Casa1Renamed"), board(2, "Casa2")))

        assertEquals(1, result.imported)
        assertEquals(1, result.skipped)
        assertTrue(dao.inserted.none { it.id == 1L })
    }

    @Test
    fun importBoards_existingIdentifierUnderDifferentId_isSkipped() = runTest {
        val dao = FakeBoardDao(seedBoards = listOf(BoardEntity(id = 1, identifier = "Casa1", numbers = List(24) { it })))
        val repository = RoomBoardRepository(dao)

        val result = repository.importBoards(listOf(board(99, "Casa1"), board(2, "Casa2")))

        assertEquals(1, result.imported)
        assertEquals(1, result.skipped)
        assertTrue(dao.inserted.none { it.identifier == "Casa1" })
    }

    @Test
    fun importBoards_inBatchDuplicateId_secondEntrySkipped() = runTest {
        val dao = FakeBoardDao()
        val repository = RoomBoardRepository(dao)

        val result = repository.importBoards(listOf(board(1, "Casa1"), board(1, "Casa2")))

        assertEquals(1, result.imported)
        assertEquals(1, result.skipped)
        assertEquals("Casa1", dao.inserted.single().identifier)
    }
}
