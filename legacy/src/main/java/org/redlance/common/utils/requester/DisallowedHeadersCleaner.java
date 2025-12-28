/*package org.redlance.common.utils.requester;

import org.redlance.common.CommonUtils;
import org.redlance.common.utils.ReflectUtils;

import java.lang.invoke.VarHandle;
import java.util.Collection;
import java.util.Set;

@SuppressWarnings("unchecked")
public class DisallowedHeadersCleaner {
    private static final Class<?> UTILS_CLASS = ReflectUtils.uncheck(() ->
            Class.forName("jdk.internal.net.http.common.Utils")
    );
    private static final VarHandle DISALLOWED_HEADERS_SET_HANDLE = ReflectUtils.uncheck(() ->
            ReflectUtils.TRUSTED_LOOKUP.findStaticVarHandle(UTILS_CLASS, "DISALLOWED_HEADERS_SET", Set.class)
    );

    static {
        if (ReflectUtils.getModifiableCollection((Set<String>) DISALLOWED_HEADERS_SET_HANDLE.get()) instanceof Collection<?> collection) {
            CommonUtils.LOGGER.warn("Disallowed headers was cleared!");
            collection.clear();
        } else throw new IllegalStateException();
    }

    public static void clearHeaders() {
        // no-op
    }
}*/
