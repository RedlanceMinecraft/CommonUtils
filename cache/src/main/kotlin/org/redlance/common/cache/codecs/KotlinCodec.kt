@file:Suppress("unused")
package org.redlance.common.cache.codecs

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.redlance.common.cache.CacheCodec
import java.io.InputStream
import java.io.OutputStream

class KotlinCodec<T>(val format: BinaryFormat, val serializer: KSerializer<T>) : CacheCodec<T> {
    override fun write(outputStream: OutputStream, value: T) {
        outputStream.write(this.format.encodeToByteArray(serializer, value))
    }

    override fun read(inputStream: InputStream): T {
        return this.format.decodeFromByteArray(serializer, inputStream.readBytes())
    }
}

inline fun <reified T> KotlinCodec(format: BinaryFormat): KotlinCodec<T> = KotlinCodec(format, serializer())
