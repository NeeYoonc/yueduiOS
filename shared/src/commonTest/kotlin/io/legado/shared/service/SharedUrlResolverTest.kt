package io.legado.shared.service

import kotlin.test.Test
import kotlin.test.assertEquals

class SharedUrlResolverTest {
    @Test
    fun resolvesAbsoluteAndRelativeHttpUrls() {
        val base = "https://source.test/book/1/detail/index.html"

        assertEquals(
            "https://other.test/a.jpg",
            SharedUrlResolver.resolve(base, "https://other.test/a.jpg")
        )
        assertEquals(
            "https://cdn.test/a.jpg",
            SharedUrlResolver.resolve(base, "//cdn.test/a.jpg")
        )
        assertEquals(
            "https://source.test/catalog",
            SharedUrlResolver.resolve(base, "/catalog")
        )
        assertEquals(
            "https://source.test/book/1/detail/chapter/1.html",
            SharedUrlResolver.resolve(base, "chapter/1.html")
        )
        assertEquals(
            "https://source.test/book/1/cover.jpg",
            SharedUrlResolver.resolve(base, "../cover.jpg")
        )
        assertEquals(
            "https://source.test/book/1/detail/index.html?page=2",
            SharedUrlResolver.resolve(base, "?page=2")
        )
        assertEquals(
            "https://source.test/book/1/detail/index.html#top",
            SharedUrlResolver.resolve(base, "#top")
        )
    }

    @Test
    fun leavesBlankTargetsAndUnsupportedBaseUrlsUnchanged() {
        assertEquals("", SharedUrlResolver.resolve("https://source.test/book/1", ""))
        assertEquals("chapter/1.html", SharedUrlResolver.resolve("file:///tmp/book.html", "chapter/1.html"))
    }
}
