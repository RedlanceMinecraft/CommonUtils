package org.redlance.common.cache;

import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class CacheTemplate<K, V> extends BaseCache<Map<K, V>> {
    private final boolean concurrent;

    public CacheTemplate(Path path, ObjectMapper mapper, boolean concurrent, Class<K> keyClass, Class<V> valueClass) {
        this(path, mapper, concurrent,
                mapper.getTypeFactory().constructType(keyClass),
                mapper.getTypeFactory().constructType(valueClass)
        );
    }

    public CacheTemplate(Path path, ObjectMapper mapper, boolean concurrent, JavaType key, JavaType value) {
        super(path, mapper, concurrent ? ConcurrentHashMap::new : LinkedHashMap::new, mapper.getTypeFactory().constructMapType(
                concurrent ? ConcurrentHashMap.class : LinkedHashMap.class, key, value
        ));

        this.concurrent = concurrent;
    }

    public void write(K key, V value) {
        V previous = getObj().put(key, value);
        if (!Objects.equals(previous, value)) {
            setDirty();
        }
    }

    public V writeIfEmpty(K key, Function<? super K, ? extends V> mappingFunction) {
        return getObj().computeIfAbsent(key, k -> {
            V map = mappingFunction.apply(k);

            setDirty();
            return map;
        });
    }

    public V remove(K key) {
        V remove = getObj().remove(key);
        if (remove != null) {
            setDirty();
        }
        return remove;
    }

    public boolean hasKey(K key) {
        return getObj().containsKey(key);
    }

    public Optional<V> getOptionalValueByKey(K key) {
        return Optional.ofNullable(getValueByKey(key));
    }

    public V getValueByKey(K key) {
        if (key == null) return null;
        return getObj().get(key);
    }

    public Optional<K> getKeyByValue(Object value) {
        return getKeyByValue(value, null);
    }

    public Optional<K> getKeyByValue(Object value, @Nullable Predicate<Map.Entry<K, V>> predicate) {
        if (value == null) return Optional.empty();
        return getObj().entrySet().stream()
                .filter(entry -> value.equals(entry.getValue()) && (predicate == null || predicate.test(entry)))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public Stream<Map.Entry<K, V>> getEntryStream() {
        return getObj().entrySet().stream();
    }

    public Collection<V> getValues() {
        return getObj().values();
    }

    public Set<K> getKeys() {
        return getObj().keySet();
    }

    @Override
    public Map<K, V> read() {
        Map<K, V> readed = super.read();

        if (this.concurrent && readed != null) {
            Map<K, V> newMap = this.defaultObj.get();
            newMap.putAll(readed);

            return newMap;
        }

        return readed;
    }

    @Override
    public String toString() {
        return String.format("CacheTemplate{%s (%s)}", this.path,
                this.obj != null && this.obj.isDone() ? getObj().size() : 0
        );
    }
}
