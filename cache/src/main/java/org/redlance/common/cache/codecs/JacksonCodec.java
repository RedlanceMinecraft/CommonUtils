package org.redlance.common.cache.codecs;

import org.redlance.common.cache.CacheCodec;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused") // API
public class JacksonCodec<T> implements CacheCodec<T> {
    protected final ObjectMapper mapper;
    private final JavaType javaType;

    public JacksonCodec(ObjectMapper mapper, JavaType javaType) {
        this.mapper = mapper;
        this.javaType = javaType;
    }

    @Override
    public void write(OutputStream os, T value) {
        this.mapper.writerFor(this.javaType).writeValue(os, value);
    }

    @Override
    public T read(InputStream is) {
        return this.mapper.readerFor(this.javaType).readValue(is);
    }

    public static <K, V> JacksonCodec<Map<K, V>> createForMap(ObjectMapper mapper, boolean concurrent, Class<K> keyClass, Class<V> valueClass) {
        return createForMap(mapper, concurrent,
                mapper.getTypeFactory().constructType(keyClass),
                mapper.getTypeFactory().constructType(valueClass)
        );
    }

    public static <K, V> JacksonCodec<Map<K, V>> createForMap(ObjectMapper mapper, boolean concurrent, JavaType key, JavaType value) {
        return new JacksonCodec<>(mapper, mapper.getTypeFactory().constructMapType(
                concurrent ? ConcurrentHashMap.class : LinkedHashMap.class, key, value
        ));
    }
}
