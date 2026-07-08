package io.legado.shared.book

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedReadRecord
import io.legado.shared.storage.SharedLibraryStore

class ReadRecordRepository(
    private val libraryStore: SharedLibraryStore
) {
    fun list(): List<SharedReadRecord> {
        return libraryStore.loadDataSnapshot().readRecords.sortedByDescending { it.lastRead }
    }

    fun record(
        book: SharedBook,
        durationMillis: Long,
        nowMillis: Long,
        deviceId: String
    ): List<SharedReadRecord> {
        if (durationMillis <= 0L) {
            return list()
        }
        val snapshot = libraryStore.loadDataSnapshot()
        val existing = snapshot.readRecords.firstOrNull {
            it.deviceId == deviceId && it.bookName == book.name
        }
        val updated = SharedReadRecord(
            deviceId = deviceId,
            bookName = book.name,
            readTime = (existing?.readTime ?: 0L) + durationMillis,
            lastRead = nowMillis
        )
        val records = listOf(updated) + snapshot.readRecords.filterNot {
            it.deviceId == deviceId && it.bookName == book.name
        }
        libraryStore.saveDataSnapshot(snapshot.copy(readRecords = records.sortedByDescending { it.lastRead }))
        return list()
    }
}
