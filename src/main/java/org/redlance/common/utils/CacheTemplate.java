package org.redlance.common.utils;

import com.google.gson.reflect.TypeToken;
import io.github.kosmx.emotes.executor.EmoteInstance;
import io.github.kosmx.emotes.server.config.Serializer;
import org.redlance.common.CommonUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class CacheTemplate<K, V> {
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    public static final Map<String, CacheTemplate<?, ?>> TRACKED_CACHES = new ConcurrentHashMap<>();

    private static final long GAP_SECONDS_THRESHOLD = 30L;

    private final Map<K, V> caches = new ConcurrentHashMap<>();
    private final TypeToken<?> token;
    private final Path path;

    private final List<Consumer<CacheTemplate<K, V>>> listeners = new ArrayList<>();
    private boolean dirty;

    public CacheTemplate(String path, Type... typeArguments) {
        this.token = TypeToken.getParameterized(Map.class, typeArguments);
        this.path = EmoteInstance.instance.getGameDirectory().resolve(path);

        TRACKED_CACHES.put(path, this);
        CommonUtils.LOGGER.debug("{} created!", path);

        read();

        EXECUTOR.scheduleAtFixedRate(
                () -> reload(false, false), GAP_SECONDS_THRESHOLD, GAP_SECONDS_THRESHOLD, TimeUnit.SECONDS
        );
    }

    public void setDirty() {
        if (!this.dirty) {
            CommonUtils.LOGGER.info("{} is now dirty!", this.path);
        }

        this.dirty = true;
    }

    public void write(K key, V value) {
        write(key, value, false);
    }

    public void write(K key, V value, boolean replace) {
        V prev;
        if (replace && this.caches.containsKey(key)) {
            prev = this.caches.replace(key, value);
        } else {
            prev = this.caches.put(key, value);
        }

        if (prev == null || prev != value)
            setDirty();
    }

    public V writeIfEmpty(K key, Function<? super K, ? extends V> mappingFunction) {
        return this.caches.computeIfAbsent(key, k -> {
            V map = mappingFunction.apply(k);

            setDirty();
            return map;
        });
    }

    public void remove(K key) {
        this.caches.remove(key);
        setDirty();
    }

    public boolean hasKey(K key) {
        return this.caches.containsKey(key);
    }

    public Optional<V> getOptionalValueByKey(K key) {
        return Optional.ofNullable(getValueByKey(key));
    }

    public V getValueByKey(K key) {
        if (key == null || !this.caches.containsKey(key))
            return null;

        return this.caches.get(key);
    }

    public Optional<K> getKeyByValue(Object value) {
        return getKeyByValue(value, kvEntry -> true);
    }

    public Optional<K> getKeyByValue(Object value, Predicate<Map.Entry<K, V>> predicate) {
        if (value == null)
            return Optional.empty();

        return this.caches.entrySet()
                .parallelStream()
                .filter(kvEntry -> kvEntry.getValue().equals(value))
                .filter(predicate)
                .map(Map.Entry::getKey)
                .findAny();
    }

    public Map<K, V> getCacheDirect() {
        return this.caches;
    }

    public List<Consumer<CacheTemplate<K, V>>> getListeners() {
        return this.listeners;
    }

    public boolean subscrube(Consumer<CacheTemplate<K, V>> listener) {
        listener.accept(this); // Begin fire
        return this.listeners.add(listener);
    }

    public boolean unsubscrube(Consumer<CacheTemplate<K, V>> listener) {
        return this.listeners.remove(listener);
    }

    private void fireListeners() {
        if (this.listeners.isEmpty()) {
            return;
        }

        CommonUtils.LOGGER.info("Firing {} listeners for {}!", this.listeners.size(), this);

        for (Consumer<CacheTemplate<K, V>> listener : this.listeners) {
            listener.accept(this);
        }
    }

    protected boolean reload(boolean read, boolean force) {
        if (read & !this.dirty) {
            CommonUtils.LOGGER.info("Reading {}...", this.path);
            read();
        }

        if (!force) {
            if (!this.dirty) { // Never accessed
                return false;
            }

            this.dirty = false;
        }

        CommonUtils.LOGGER.info("Reloading {}...", this.path);
        save();

        return true;
    }

    public Stream<Map.Entry<K, V>> getStream() {
        return this.caches.entrySet().parallelStream();
    }

    public Stream<V> getValuesStream() {
        return this.caches.values().parallelStream();
    }

    public Stream<K> getKeyStream() {
        return this.caches.keySet().parallelStream();
    }

    public void save() {
        try (BufferedWriter writer = Files.newBufferedWriter(this.path)) {

            Serializer.serializer.toJson(this.caches, this.token.getType(), writer);

            writer.flush();

        } catch (Throwable e) {
            CommonUtils.LOGGER.warn("Failed to save caches!", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void read() {
        try (BufferedReader reader = Files.newBufferedReader(this.path)) {

            this.caches.clear();
            this.caches.putAll((Map<? extends K, ? extends V>) Serializer.serializer.fromJson(reader, this.token));

            fireListeners();
        } catch (Throwable e) {
            CommonUtils.LOGGER.warn("Failed to read caches!", e);
        }
    }

    public static void reload(Consumer<Map.Entry<String, CacheTemplate<?, ?>>> reloader, boolean read, boolean force) {
        for (var cacheEntry : CacheTemplate.TRACKED_CACHES.entrySet()) {
            if (cacheEntry.getValue().reload(read, force) && reloader != null) {
                reloader.accept(cacheEntry);
            }
        }
    }

    static {
        CommonUtils.LOGGER.debug("Save hook added!");
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                CacheTemplate.reload(null, false, true), "SaveThread"
        ));
    }
}
