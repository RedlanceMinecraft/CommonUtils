package org.redlance.common.requester.bypass;

import org.junit.jupiter.api.Test;
import org.redlance.common.requester.bypass.bgp.BgpScanner;
import org.redlance.common.requester.bypass.dns.OverrideInetAddressResolver;
import org.redlance.common.requester.bypass.dns.ResolverInstaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class BgpScannerTest {
    public static final Logger LOGGER = LoggerFactory.getLogger("requester-bypass-tests");

    @Test
    void findWorking() throws Exception {
        InetAddress ip = BgpScanner.findWorking("AS13335", "probe.redlance.org", 443, "/32k", 30_000).join();

        assertNotNull(ip);
        BgpScannerTest.LOGGER.info("Found: {}", ip.getHostAddress());

        OverrideInetAddressResolver.override("api.constructlegacy.ru", ip);
        ResolverInstaller.install();
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(URI.create("https://api.constructlegacy.ru/websockets/online-emotes/verify-epa-changelog")).build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            BgpScannerTest.LOGGER.info("Status: {}, Body: {} bytes", response.statusCode(), response.body().length());
            assertTrue(response.body().length() > 16_000);
            BgpScannerTest.LOGGER.info(response.body());
        } finally {
            OverrideInetAddressResolver.clear("api.constructlegacy.ru");
        }
    }
}
