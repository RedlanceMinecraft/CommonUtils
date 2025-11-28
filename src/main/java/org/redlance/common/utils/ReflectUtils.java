package org.redlance.common.utils;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

@SuppressWarnings("unused")
public class ReflectUtils {
    public static final Unsafe UNSAFE = uncheck(() -> {
        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        return (Unsafe) theUnsafe.get(null);
    });

    public static final Object INTERNAL_UNSAFE = uncheck(() -> {
        Field theUnsafe = Unsafe.class.getDeclaredField("theInternalUnsafe");
        theUnsafe.setAccessible(true);
        return theUnsafe.get(null);
    });

    public static final Method OBJECT_FIELD_OFFSET = uncheck(() ->
            INTERNAL_UNSAFE.getClass().getDeclaredMethod("objectFieldOffset", Field.class)
    );

    @SuppressWarnings("deprecation")
    public static final MethodHandles.Lookup TRUSTED_LOOKUP = uncheck(() -> {
        Field hackfield = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        return (MethodHandles.Lookup) UNSAFE.getObject(UNSAFE.staticFieldBase(hackfield), UNSAFE.staticFieldOffset(hackfield));
    });

    static {
        try {
            ReflectUtils.TRUSTED_LOOKUP.findVarHandle(AccessibleObject.class, "override", boolean.class).set(OBJECT_FIELD_OFFSET, true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

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

    public static void setRecordField(Object record, String fieldName, Object newValue) {
        try {
            Field field = record.getClass().getDeclaredField(fieldName);
            long offset = (long) OBJECT_FIELD_OFFSET.invoke(INTERNAL_UNSAFE, field);
            ReflectUtils.UNSAFE.putObject(record, offset, newValue);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to mutate record field " + fieldName, e);
        }
    }
}
