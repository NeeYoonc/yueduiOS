package io.legado.shared.local

import io.legado.shared.book.BookshelfService
import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedChapterContent
import io.legado.shared.storage.SharedLibraryStore

class LocalDocumentBookService(
    private val libraryStore: SharedLibraryStore,
    private val bookshelfService: BookshelfService = BookshelfService(libraryStore)
) {
    fun importDocument(
        fileName: String,
        fileUrl: String,
        mimeType: String? = null,
        nowMillis: Long = 0L
    ): LocalDocumentImportResult {
        val cleanUrl = fileUrl.trim()
        require(cleanUrl.isNotBlank()) { "Document URL is empty" }
        val cleanFileName = fileName.trim()
            .ifBlank { cleanUrl.substringAfterLast('/').substringBefore('?').ifBlank { "Untitled Document" } }
        val title = cleanFileName.substringBeforeLast(".").ifBlank { cleanFileName }
        val type = detectBookType(cleanFileName, mimeType)
        val bookUrl = "local://file/${stableHash("$cleanFileName\n$cleanUrl")}"
        val chapter = SharedBookChapter(
            title = cleanFileName,
            url = "$bookUrl#document",
            index = 0,
            bookUrl = bookUrl,
            resourceUrl = cleanUrl,
            imgUrl = cleanUrl.takeIf { type hasType BOOK_TYPE_IMAGE },
            wordCount = "0"
        )
        val book = SharedBook(
            name = title,
            author = "",
            bookUrl = bookUrl,
            tocUrl = bookUrl,
            origin = LOCAL_ORIGIN,
            originName = cleanFileName,
            kind = cleanFileName.substringAfterLast('.', "").uppercase().ifBlank { mimeType },
            latestChapterTitle = cleanFileName,
            latestChapterTime = nowMillis,
            totalChapterNum = 1,
            durChapterTitle = cleanFileName,
            type = type,
            coverUrl = cleanUrl.takeIf { type hasType BOOK_TYPE_IMAGE },
            canUpdate = false,
            variableMap = buildMap {
                put("fileUrl", cleanUrl)
                if (!mimeType.isNullOrBlank()) {
                    put("mimeType", mimeType)
                }
            }
        )
        val savedBook = bookshelfService.upsertBook(book)
        libraryStore.saveBookChapters(savedBook, listOf(chapter))
        libraryStore.saveChapterContent(
            savedBook,
            chapter,
            SharedChapterContent(
                title = cleanFileName,
                content = "Local document: $cleanFileName\n$cleanUrl"
            )
        )
        return LocalDocumentImportResult(savedBook, listOf(chapter))
    }

    private fun detectBookType(fileName: String, mimeType: String?): Int {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val mime = mimeType.orEmpty().lowercase()
        val base = when {
            extension in imageExtensions || mime.startsWith("image/") -> BOOK_TYPE_IMAGE
            extension in audioExtensions || mime.startsWith("audio/") -> BOOK_TYPE_AUDIO
            extension in videoExtensions || mime.startsWith("video/") -> BOOK_TYPE_VIDEO
            extension in textLikeExtensions || mime.startsWith("text/") -> BOOK_TYPE_TEXT
            else -> BOOK_TYPE_WEB_FILE
        }
        val archive = if (extension in archiveExtensions) BOOK_TYPE_ARCHIVE else 0
        return base or BOOK_TYPE_LOCAL or archive
    }

    private infix fun Int.hasType(type: Int): Boolean {
        return (this and type) > 0
    }

    private fun stableHash(input: String): String {
        var hash = 0xcbf29ce484222325UL
        input.encodeToByteArray().forEach { byte ->
            hash = hash xor byte.toUByte().toULong()
            hash *= 0x100000001b3UL
        }
        return hash.toString(16)
    }

    companion object {
        const val LOCAL_ORIGIN = "loc_book"
        const val BOOK_TYPE_VIDEO = 0b100
        const val BOOK_TYPE_TEXT = 0b1000
        const val BOOK_TYPE_AUDIO = 0b100000
        const val BOOK_TYPE_IMAGE = 0b1000000
        const val BOOK_TYPE_WEB_FILE = 0b10000000
        const val BOOK_TYPE_LOCAL = 0b100000000
        const val BOOK_TYPE_ARCHIVE = 0b1000000000

        private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")
        private val audioExtensions = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus")
        private val videoExtensions = setOf("mp4", "m4v", "mov", "mkv", "webm", "avi")
        private val textLikeExtensions = setOf("txt", "md", "html", "htm", "xml", "json")
        private val archiveExtensions = setOf("epub", "mobi", "azw3", "umd", "zip", "cbz")
    }
}

data class LocalDocumentImportResult(
    val book: SharedBook,
    val chapters: List<SharedBookChapter>
)
