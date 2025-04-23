package org.redlance.common.utils.requester;

import org.redlance.common.CommonUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class UrlProxySelector extends ProxySelector {
    protected static final InetSocketAddress LOCAL_VLESS = InetSocketAddress.createUnresolved("192.168.0.101", 25564);
    protected static final List<Proxy> NO_PROXY_LIST = List.of(Proxy.NO_PROXY);

    public static final UrlProxySelector INSTANCE = new UrlProxySelector();

    protected final Map<Predicate<URI>, Proxy> proxies = new HashMap<>();
    protected UrlProxySelector() {
        // no-op
    }

    @Override
    public List<Proxy> select(URI uri) {
        List<Proxy> proxies = new ArrayList<>();

        for (var proxy : this.proxies.entrySet()) {
            if (proxy.getKey().test(uri)) {
                proxies.add(proxy.getValue());
            }
        }

        if (proxies.isEmpty()) {
            return NO_PROXY_LIST;
        }

        return proxies;
    }

    public Proxy registerDomainPredicateToLocal(String domain) {
        return registerDomainPredicate(domain, new Proxy(Proxy.Type.HTTP, UrlProxySelector.LOCAL_VLESS));
    }

    public Proxy registerDomainPredicate(String domain, InetSocketAddress address) {
        return registerDomainPredicate(domain, new Proxy(Proxy.Type.HTTP, address));
    }

    public Proxy registerDomainPredicate(String domain, Proxy proxy) {
        return registerCustomPredicate(uri -> uri.getHost().contains(domain), proxy);
    }

    public Proxy registerCustomPredicate(Predicate<URI> predicate, InetSocketAddress address) {
        return this.proxies.put(predicate, new Proxy(Proxy.Type.HTTP, address));
    }

    public Proxy registerCustomPredicate(Predicate<URI> predicate, Proxy proxy) {
        return this.proxies.put(predicate, proxy);
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        CommonUtils.LOGGER.warn("Failed to connect to {} (via {})!", uri, sa, ioe);
    }
}
