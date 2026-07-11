package org.redlance.common.requester.openpanel;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redlance.common.jackson.JacksonMappers;
import org.redlance.common.requester.openpanel.payload.IdentifyPayload;
import org.redlance.common.requester.openpanel.payload.IncrementPayload;
import org.redlance.common.requester.openpanel.payload.TrackPayload;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class OpenPanelRequesterTest {
    private final LinkedBlockingQueue<CapturedRequest> requests = new LinkedBlockingQueue<>();
    private HttpServer server;
    private OpenPanelRequester requester;

    @BeforeEach
    void setUp() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/track", this::handleSuccess);
        this.server.start();
        this.requester = new OpenPanelRequester(
                "client-id",
                "client-secret",
                "http://127.0.0.1:" + this.server.getAddress().getPort()
        );
    }

    @AfterEach
    void tearDown() {
        this.server.stop(0);
    }

    @Test
    void sendsTypeSpecificPayloadsAsynchronously() throws Exception {
        ObjectNode trackProperties = JsonNodeFactory.instance.objectNode().put("liked", true);
        TrackPayload trackPayload = new TrackPayload("emote_liked", "42", trackProperties);
        trackProperties.put("liked", false);

        this.requester.track(
                trackPayload,
                "203.0.113.10",
                "Emotecraft/1.0"
        ).join();

        CapturedRequest track = this.requests.take();
        Assertions.assertEquals("client-id", track.clientId());
        Assertions.assertEquals("client-secret", track.clientSecret());
        Assertions.assertEquals("203.0.113.10", track.clientIp());
        Assertions.assertEquals("Emotecraft/1.0", track.userAgent());
        Assertions.assertEquals("track", track.body().get("type").stringValue());
        Assertions.assertEquals("emote_liked", track.body().get("payload").get("name").stringValue());
        Assertions.assertEquals("42", track.body().get("payload").get("profileId").stringValue());
        Assertions.assertTrue(track.body().get("payload").get("properties").get("liked").booleanValue());

        this.requester.identify(new IdentifyPayload(
                "42", "Dima", null, "dima@example.com", "https://example.com/avatar.png",
                JsonNodeFactory.instance.objectNode().put("is_public", true)
        )).join();
        CapturedRequest identify = this.requests.take();
        Assertions.assertEquals("identify", identify.body().get("type").stringValue());
        Assertions.assertEquals("42", identify.body().get("payload").get("profileId").stringValue());
        Assertions.assertEquals("Dima", identify.body().get("payload").get("firstName").stringValue());
        Assertions.assertFalse(identify.body().get("payload").has("lastName"));

        this.requester.increment(new IncrementPayload("42", "emotes_downloaded", 2)).join();
        CapturedRequest increment = this.requests.take();
        Assertions.assertEquals("increment", increment.body().get("type").stringValue());
        Assertions.assertEquals("emotes_downloaded", increment.body().get("payload").get("property").stringValue());
        Assertions.assertEquals(2, increment.body().get("payload").get("value").longValue());
        Assertions.assertFalse(increment.body().get("payload").has("name"));
    }

    @Test
    void identifiesProfileLazilyAndDeduplicatesConcurrentEvents() throws Exception {
        AtomicInteger lookups = new AtomicInteger();
        this.requester = new OpenPanelRequester(
                "client-id",
                "client-secret",
                "http://127.0.0.1:" + this.server.getAddress().getPort()
        );
        OpenPanelProfileSupplier profileSupplier = () -> {
            lookups.incrementAndGet();
            return new IdentifyPayload(
                    "42", "Dima", null, "dima@example.com", null,
                    JsonNodeFactory.instance.objectNode()
            );
        };

        CompletableFuture.allOf(
                this.requester.track(new TrackPayload("first", "42"), profileSupplier, null, null),
                this.requester.track(new TrackPayload("second", "42"), profileSupplier, null, null),
                this.requester.increment(
                        new IncrementPayload("42", "downloads", 1), profileSupplier, null, null
                )
        ).join();

        CapturedRequest first = this.requests.poll(5, TimeUnit.SECONDS);
        CapturedRequest second = this.requests.poll(5, TimeUnit.SECONDS);
        CapturedRequest third = this.requests.poll(5, TimeUnit.SECONDS);
        CapturedRequest fourth = this.requests.poll(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(first);
        Assertions.assertNotNull(second);
        Assertions.assertNotNull(third);
        Assertions.assertNotNull(fourth);
        Assertions.assertEquals("identify", first.body().get("type").stringValue());

        Set<String> types = Set.of(first, second, third, fourth).stream()
                .map(request -> request.body().get("type").stringValue())
                .collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("track", "increment", "identify"), types);
        Assertions.assertEquals(1, lookups.get());
    }

    @Test
    void sendsEventAfterFailedIdentificationAndHonorsCooldown() throws Exception {
        this.server.removeContext("/track");
        this.server.createContext("/track", exchange -> {
            CapturedRequest request = capture(exchange);
            this.requests.add(request);
            String type = request.body().get("type").stringValue();
            respond(exchange, "identify".equals(type) ? 503 : 200, "response");
        });
        this.requester = new OpenPanelRequester(
                "client-id",
                "client-secret",
                "http://127.0.0.1:" + this.server.getAddress().getPort()
        );
        OpenPanelProfileSupplier profileSupplier = () -> new IdentifyPayload("42");

        this.requester.track(
                new TrackPayload("first", "42"), profileSupplier, null, null
        ).join();
        Assertions.assertEquals("identify", this.requests.take().body().get("type").stringValue());
        Assertions.assertEquals("track", this.requests.take().body().get("type").stringValue());

        this.requester.track(
                new TrackPayload("second", "42"), profileSupplier, null, null
        ).join();
        Assertions.assertEquals("track", this.requests.take().body().get("type").stringValue());
        Assertions.assertTrue(this.requests.isEmpty());
    }

    @Test
    void acceptsCompleteProfileFromPerCallSupplier() throws Exception {
        record User(String id, String displayName, String email) {
        }
        User user = new User("42", "Dima", "dima@example.com");

        this.requester.track(
                new TrackPayload("profile_event", user.id()),
                () -> new IdentifyPayload(
                        user.id(), user.displayName(), null, user.email(), null,
                        JsonNodeFactory.instance.objectNode().put("source", "domain-context")
                ),
                null,
                null
        ).join();

        CapturedRequest identify = this.requests.take();
        Assertions.assertEquals("identify", identify.body().get("type").stringValue());
        Assertions.assertEquals("Dima", identify.body().get("payload").get("firstName").stringValue());
        Assertions.assertEquals(
                "domain-context",
                identify.body().get("payload").get("properties").get("source").stringValue()
        );
        Assertions.assertEquals("track", this.requests.take().body().get("type").stringValue());
    }

    @Test
    void completesExceptionallyOnNonSuccessfulStatus() {
        this.server.removeContext("/track");
        this.server.createContext("/track", exchange -> respond(exchange, 401, "{\"message\":\"Unauthorized\"}"));

        CompletionException thrown = Assertions.assertThrows(
                CompletionException.class,
                () -> this.requester.track(new TrackPayload("test")).join()
        );
        OpenPanelRequestException cause = Assertions.assertInstanceOf(
                OpenPanelRequestException.class,
                thrown.getCause()
        );
        Assertions.assertEquals(401, cause.statusCode());
        Assertions.assertEquals("{\"message\":\"Unauthorized\"}", cause.responseBody());
    }

    @Test
    void disablesRequesterWithoutCredentials() {
        this.requester = new OpenPanelRequester(
                "", "", "http://127.0.0.1:" + this.server.getAddress().getPort()
        );

        Assertions.assertFalse(this.requester.enabled());
        this.requester.track(new TrackPayload("ignored")).join();
        Assertions.assertTrue(this.requests.isEmpty());
    }

    @Test
    void boundsPendingOperationsAndDrainsOnShutdown() throws Exception {
        CountDownLatch requestStarted = new CountDownLatch(1);
        CountDownLatch releaseRequest = new CountDownLatch(1);
        this.server.removeContext("/track");
        this.server.createContext("/track", exchange -> {
            requestStarted.countDown();
            try {
                releaseRequest.await();
                handleSuccess(exchange);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                respond(exchange, 500, "interrupted");
            }
        });
        this.requester = new OpenPanelRequester(
                "client-id",
                "client-secret",
                "http://127.0.0.1:" + this.server.getAddress().getPort(),
                Runnable::run,
                1
        );

        CompletableFuture<Void> first = this.requester.track(new TrackPayload("first"));
        Assertions.assertTrue(requestStarted.await(5, TimeUnit.SECONDS));

        CompletionException rejected = Assertions.assertThrows(
                CompletionException.class,
                () -> this.requester.track(new TrackPayload("overflow")).join()
        );
        Assertions.assertInstanceOf(RejectedExecutionException.class, rejected.getCause());

        CompletableFuture<Void> shutdown = this.requester.shutdown();
        Assertions.assertFalse(shutdown.isDone());
        releaseRequest.countDown();
        first.join();
        shutdown.join();

        CompletionException stopped = Assertions.assertThrows(
                CompletionException.class,
                () -> this.requester.track(new TrackPayload("after-shutdown")).join()
        );
        Assertions.assertInstanceOf(IllegalStateException.class, stopped.getCause());
    }

    private void handleSuccess(HttpExchange exchange) throws IOException {
        this.requests.add(capture(exchange));
        respond(exchange, 200, "successful response is deliberately not parsed");
    }

    private static CapturedRequest capture(HttpExchange exchange) throws IOException {
        return new CapturedRequest(
                JacksonMappers.OBJECT_MAPPER.readTree(exchange.getRequestBody()),
                exchange.getRequestHeaders().getFirst("openpanel-client-id"),
                exchange.getRequestHeaders().getFirst("openpanel-client-secret"),
                exchange.getRequestHeaders().getFirst("x-client-ip"),
                exchange.getRequestHeaders().getFirst("User-Agent")
        );
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private record CapturedRequest(
            JsonNode body,
            String clientId,
            String clientSecret,
            String clientIp,
            String userAgent
    ) {
    }
}
