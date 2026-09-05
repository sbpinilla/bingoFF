package com.sergiodev.bingo.di

import android.content.Context
import androidx.room.Room
import com.sergiodev.bingo.data.local.BingoDatabase
import com.sergiodev.bingo.data.local.dao.SampleItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideBingoDatabase(@ApplicationContext context: Context): BingoDatabase =
        Room.databaseBuilder(context, BingoDatabase::class.java, "bingo.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideSampleItemDao(database: BingoDatabase): SampleItemDao = database.sampleItemDao()
}
