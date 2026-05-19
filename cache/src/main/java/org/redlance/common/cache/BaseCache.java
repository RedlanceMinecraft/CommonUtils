package org.redlance.common.cache;

import org.redlance.common.utils.CommonExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class BaseCache<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger("CommonCache");

    private static final ScheduledExecutorService CACHE_SAVER = CommonExecutors.createScheduledExecutor(2, "cache-saver-");
    private static final ExecutorService CACHE_READER = CommonExecutors.createExecutor("cache-reader-");

    private static final Map<Path, BaseCache<?>> TRACKED_CACHES = new ConcurrentHashMap<>();

    static {
        BaseCache.LOGGER.debug("Added json cache saving hook!");
        Runnable saveAllDirtyCaches = () -> reloadAll(false);
        BaseCache.CACHE_SAVER.scheduleAtFixedRate(saveAllDirtyCaches, 30L, 30L, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(saveAllDirtyCaches, "json-cache-saver"));
    }

    private final List<Consumer<BaseCache<T>>> listeners = new CopyOnWriteArrayList<>();

    protected final Path path;
    protected final CacheCodec<T> codec;

    protected final Supplier<T> defaultObj;

    protected CompletableFuture<T> obj;
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public BaseCache(Path path, CacheCodec<T> codec, Supplier<T> defaultObj) {
        this.path = path;
        this.codec = codec;
        this.defaultObj = defaultObj;

        TRACKED_CACHES.put(path, this);
        BaseCache.LOGGER.debug("{} created!", path);

        reload(true);
    }

    public void setDirty() {
        if (this.dirty.compareAndSet(false, true)) {
            BaseCache.LOGGER.info("{} is now dirty!", this);
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

        BaseCache.LOGGER.info("Firing {} listeners for {}!", this.listeners.size(), this);
        for (Consumer<BaseCache<T>> listener : this.listeners) {
            try {
                listener.accept(this);
            } catch (Throwable th) {
                BaseCache.LOGGER.warn("Failed to fire listener {} for {}!", listener.toString(), this, th);
            }
        }
    }

    public T getObj() {
        try {
            return this.obj.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            BaseCache.LOGGER.error("Failed to wait for {}!", this, e);
            throw new CompletionException(e);
        } catch (ExecutionException e) {
            BaseCache.LOGGER.error("Failed to load {}!", this, e.getCause());
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
            BaseCache.LOGGER.info("Saving {}...", this);
            if (!save()) setDirty();
        } else if (read) {
            BaseCache.LOGGER.info("Reading {}...", this);
            this.obj = CompletableFuture.supplyAsync(this::read, BaseCache.CACHE_READER);
            this.obj.thenRun(this::fireListeners);
        }
    }

    public T read() {
        if (!Files.exists(this.path)) return this.defaultObj.get();

        try (InputStream is = Files.newInputStream(this.path)) {
            T loadedObj = this.codec.read(is);
            return loadedObj != null ? loadedObj : this.defaultObj.get();
        } catch (Exception e) {
            BaseCache.LOGGER.warn("Failed to read {}!", this, e);
            return this.defaultObj.get();
        }
    }

    public boolean save() {
        T obj = getObj(); // Block before a writer

        Path parent = this.path.toAbsolutePath().getParent();
        Path tmp;
        try {
            if (parent != null) {
                Files.createDirectories(parent);
                tmp = Files.createTempFile(parent, this.path.getFileName().toString() + "-", ".tmp");
            } else {
                tmp = Files.createTempFile(this.path.getFileName().toString() + "-", ".tmp");
            }
        } catch (IOException e) {
            BaseCache.LOGGER.warn("Failed to create temp file for {}!", this, e);
            return false;
        }

        try {
            try (OutputStream os = Files.newOutputStream(tmp)) {
                this.codec.write(os, obj);
                os.flush();
            }
            Files.move(tmp, this.path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

            BaseCache.LOGGER.debug("{} saved!", this);
            return true;
        } catch (Throwable e) {
            BaseCache.LOGGER.warn("Failed to save {}!", this, e);
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
            }
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
