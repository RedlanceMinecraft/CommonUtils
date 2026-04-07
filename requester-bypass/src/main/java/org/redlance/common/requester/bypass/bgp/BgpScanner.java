package org.redlance.common.requester.bypass.bgp;

import org.redlance.common.requester.bypass.utils.DirectTlsProbe;
import org.redlance.common.requester.bypass.utils.RangeUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BgpScanner implements Runnable {
    private final String origin;
    private final String hostname;
    private final int port;
    private final String path;
    private final int minBytes;
    private final ExecutorService executor;
    private final CompletableFuture<InetAddress> result = new CompletableFuture<>();

    public BgpScanner(String origin, String hostname, int port, String path, int minBytes) {
        this.origin = origin;
        this.hostname = hostname;
        this.port = port;
        this.path = path;
        this.minBytes = minBytes;

        this.executor = Executors.newFixedThreadPool(50, Thread.ofPlatform()
                .name("bgp-scanner-", 0)
                .daemon(true)
                .factory()
        );
    }

    public static CompletableFuture<InetAddress> findWorking(String origin, String hostname, int port, String path, int minBytes) {
        BgpScanner scanner = new BgpScanner(origin, hostname, port, path, minBytes);
        Thread.ofPlatform().daemon(true).start(scanner);
        return scanner.result;
    }

    @Override
    public void run() {
        try {
            BgpRangeFetcher.fetchBgpRanges(this.origin, this::onRange);
        } catch (IOException e) {
            if (!this.result.isDone()) this.result.completeExceptionally(e);
        } finally {
            if (this.result.isDone()) this.executor.shutdown();
        }
    }

    private boolean onRange(String range) {
        for (InetAddress ip : RangeUtils.expandRange(range, 20)) {
            if (this.result.isDone()) return false;
            this.executor.submit(() -> probe(ip));
        }
        return true;
    }

    private void probe(InetAddress ip) {
        if (this.result.isDone()) return;

        try {
            int bytes = DirectTlsProbe.probe(ip, this.hostname, this.port, this.path, this.minBytes, 3000);

            if (bytes >= this.minBytes && !this.result.isDone()) {
                this.result.complete(ip);
                this.executor.shutdownNow();
            }
        } catch (IOException ignored) {}
    }
}
