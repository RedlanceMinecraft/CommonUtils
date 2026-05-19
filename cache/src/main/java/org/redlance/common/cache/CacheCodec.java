package org.redlance.common.cache;

import java.io.InputStream;
import java.io.OutputStream;

public interface CacheCodec<T> {
    void write(OutputStream os, T value);
    T read(InputStream is);
}
