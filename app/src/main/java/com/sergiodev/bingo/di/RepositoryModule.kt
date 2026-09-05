package com.sergiodev.bingo.di

import com.sergiodev.bingo.data.repository.RoomSampleItemRepository
import com.sergiodev.bingo.domain.repository.SampleItemRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindSampleItemRepository(impl: RoomSampleItemRepository): SampleItemRepository
}
