package com.sergiodev.bingo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sergiodev.bingo.data.local.converter.IntListConverter
import com.sergiodev.bingo.data.local.dao.BoardDao
import com.sergiodev.bingo.data.local.entity.BoardEntity

@Database(
    entities = [BoardEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(IntListConverter::class)
abstract class BingoDatabase : RoomDatabase() {
    abstract fun boardDao(): BoardDao
}
