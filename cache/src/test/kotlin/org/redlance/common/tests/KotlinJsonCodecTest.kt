package org.redlance.common.tests

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.redlance.common.cache.CacheTemplate
import org.redlance.common.cache.codecs.KotlinJsonCodec
import java.nio.file.Files
import java.nio.file.Path

class KotlinJsonCodecTest {
    @Serializable
    data class Entry(val name: String, val count: Int)

    private fun createTemplate(): CacheTemplate<String, Entry> {
        return CacheTemplate(PATH, true, KotlinJsonCodec<Map<String, Entry>>(Json))
    }

    @Test
    fun test() {
        val first = createTemplate()
        first.write("alpha", Entry("alpha", 1))
        first.write("beta", Entry("beta", 2))
        first.save()

        val second = createTemplate()
        second.read()
        Assertions.assertEquals(Entry("alpha", 1), second.getValueByKey("alpha"))
        Assertions.assertEquals(Entry("beta", 2), second.getValueByKey("beta"))

        Assertions.assertFalse(second.hasKey("missing"))
        Assertions.assertNull(second.getValueByKey("missing"))
    }

    companion object {
        val PATH: Path = Path.of("kotlin-json-codec-test.json")

        @JvmStatic
        @AfterAll
        fun clean() {
            Files.deleteIfExists(PATH)
        }
    }
}
