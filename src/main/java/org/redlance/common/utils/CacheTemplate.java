package org.redlance.common.utils;

import com.google.gson.reflect.TypeToken;
import org.redlance.common.utils.cache.BaseCache;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class CacheTemplate<K, V> extends BaseCache<Map<K, V>> {
    private final boolean concurrent;

    @SuppressWarnings("unchecked")
    public CacheTemplate(String path, boolean concurrent, Type... typeArguments) {
        super(path, concurrent ? ConcurrentHashMap::new : HashMap::new, (TypeToken<Map<K, V>>)
                TypeToken.getParameterized(Map.class, typeArguments));

        this.concurrent = concurrent;
    }

    public void write(K key, V value) {
        write(key, value, false);
    }

    public void write(K key, V value, boolean replace) {
        Map<K, V> caches = getObj();

        V prev;
        if (replace && caches.containsKey(key)) {
            prev = caches.replace(key, value);
        } else {
            prev = caches.put(key, value);
        }

        if (prev == null || prev != value) {
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
        if (key == null) {
            return null;
        }

        Map<K, V> caches = getObj();

        if (!caches.containsKey(key)) {
            return null;
        }

        return caches.get(key);
    }

    public Optional<K> getKeyByValue(Object value) {
        return getKeyByValue(value, kvEntry -> true);
    }

    public Optional<K> getKeyByValue(Object value, Predicate<Map.Entry<K, V>> predicate) {
        if (value == null) {
            return Optional.empty();
        }

        return getObj().entrySet().parallelStream()
                .filter(kvEntry -> kvEntry.getValue().equals(value))
                .filter(predicate)
                .map(Map.Entry::getKey)
                .findAny();
    }

    public Stream<Map.Entry<K, V>> getStream() {
        return getObj().entrySet().parallelStream();
    }

    public Stream<V> getValuesStream() {
        return getObj().values().parallelStream();
    }

    public Stream<K> getKeyStream() {
        return getObj().keySet().parallelStream();
    }

    @Override
    public Map<K, V> read() {
        Map<K, V> readed = super.read();

        if (this.concurrent && readed != null && !readed.isEmpty()) {
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
