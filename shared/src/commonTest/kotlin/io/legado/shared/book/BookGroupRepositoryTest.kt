package io.legado.shared.book

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookGroup
import io.legado.shared.model.SharedDataSnapshot
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BookGroupRepositoryTest {
    @Test
    fun createsAndroidCompatibleDefaultGroupsWhenSnapshotHasNone() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val repository = BookGroupRepository(store)

        val groups = repository.list()

        assertEquals(listOf(-1L, -2L, -3L, -4L, -5L, -6L, -11L), groups.map { it.groupId })
        assertEquals("All", groups.first().groupName)
        assertEquals(groups, store.loadDataSnapshot().bookGroups)
    }

    @Test
    fun addsUserGroupWithUnusedBitAndNextOrder() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        store.saveDataSnapshot(
            SharedDataSnapshot(
                bookGroups = listOf(
                    SharedBookGroup(groupId = 1L, groupName = "Read", order = 1),
                    SharedBookGroup(groupId = 4L, groupName = "Later", order = 2)
                )
            )
        )
        val repository = BookGroupRepository(store)

        val saved = repository.upsert(SharedBookGroup(groupId = 0L, groupName = "Sci-Fi", bookSort = 3))

        assertEquals(2L, saved.groupId)
        assertEquals(3, saved.order)
        assertEquals(listOf(1L, 4L, 2L), repository.list().map { it.groupId })
        assertEquals(3, repository.listSelectable().size)
    }

    @Test
    fun filtersBooksByAllOrCustomGroupMask() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val books = listOf(
            SharedBook(name = "A", bookUrl = "a", group = 1L, order = 1),
            SharedBook(name = "B", bookUrl = "b", group = 2L, order = 2),
            SharedBook(name = "C", bookUrl = "c", group = 3L, order = 3)
        )
        store.saveBooks(books)
        val repository = BookGroupRepository(store)

        assertEquals(listOf("A", "B", "C"), repository.booksForGroup(BookGroupRepository.ID_ALL).map { it.name })
        assertEquals(listOf("A", "C"), repository.booksForGroup(1L).map { it.name })
        assertEquals(listOf("B", "C"), repository.booksForGroup(2L).map { it.name })
    }

    @Test
    fun deletesUserGroupAndRemovesItsBitFromBooks() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        store.saveDataSnapshot(
            SharedDataSnapshot(
                bookGroups = listOf(
                    SharedBookGroup(groupId = 1L, groupName = "Read", order = 1),
                    SharedBookGroup(groupId = 2L, groupName = "Later", order = 2)
                )
            )
        )
        store.saveBooks(
            listOf(
                SharedBook(name = "A", bookUrl = "a", group = 3L),
                SharedBook(name = "B", bookUrl = "b", group = 2L)
            )
        )
        val repository = BookGroupRepository(store)

        val groups = repository.delete(2L)

        assertFalse(groups.any { it.groupId == 2L })
        assertEquals(listOf(1L, 0L), store.loadBooks().map { it.group })
    }

    @Test
    fun togglesBookMembershipByGroupBit() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val book = SharedBook(name = "A", bookUrl = "a", group = 1L)
        store.saveBooks(listOf(book))
        val repository = BookGroupRepository(store)

        val added = repository.setBookGroupEnabled(book, groupId = 2L, enabled = true)
        val removed = repository.setBookGroupEnabled(added, groupId = 1L, enabled = false)

        assertEquals(3L, added.group)
        assertEquals(2L, removed.group)
        assertEquals(2L, store.loadBooks().single().group)
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }

        override fun removeText(key: String) {
            values.remove(key)
        }
    }
}
