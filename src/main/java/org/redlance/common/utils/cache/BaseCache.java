package org.redlance.common.utils.cache;

import com.google.gson.reflect.TypeToken;
import io.github.kosmx.emotes.executor.EmoteInstance;
import io.github.kosmx.emotes.server.config.Serializer;
import org.redlance.common.CommonUtils;
import org.redlance.common.utils.CacheTemplate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BaseCache<T> {
    public static final Map<Path, BaseCache<?>> TRACKED_CACHES = new ConcurrentHashMap<>();

    static {
        CommonUtils.LOGGER.debug("Added cache saving hook!");

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                BaseCache.reload(null, false), "CacheSaveThread"
        ));
    }

    private final List<Consumer<BaseCache<T>>> listeners = new ArrayList<>();

    public final Path path;

    protected final Supplier<T> defaultObj;
    private final TypeToken<T> token;

    protected CompletableFuture<T> obj;
    private boolean dirty;

    public BaseCache(String path, Supplier<T> defaultObj, TypeToken<T> token) {
        this(EmoteInstance.instance.getGameDirectory().resolve(path), defaultObj, token);
    }

    public BaseCache(Path path, Supplier<T> defaultObj, TypeToken<T> token) {
        this.path = path;
        this.defaultObj = defaultObj;
        this.token = token;

        TRACKED_CACHES.put(path, this);
        CommonUtils.LOGGER.debug("{} created!", path);

        reload(true);

        CommonUtils.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                () -> reload(false), 30L, 30L, TimeUnit.SECONDS
        );
    }

    public void setDirty() {
        if (!this.dirty) {
            CommonUtils.LOGGER.info("{} is now dirty!", this);
            fireListeners();
        }

        this.dirty = true;
    }

    public List<Consumer<BaseCache<T>>> getListeners() {
        return Collections.unmodifiableList(this.listeners);
    }

    public boolean subscrube(Consumer<BaseCache<T>> listener) {
        return this.listeners.add(listener);
    }

    public boolean unsubscrube(Consumer<BaseCache<T>> listener) {
        return this.listeners.remove(listener);
    }

    protected void fireListeners() {
        if (this.listeners.isEmpty()) {
            return;
        }

        CommonUtils.LOGGER.info("Firing {} listeners for {}!", this.listeners.size(), this);

        for (Consumer<BaseCache<T>> listener : this.listeners) {
            try {
                listener.accept(this);
            } catch (Throwable th) {
                CommonUtils.LOGGER.warn("Failed to fire listener {} for {}!", listener.toString(), this);
            }
        }
    }

    public T getObj() {
        if (!this.obj.isDone()) {
            CommonUtils.LOGGER.debug("Blocking thread '{}' until the {} is read!",
                    Thread.currentThread().getName(), this, new Throwable("Blocking")
            );
        }

        return this.obj.join();
    }

    public void setObj(T obj) {
        setObj(CompletableFuture.completedFuture(obj));
    }

    public CompletableFuture<T> setObj(CompletableFuture<T> obj) {
        this.obj = obj;
        this.obj.thenRun(this::fireListeners);
        setDirty();

        return this.obj;
    }

    protected boolean reload(boolean read) {
        if (!this.dirty) {
            if (read) {
                CommonUtils.LOGGER.info("Reading {}...", this);
                this.obj = CompletableFuture.supplyAsync(this::read, CommonUtils.EXECUTOR);
                this.obj.thenRun(this::fireListeners);
            }

            return false; // Never accessed
        }

        CommonUtils.LOGGER.info("Saving {}...", this);
        this.dirty = false;

        return save();
    }

    public T read() {
        try (BufferedReader reader = Files.newBufferedReader(this.path)) {
            return Serializer.serializer.fromJson(reader, this.token);
        } catch (Throwable e) {
            CommonUtils.LOGGER.warn("Failed to read {}!", this, e);
            return this.defaultObj.get();
        }
    }

    public boolean save() {
        T obj = getObj(); // Block before writer
        try (BufferedWriter writer = Files.newBufferedWriter(this.path)) {
            Serializer.serializer.toJson(obj, this.token.getType(), writer);

            writer.flush();

            return true;
        } catch (Throwable e) {
            CommonUtils.LOGGER.warn("Failed to save {}!", this, e);

            return false;
        }
    }

    public static void reload(Consumer<BaseCache<?>> callback, boolean read) {
        for (BaseCache<?> cache : CacheTemplate.TRACKED_CACHES.values()) {
            if (cache.reload(read) && callback != null) {
                callback.accept(cache);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("Cache{%s}", this.path);
    }
}
