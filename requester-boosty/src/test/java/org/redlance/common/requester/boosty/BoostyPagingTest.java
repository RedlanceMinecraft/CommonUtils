package org.redlance.common.requester.boosty;

import com.github.mizosoft.methanol.TypeRef;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redlance.common.requester.Chunker;
import org.redlance.common.requester.Paginator;
import org.redlance.common.requester.boosty.obj.donation.Donation;
import org.redlance.common.requester.boosty.obj.post.PostSales;
import org.redlance.common.requester.boosty.obj.user.BoostyUser;
import org.redlance.common.requester.boosty.paginators.BoostyLegacyPaginator;
import org.redlance.common.requester.boosty.paginators.BoostyPaginator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Walks pages served the way Boosty serves them.
 */
class BoostyPagingTest {
    private final List<Integer> requested = new CopyOnWriteArrayList<>();
    private volatile Map<Integer, String> pages = Map.of();
    private volatile int status = 200;
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/page", this::respond);
        this.server.start();
    }

    @AfterEach
    void stopServer() {
        this.server.stop(0);
    }

    @Test
    void walksEveryPage() throws Exception {
        this.pages = Map.of(
                0, "{\"data\": {\"postsSales\": []}, \"extra\": {\"offset\": \"100\", \"total\": 250}}",
                100, "{\"data\": {\"postsSales\": []}, \"extra\": {\"offset\": \"200\", \"total\": 250}}",
                200, "{\"data\": {\"postsSales\": []}, \"extra\": {\"offset\": \"300\", \"total\": 250}}"
        );

        Assertions.assertNull(walk(new TypeRef<BoostyPaginator<PostSales>>() {}));
        Assertions.assertEquals(List.of(0, 100, 200), this.requested);
    }

    @Test
    void stopsWhenOffsetDoesNotMove() throws Exception {
        this.pages = Map.of(0, "{\"data\": {\"postsSales\": []}, \"extra\": {\"offset\": \"\", \"total\": 136}}");

        Assertions.assertNull(walk(new TypeRef<BoostyPaginator<PostSales>>() {}));
        Assertions.assertEquals(List.of(0), this.requested);
    }

    @Test
    void stopsOnEmptyPage() throws Exception {
        this.pages = Map.of(
                0, "{\"data\": [{\"id\": 1}], \"limit\": 300, \"offset\": 300, \"total\": 310}",
                300, "{\"data\": [], \"limit\": 300, \"offset\": 300, \"total\": 310}"
        );

        Assertions.assertNull(walk(new TypeRef<BoostyLegacyPaginator<List<BoostyUser>>>() {}));
        Assertions.assertEquals(List.of(0, 300), this.requested);
    }

    @Test
    void throwsBoostyErrors() {
        this.status = 401;
        this.pages = Map.of(0, "{\"error\": \"unauthorized\", \"error_description\": \"Authorization required\"}");

        BoostyRequestException exception = Assertions.assertThrows(BoostyRequestException.class,
                () -> walk(new TypeRef<BoostyLegacyPaginator<List<BoostyUser>>>() {}));
        Assertions.assertEquals("unauthorized", exception.error());
        Assertions.assertEquals("Authorization required", exception.errorDescription());
    }

    @Test
    void failsOnPageWithoutData() {
        this.status = 429;
        this.pages = Map.of(0, "{\"message\": \"Too many requests\"}");

        BoostyRequestException exception = Assertions.assertThrows(BoostyRequestException.class,
                () -> walk(new TypeRef<BoostyPaginator<PostSales>>() {}));
        Assertions.assertNull(exception.error());
        Assertions.assertEquals("Boosty sent no page", exception.getMessage());
    }

    @Test
    void failsMidWalkOnEmptyAnswer() {
        this.pages = Map.of(
                0, "{\"data\": [{\"id\": 1}], \"limit\": 300, \"offset\": 300, \"total\": 600}",
                300, "{}"
        );

        Assertions.assertThrows(BoostyRequestException.class,
                () -> walk(new TypeRef<BoostyLegacyPaginator<List<BoostyUser>>>() {}));
        Assertions.assertEquals(List.of(0, 300), this.requested);
    }

    @Test
    void rejectsDonationsOfUnknownKind() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> BoostyRequester.requestDonationsChunking(
                page -> null, "blog", "token", Donation.Type.UNKNOWN, 0
        ));
    }

    @Test
    void keepsNothingOnDisk() {
        Assertions.assertTrue(BoostyRequester.HTTP_CLIENT.cache().isEmpty(), "Boosty answers hold personal data");
    }

    private <P, V extends Paginator<P>> Object walk(TypeRef<V> type) throws IOException, InterruptedException {
        int port = this.server.getAddress().getPort();
        return Chunker.sendChunkingRequest(BoostyRequester.HTTP_CLIENT, page -> null, paginator -> HttpRequest.newBuilder(URI.create(
                "http://127.0.0.1:" + port + "/page?offset=" + (paginator == null ? 0 : paginator.offset())
        )).build(), type);
    }

    private void respond(HttpExchange exchange) throws IOException {
        int offset = Integer.parseInt(exchange.getRequestURI().getQuery().substring("offset=".length()));
        this.requested.add(offset);

        byte[] body = this.pages.getOrDefault(offset, "{}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(this.status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
