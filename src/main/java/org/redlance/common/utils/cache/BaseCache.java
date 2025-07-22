package org.redlance.common.utils.cache;

import com.google.gson.reflect.TypeToken;
import io.github.kosmx.emotes.server.config.Serializer;
import io.github.kosmx.emotes.server.services.InstanceService;
import org.redlance.common.CommonUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class BaseCache<T> {
    private static final ScheduledExecutorService CACHE_SAVER = CommonUtils.createScheduledExecutor(2, "cache-saver-");
    private static final ExecutorService CACHE_READER = CommonUtils.createExecutor("cache-reader-");

    private static final Map<Path, BaseCache<?>> TRACKED_CACHES = new ConcurrentHashMap<>();

    static {
        CommonUtils.LOGGER.debug("Added cache saving hook!");
        Runnable saveAllDirtyCaches = () -> reloadAll(false);
        BaseCache.CACHE_SAVER.scheduleAtFixedRate(saveAllDirtyCaches, 30L, 30L, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(saveAllDirtyCaches, "CacheSaveThread"));
    }

    private final List<Consumer<BaseCache<T>>> listeners = new CopyOnWriteArrayList<>();

    public final Path path;

    protected final Supplier<T> defaultObj;
    private final TypeToken<T> token;

    protected CompletableFuture<T> obj;
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public BaseCache(String path, Supplier<T> defaultObj, TypeToken<T> token) {
        this(InstanceService.INSTANCE.getGameDirectory().resolve(path), defaultObj, token);
    }

    public BaseCache(Path path, Supplier<T> defaultObj, TypeToken<T> token) {
        this.path = path;
        this.defaultObj = defaultObj;
        this.token = token;

        TRACKED_CACHES.put(path, this);
        CommonUtils.LOGGER.debug("{} created!", path);

        reload(true);
    }

    public void setDirty() {
        if (this.dirty.compareAndSet(false, true)) {
            CommonUtils.LOGGER.info("{} is now dirty!", this);
            fireListeners();
        }
    }

    public List<Consumer<BaseCache<T>>> getListeners() {
        return Collections.unmodifiableList(this.listeners);
    }

    public boolean subscribe(Consumer<BaseCache<T>> listener) {
        return this.listeners.add(listener);
    }

    public boolean unsubscribe(Consumer<BaseCache<T>> listener) {
        return this.listeners.remove(listener);
    }

    protected void fireListeners() {
        if (this.listeners.isEmpty()) return;

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
        try {
            return this.obj.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            CommonUtils.LOGGER.error("Failed to wait for {}!", this, e);
            throw new CompletionException(e);
        } catch (ExecutionException e) {
            CommonUtils.LOGGER.error("Failed to load {}!", this, e.getCause());
            throw new CompletionException(e.getCause());
        }
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

    protected void reload(boolean read) {
        if (this.dirty.getAndSet(false)) {
            CommonUtils.LOGGER.info("Saving {}...", this);
            if (!save()) setDirty();
        } else if (read) {
            CommonUtils.LOGGER.info("Reading {}...", this);
            this.obj = CompletableFuture.supplyAsync(this::read, BaseCache.CACHE_READER);
            this.obj.thenRun(this::fireListeners);
        }
    }

    public T read() {
        if (!Files.exists(this.path)) return this.defaultObj.get();

        try (BufferedReader reader = Files.newBufferedReader(this.path)) {
            T loadedObj = Serializer.getSerializer().fromJson(reader, this.token);
            return loadedObj != null ? loadedObj : this.defaultObj.get();
        } catch (Exception e) {
            CommonUtils.LOGGER.warn("Failed to read {}!", this, e);
            return this.defaultObj.get();
        }
    }

    public boolean save() {
        T obj = getObj(); // Block before a writer
        try (BufferedWriter writer = Files.newBufferedWriter(this.path)) {
            Serializer.getSerializer().toJson(obj, this.token.getType(), writer);
            writer.flush();

            CommonUtils.LOGGER.debug("{} saved!", this);
            return true;
        } catch (Throwable e) {
            CommonUtils.LOGGER.warn("Failed to save {}!", this, e);
            return false;
        }
    }

    public static void reloadAll(boolean read) {
        for (BaseCache<?> cache : TRACKED_CACHES.values()) {
            cache.reload(read);
        }
    }

    @Override
    public String toString() {
        return String.format("Cache{%s}", this.path);
    }
}
