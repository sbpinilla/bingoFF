package com.sergiodev.bingo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sergiodev.bingo.data.local.dao.SampleItemDao
import com.sergiodev.bingo.data.local.entity.SampleItemEntity

@Database(entities = [SampleItemEntity::class], version = 1, exportSchema = true)
abstract class BingoDatabase : RoomDatabase() {
    abstract fun sampleItemDao(): SampleItemDao
}
