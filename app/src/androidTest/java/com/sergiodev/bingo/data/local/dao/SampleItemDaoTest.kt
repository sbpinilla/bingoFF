package com.sergiodev.bingo.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sergiodev.bingo.data.local.BingoDatabase
import com.sergiodev.bingo.data.local.entity.SampleItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test verifying the Room insert-then-observe round trip.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class SampleItemDaoTest {

    private lateinit var database: BingoDatabase
    private lateinit var dao: SampleItemDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, BingoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.sampleItemDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertThenObserve_returnsInsertedItem() = runTest {
        dao.insert(SampleItemEntity(label = "Test item"))

        val items = dao.observeAll().first()

        assertTrue(items.any { it.label == "Test item" })
        assertEquals(1, items.size)
    }
}
