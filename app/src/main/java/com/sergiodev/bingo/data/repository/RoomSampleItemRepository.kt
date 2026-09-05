package com.sergiodev.bingo.data.repository

import com.sergiodev.bingo.data.local.dao.SampleItemDao
import com.sergiodev.bingo.data.local.entity.SampleItemEntity
import com.sergiodev.bingo.domain.model.SampleItem
import com.sergiodev.bingo.domain.repository.SampleItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomSampleItemRepository @Inject constructor(
    private val dao: SampleItemDao,
) : SampleItemRepository {

    override fun observeItems(): Flow<List<SampleItem>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun add(label: String) {
        dao.insert(SampleItemEntity(label = label))
    }
}
