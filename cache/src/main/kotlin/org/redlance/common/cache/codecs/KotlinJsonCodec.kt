@file:Suppress("unused")
package org.redlance.common.cache.codecs

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import org.redlance.common.cache.CacheCodec
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
class KotlinJsonCodec<T>(val json: Json, val serializer: KSerializer<T>) : CacheCodec<T> {
    override fun write(outputStream: OutputStream, value: T) {
        this.json.encodeToStream(serializer, value, outputStream)
    }

    override fun read(inputStream: InputStream): T {
        return this.json.decodeFromStream(serializer, inputStream)
    }
}

inline fun <reified T> KotlinJsonCodec(json: Json): KotlinJsonCodec<T> = KotlinJsonCodec(json, serializer())
