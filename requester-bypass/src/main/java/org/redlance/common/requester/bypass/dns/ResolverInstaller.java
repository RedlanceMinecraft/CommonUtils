package org.redlance.common.requester.bypass.dns;

import org.redlance.common.utils.ReflectUtils;

import java.lang.invoke.VarHandle;
import java.net.InetAddress;
import java.net.spi.InetAddressResolver;

public class ResolverInstaller {
    private static final VarHandle RESOLVER_HANDLE = ReflectUtils.uncheck(() ->
            ReflectUtils.TRUSTED_LOOKUP.findStaticVarHandle(InetAddress.class, "resolver", InetAddressResolver.class)
    );

    static {
        RESOLVER_HANDLE.set(new OverrideInetAddressResolver((InetAddressResolver) RESOLVER_HANDLE.get()));
    }

    public static void install() {
        // no-op
    }
}
