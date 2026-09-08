package com.sergiodev.bingo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sergiodev.bingo.data.local.entity.BoardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BoardDao {
    @Query("SELECT * FROM board ORDER BY id ASC")
    fun observeAll(): Flow<List<BoardEntity>>

    /**
     * [OnConflictStrategy.ABORT] (the Room default) surfaces the
     * unique-identifier violation as a thrown `SQLiteConstraintException`
     * rather than silently ignoring or replacing the row.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(board: BoardEntity): Long

    @Query("SELECT id FROM board")
    suspend fun getAllIds(): List<Long>

    @Query("SELECT identifier FROM board")
    suspend fun getAllIdentifiers(): List<String>

    /**
     * Bulk insert used by import. Entities may carry an explicit non-zero
     * [BoardEntity.id]: Room only omits the PK column from the generated
     * INSERT when the value is exactly `0`, so a non-zero id is inserted
     * literally and [OnConflictStrategy.ABORT] still throws on a real
     * collision instead of silently overwriting.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(boards: List<BoardEntity>)
}
