package io.legado.shared.book

import io.legado.shared.model.SharedBook
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadRecordRepositoryTest {
    @Test
    fun recordsAndAccumulatesReadTimePerDeviceAndBook() {
        val repository = ReadRecordRepository(SharedLibraryStore(InMemoryCacheStore()))
        val book = SharedBook(name = "Book A", bookUrl = "https://book.test/a")

        repository.record(book, durationMillis = 3_000L, nowMillis = 10L, deviceId = "ios")
        repository.record(book, durationMillis = 2_000L, nowMillis = 20L, deviceId = "ios")
        repository.record(book, durationMillis = 7_000L, nowMillis = 30L, deviceId = "ipad")

        val records = repository.list()

        assertEquals(listOf("ipad", "ios"), records.map { it.deviceId })
        assertEquals(listOf(7_000L, 5_000L), records.map { it.readTime })
        assertEquals(20L, records.first { it.deviceId == "ios" }.lastRead)
    }

    @Test
    fun ignoresNonPositiveDurations() {
        val repository = ReadRecordRepository(SharedLibraryStore(InMemoryCacheStore()))

        repository.record(
            SharedBook(name = "Book B", bookUrl = "https://book.test/b"),
            durationMillis = 0L,
            nowMillis = 10L,
            deviceId = "ios"
        )

        assertEquals(emptyList(), repository.list())
    }

    @Test
    fun deletesAndClearsReadRecords() {
        val repository = ReadRecordRepository(SharedLibraryStore(InMemoryCacheStore()))
        val first = SharedBook(name = "Book A", bookUrl = "https://book.test/a")
        val second = SharedBook(name = "Book B", bookUrl = "https://book.test/b")
        repository.record(first, durationMillis = 1_000L, nowMillis = 10L, deviceId = "ios")
        repository.record(second, durationMillis = 2_000L, nowMillis = 20L, deviceId = "ios")

        val afterDelete = repository.delete(deviceId = "ios", bookName = "Book A")

        assertEquals(listOf("Book B"), afterDelete.map { it.bookName })
        assertEquals(emptyList(), repository.clear())
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}
