package org.redlance.common.requester.bypass.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.spi.InetAddressResolver;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public class OverrideInetAddressResolver implements java.net.spi.InetAddressResolver {
    private static final ConcurrentMap<String, InetAddress> OVERRIDES = new ConcurrentHashMap<>();

    private final InetAddressResolver delegate;

    public OverrideInetAddressResolver(InetAddressResolver delegate) {
        this.delegate = delegate;
    }

    public static void override(String hostname, InetAddress address) {
        OverrideInetAddressResolver.OVERRIDES.put(hostname, address);
    }

    public static void clear(String hostname) {
        OverrideInetAddressResolver.OVERRIDES.remove(hostname);
    }

    public static void clearAll() {
        OverrideInetAddressResolver.OVERRIDES.clear();
    }

    @Override
    public Stream<InetAddress> lookupByName(String host, LookupPolicy lookupPolicy) throws UnknownHostException {
        InetAddress override = OverrideInetAddressResolver.OVERRIDES.get(host);
        System.out.println(host);
        if (override != null) return Stream.of(InetAddress.getByAddress(host, override.getAddress()));
        return this.delegate.lookupByName(host, lookupPolicy);
    }

    @Override
    public String lookupByAddress(byte[] addr) throws UnknownHostException {
        return this.delegate.lookupByAddress(addr);
    }
}
