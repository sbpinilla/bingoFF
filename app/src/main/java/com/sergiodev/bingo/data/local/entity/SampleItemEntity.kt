package com.sergiodev.bingo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sergiodev.bingo.domain.model.SampleItem

@Entity(tableName = "sample_item")
data class SampleItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
) {
    fun toDomain(): SampleItem = SampleItem(id = id, label = label)
}
