package io.legado.shared.book

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookGroup
import io.legado.shared.storage.SharedLibraryStore

class BookGroupRepository(
    private val libraryStore: SharedLibraryStore
) {
    fun list(): List<SharedBookGroup> {
        val snapshot = libraryStore.loadDataSnapshot()
        val groups = snapshot.bookGroups.ifEmpty {
            libraryStore.saveDataSnapshot(snapshot.copy(bookGroups = DEFAULT_GROUPS))
            DEFAULT_GROUPS
        }
        return groups.sortedForDisplay()
    }

    fun listSelectable(): List<SharedBookGroup> {
        return list().filter { it.groupId > 0L && it.show }
    }

    fun upsert(group: SharedBookGroup): SharedBookGroup {
        val groups = list().toMutableList()
        val existingIndex = groups.indexOfFirst { it.groupId == group.groupId && group.groupId != 0L }
        val saved = group.normalizedForSave(groups, existingIndex >= 0)
        if (existingIndex >= 0) {
            groups[existingIndex] = saved
        } else {
            groups.add(saved)
        }
        saveGroups(groups)
        return saved
    }

    fun setVisible(groupId: Long, show: Boolean): SharedBookGroup? {
        val groups = list().toMutableList()
        val index = groups.indexOfFirst { it.groupId == groupId }
        if (index < 0) {
            return null
        }
        val updated = groups[index].copy(show = show)
        groups[index] = updated
        saveGroups(groups)
        return updated
    }

    fun delete(groupId: Long): List<SharedBookGroup> {
        if (groupId <= 0L) {
            return list()
        }
        val groups = list().filterNot { it.groupId == groupId }
        saveGroups(groups)
        libraryStore.saveBooks(
            libraryStore.loadBooks().map { book ->
                book.copy(group = book.group and groupId.inv())
            }
        )
        return list()
    }

    fun booksForGroup(groupId: Long): List<SharedBook> {
        val books = libraryStore.loadBooks().sortedWith(compareBy<SharedBook> { it.order }.thenBy { it.name })
        return when (groupId) {
            ID_ALL, ID_ROOT -> books
            else -> if (groupId > 0L) {
                books.filter { it.group and groupId > 0L }
            } else {
                books
            }
        }
    }

    fun setBookGroupEnabled(
        book: SharedBook,
        groupId: Long,
        enabled: Boolean
    ): SharedBook {
        if (groupId <= 0L) {
            return book
        }
        val updatedGroup = if (enabled) {
            book.group or groupId
        } else {
            book.group and groupId.inv()
        }
        return saveBook(book.copy(group = updatedGroup))
    }

    fun setBookGroupMask(
        book: SharedBook,
        groupMask: Long
    ): SharedBook {
        return saveBook(book.copy(group = groupMask.coerceAtLeast(0L)))
    }

    private fun saveBook(book: SharedBook): SharedBook {
        val books = libraryStore.loadBooks().toMutableList()
        val index = books.indexOfFirst { it.sameBookAs(book) }
        if (index >= 0) {
            books[index] = book
        } else {
            books.add(book)
        }
        libraryStore.saveBooks(books.sortedWith(compareBy<SharedBook> { it.order }.thenBy { it.name }))
        return book
    }

    private fun saveGroups(groups: List<SharedBookGroup>) {
        val snapshot = libraryStore.loadDataSnapshot()
        libraryStore.saveDataSnapshot(snapshot.copy(bookGroups = groups.sortedForDisplay()))
    }

    private fun SharedBookGroup.normalizedForSave(
        existing: List<SharedBookGroup>,
        isUpdate: Boolean
    ): SharedBookGroup {
        val savedId = groupId.takeUnless { it == 0L } ?: nextUnusedId(existing)
        val savedOrder = if (!isUpdate && order == 0) {
            nextOrder(existing)
        } else {
            order
        }
        return copy(
            groupId = savedId,
            groupName = groupName.ifBlank { "Group $savedId" },
            order = savedOrder
        )
    }

    private fun nextUnusedId(groups: List<SharedBookGroup>): Long {
        var id = 1L
        val used = groups.filter { it.groupId > 0L }.fold(0L) { acc, group -> acc or group.groupId }
        while (id and used != 0L) {
            id = id shl 1
        }
        return id
    }

    private fun nextOrder(groups: List<SharedBookGroup>): Int {
        return (groups.maxOfOrNull { it.order } ?: -1) + 1
    }

    private fun List<SharedBookGroup>.sortedForDisplay(): List<SharedBookGroup> {
        return sortedWith(compareBy<SharedBookGroup> { it.order }.thenBy { it.groupId }.thenBy { it.groupName })
    }

    private fun SharedBook.sameBookAs(other: SharedBook): Boolean {
        if (origin.isNotBlank() || other.origin.isNotBlank()) {
            return origin == other.origin && bookUrl == other.bookUrl
        }
        return bookUrl == other.bookUrl && name == other.name && author == other.author
    }

    companion object {
        const val ID_ROOT = -100L
        const val ID_ALL = -1L
        const val ID_LOCAL = -2L
        const val ID_AUDIO = -3L
        const val ID_NET_NONE = -4L
        const val ID_LOCAL_NONE = -5L
        const val ID_VIDEO = -6L
        const val ID_ERROR = -11L

        val DEFAULT_GROUPS: List<SharedBookGroup> = listOf(
            SharedBookGroup(groupId = ID_ALL, groupName = "All", order = -10, show = true),
            SharedBookGroup(groupId = ID_LOCAL, groupName = "Local", order = -9, enableRefresh = false, show = true),
            SharedBookGroup(groupId = ID_AUDIO, groupName = "Audio", order = -8, show = true),
            SharedBookGroup(groupId = ID_NET_NONE, groupName = "Network ungrouped", order = -7, show = true),
            SharedBookGroup(groupId = ID_LOCAL_NONE, groupName = "Local ungrouped", order = -6, enableRefresh = false, show = true),
            SharedBookGroup(groupId = ID_VIDEO, groupName = "Video", order = -5, show = true),
            SharedBookGroup(groupId = ID_ERROR, groupName = "Update failed", order = -1, show = true)
        )
    }
}
