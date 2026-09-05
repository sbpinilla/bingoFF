package com.sergiodev.bingo.data.local.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sergiodev.bingo.data.local.BingoDatabase
import com.sergiodev.bingo.data.local.converter.IntListConverter
import com.sergiodev.bingo.data.local.entity.BoardEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test verifying the Room insert-then-observe round trip for
 * [BoardEntity], the unique-identifier conflict, and [IntListConverter]
 * lossless round-tripping of 24 numbers.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class BoardDaoTest {

    private lateinit var database: BingoDatabase
    private lateinit var dao: BoardDao

    private val twentyFourNumbers = listOf(
        3, 7, 12, 14, 15, // B
        16, 17, 18, 19, 20, // I
        31, 32, 34, 35, // N (FREE omitted)
        46, 47, 48, 49, 50, // G
        61, 62, 63, 64, 65, // O
    )

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, BingoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.boardDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertThenObserve_returnsInsertedBoard() = runTest {
        dao.insert(BoardEntity(identifier = "Casa1", numbers = twentyFourNumbers))

        val boards = dao.observeAll().first()

        assertEquals(1, boards.size)
        assertEquals("Casa1", boards.first().identifier)
        assertEquals(twentyFourNumbers, boards.first().numbers)
    }

    @Test
    fun uniqueIdentifierConflict_throwsConstraintException() = runTest {
        dao.insert(BoardEntity(identifier = "Casa1", numbers = twentyFourNumbers))

        assertThrows(SQLiteConstraintException::class.java) {
            kotlinx.coroutines.runBlocking {
                dao.insert(BoardEntity(identifier = "Casa1", numbers = twentyFourNumbers))
            }
        }
    }

    @Test
    fun intListConverter_roundTripsTwentyFourNumbersLosslessly() {
        val converter = IntListConverter()

        val serialized = converter.fromList(twentyFourNumbers)
        val deserialized = converter.toList(serialized)

        assertEquals(twentyFourNumbers, deserialized)
        assertTrue(deserialized.size == 24)
    }
}
