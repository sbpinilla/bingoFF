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
}
