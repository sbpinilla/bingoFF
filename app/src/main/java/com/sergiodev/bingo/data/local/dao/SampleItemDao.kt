package com.sergiodev.bingo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sergiodev.bingo.data.local.entity.SampleItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SampleItemDao {
    @Query("SELECT * FROM sample_item ORDER BY id ASC")
    fun observeAll(): Flow<List<SampleItemEntity>>

    @Insert
    suspend fun insert(item: SampleItemEntity)
}
