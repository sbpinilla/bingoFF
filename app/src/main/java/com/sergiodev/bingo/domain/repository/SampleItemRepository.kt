package com.sergiodev.bingo.domain.repository

import com.sergiodev.bingo.domain.model.SampleItem
import kotlinx.coroutines.flow.Flow

interface SampleItemRepository {
    fun observeItems(): Flow<List<SampleItem>>

    suspend fun add(label: String)
}
