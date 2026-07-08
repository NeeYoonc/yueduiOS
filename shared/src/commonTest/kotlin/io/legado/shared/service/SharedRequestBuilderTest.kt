package io.legado.shared.service

import io.legado.shared.platform.SharedHttpMethod
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedRequestBuilderTest {
    @Test
    fun parsesExtendedHttpMethodsFromUrlOptions() {
        assertEquals(
            SharedHttpMethod.PUT,
            SharedRequestBuilder.build("""https://dav.test/a,{"method":"PUT","body":"x"}""").method
        )
        assertEquals(
            SharedHttpMethod.DELETE,
            SharedRequestBuilder.build("""https://dav.test/a,{"method":"DELETE"}""").method
        )
        assertEquals(
            SharedHttpMethod.PROPFIND,
            SharedRequestBuilder.build("""https://dav.test/a,{"method":"PROPFIND"}""").method
        )
    }
}
