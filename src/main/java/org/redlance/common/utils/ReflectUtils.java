package org.redlance.common.utils;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public class ReflectUtils {
    public static final Unsafe UNSAFE = uncheck(() -> {
        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        return (Unsafe) theUnsafe.get(null);
    });

    public static final MethodHandles.Lookup TRUSTED_LOOKUP = uncheck(() -> {
        Field hackfield = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        return (MethodHandles.Lookup) getStaticField(hackfield);
    });

    public static <R, E extends Exception> R uncheck(LambdaExceptionUtils.Supplier_WithExceptions<R, E> supplier) {
        try {
            return supplier.get();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @SuppressWarnings("deprecation")
    public static Object getStaticField(Field hackfield) {
        return UNSAFE.getObject(UNSAFE.staticFieldBase(hackfield), UNSAFE.staticFieldOffset(hackfield));
    }

    @SuppressWarnings("deprecation")
    public static void setStaticField(Field hackfield, Object value) {
        UNSAFE.putObject(ReflectUtils.UNSAFE.staticFieldBase(hackfield), ReflectUtils.UNSAFE.staticFieldOffset(hackfield), value);
    }
}
