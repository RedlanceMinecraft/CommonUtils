package org.redlance.common.utils;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.Collection;

public class ReflectUtils {
    private static final Unsafe UNSAFE = uncheck(() -> {
        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        return (Unsafe) theUnsafe.get(null);
    });

    @SuppressWarnings("removal")
    public static final MethodHandles.Lookup TRUSTED_LOOKUP = uncheck(() -> {
        Field hackfield = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        return (MethodHandles.Lookup) UNSAFE.getObject(UNSAFE.staticFieldBase(hackfield), UNSAFE.staticFieldOffset(hackfield));
    });

    private static final Class<?> UNMODIFIABLE_COLLECTION_CLASS = ReflectUtils.uncheck(() ->
            Class.forName("java.util.Collections$UnmodifiableCollection")
    );
    private static final VarHandle COLLECTION_HANDLE = ReflectUtils.uncheck(() ->
            ReflectUtils.TRUSTED_LOOKUP.findVarHandle(UNMODIFIABLE_COLLECTION_CLASS, "c", Collection.class)
    );

    public static <R, E extends Exception> R uncheck(LambdaExceptionUtils.Supplier_WithExceptions<R, E> supplier) {
        try {
            return supplier.get();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Collection<T> getModifiableCollection(Collection<T> unmodifiable) {
        return (Collection<T>) COLLECTION_HANDLE.get(unmodifiable);
    }
}
