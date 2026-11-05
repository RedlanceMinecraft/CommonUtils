package org.redlance.common.requester.openpanel;

import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MutableRequest;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.jetbrains.annotations.Nullable;
import org.redlance.common.requester.HttpClients;
import org.redlance.common.requester.RequesterUtils;
import org.redlance.common.requester.openpanel.payload.AiPayload;
import org.redlance.common.requester.openpanel.payload.IdentifyPayload;
import org.redlance.common.requester.openpanel.payload.IncrementPayload;
import org.redlance.common.requester.openpanel.payload.TrackPayload;

import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Fully asynchronous client for OpenPanel's server-side tracking API.
 *
 * <p>Events associated with a profile wait for a best-effort identification attempt.
 * Concurrent events share one in-flight identification, and an identification failure
 * never prevents the events themselves from being sent.</p>
 */
@SuppressWarnings("unused")
public final class OpenPanelRequester {
    public static final String DEFAULT_API_URL = "https://api.openpanel.dev";

    /**
     * Client of the OpenPanel API: the shared settings without the cache on disk, which keeps only answers to GET,
     * while every request here is a POST. Its own User-Agent goes with requests that forward none: OpenPanel takes it
     * for the visitor's device and files a {@code name/version} one as a server, whereas the shared browser one would
     * make every such event a desktop Chrome.
     */
    private static final Methanol HTTP_CLIENT = HttpClients.newBuilder("http-openpanel")
            .userAgent("common-utils-requester-openpanel/1.0.0")
            .build();

    private static final String CLIENT_ID_HEADER = "openpanel-client-id";
    private static final String CLIENT_SECRET_HEADER = "openpanel-client-secret";
    private static final String CLIENT_IP_HEADER = "x-client-ip";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration IDENTIFIED_TTL = Duration.ofHours(6);
    private static final Duration IDENTIFY_RETRY_COOLDOWN = Duration.ofMinutes(5);
    private static final int MAX_IDENTIFIED = 100_000;
    private static final int DEFAULT_MAX_IN_FLIGHT = 64;
    private static final long FAILURE_WARN_THROTTLE_NANOS = Duration.ofMinutes(1).toNanos();

    /**
     * Successful responses have no useful body, so only rejected responses are materialized.
     */
    private static final HttpResponse.BodyHandler<String> ERROR_BODY_HANDLER = responseInfo ->
            responseInfo.statusCode() >= 300
                    ? HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8)
                    : HttpResponse.BodySubscribers.replacing(null);

    private final String clientId;
    private final String clientSecret;
    private final URI trackUri;
    private final boolean enabled;
    private final Executor profileLookupExecutor;
    private final Semaphore inFlightPermits;
    private final Object lifecycleLock = new Object();
    private int pendingOperations;
    private @Nullable CompletableFuture<Void> shutdownFuture;
    private final Cache<String, Boolean> identified = CacheBuilder.newBuilder()
            .expireAfterWrite(IDENTIFIED_TTL)
            .maximumSize(MAX_IDENTIFIED)
            .build();
    private final Cache<String, Boolean> identifyCooldown = CacheBuilder.newBuilder()
            .expireAfterWrite(IDENTIFY_RETRY_COOLDOWN)
            .maximumSize(MAX_IDENTIFIED)
            .build();
    private final ConcurrentHashMap<String, CompletableFuture<Void>> identificationsInFlight =
            new ConcurrentHashMap<>();
    private final AtomicLong lastFailureWarnNanos = new AtomicLong(
            System.nanoTime() - FAILURE_WARN_THROTTLE_NANOS
    );

    public OpenPanelRequester(String clientId, String clientSecret) {
        this(clientId, clientSecret, DEFAULT_API_URL);
    }

    public OpenPanelRequester(String clientId, String clientSecret, String apiUrl) {
        this(clientId, clientSecret, apiUrl,
                HTTP_CLIENT.executor().orElseThrow(), DEFAULT_MAX_IN_FLIGHT);
    }

    public OpenPanelRequester(
            String clientId,
            String clientSecret,
            String apiUrl,
            Executor profileLookupExecutor
    ) {
        this(clientId, clientSecret, apiUrl, profileLookupExecutor, DEFAULT_MAX_IN_FLIGHT);
    }

    public OpenPanelRequester(
            String clientId,
            String clientSecret,
            String apiUrl,
            Executor profileLookupExecutor,
            int maxInFlight
    ) {
        this.clientId = Objects.requireNonNull(clientId, "clientId").trim();
        this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret").trim();
        String normalizedApiUrl = requireNonBlank(apiUrl, "apiUrl").replaceAll("/+$", "");
        this.trackUri = URI.create(normalizedApiUrl + "/track");
        this.enabled = !this.clientId.isEmpty() && !this.clientSecret.isEmpty();
        this.profileLookupExecutor = Objects.requireNonNull(profileLookupExecutor, "profileLookupExecutor");
        if (maxInFlight <= 0) {
            throw new IllegalArgumentException("maxInFlight must be positive");
        }
        this.inFlightPermits = new Semaphore(maxInFlight);

        if (!this.clientId.isEmpty() && this.clientSecret.isEmpty()) {
            RequesterUtils.LOGGER.warn(
                    "OpenPanel clientSecret is blank; server-side analytics is disabled"
            );
        }
    }

    public boolean enabled() {
        return this.enabled;
    }

    public CompletableFuture<Void> track(TrackPayload payload) {
        return track(payload, null, null);
    }

    public CompletableFuture<Void> track(
            TrackPayload payload,
            @Nullable String clientIp,
            @Nullable String userAgent
    ) {
        return track(payload, null, clientIp, userAgent);
    }

    public CompletableFuture<Void> track(
            TrackPayload payload,
            @Nullable OpenPanelProfileSupplier profileSupplier,
            @Nullable String clientIp,
            @Nullable String userAgent
    ) {
        return submit("track", () -> {
            Objects.requireNonNull(payload, "payload");
            return sendIdentified(
                    "track", payload, payload.profileId(), profileSupplier, clientIp, userAgent
            );
        });
    }

    public CompletableFuture<Void> identify(IdentifyPayload payload) {
        return identify(payload, null, null);
    }

    public CompletableFuture<Void> identify(
            IdentifyPayload payload,
            @Nullable String clientIp,
            @Nullable String userAgent
    ) {
        return submit("identify", () -> {
            Objects.requireNonNull(payload, "payload");
            this.identifyCooldown.put(payload.profileId(), true);
            return send("identify", payload, clientIp, userAgent)
                    .thenRun(() -> this.identified.put(payload.profileId(), true));
        });
    }

    public CompletableFuture<Void> increment(IncrementPayload payload) {
        return increment(payload, null, null);
    }

    public CompletableFuture<Void> increment(
            IncrementPayload payload,
            @Nullable String clientIp,
            @Nullable String userAgent
    ) {
        return increment(payload, null, clientIp, userAgent);
    }

    public CompletableFuture<Void> increment(
            IncrementPayload payload,
            @Nullable OpenPanelProfileSupplier profileSupplier,
            @Nullable String clientIp,
            @Nullable String userAgent
    ) {
        return submit("increment", () -> {
            Objects.requireNonNull(payload, "payload");
            return sendIdentified(
                    "increment", payload, payload.profileId(), profileSupplier, clientIp, userAgent
            );
        });
    }

    public CompletableFuture<Void> ai(AiPayload payload) {
        return ai(payload, null, null);
    }

    public CompletableFuture<Void> ai(
            AiPayload payload,
            @Nullable String clientIp,
            @Nullable String userAgent
    ) {
        return ai(payload, null, clientIp, userAgent);
    }

    /**
     * Sends an AI observability event, see {@link AiPayload}; like a {@code track}, one with a profile waits for its
     * identification.
     */
    public CompletableFuture<Void> ai(
            AiPayload payload,
            @Nullable OpenPanelProfileSupplier profileSupplier,
            @Nullable String clientIp,
            @Nullable String userAgent
    ) {
        return submit("ai", () -> {
            Objects.requireNonNull(payload, "payload");
            return sendIdentified(
                    "ai", payload, payload.profileId(), profileSupplier, clientIp, userAgent
            );
        });
    }

    /**
     * Stops accepting new operations and completes after all accepted operations finish.
     */
    public CompletableFuture<Void> shutdown() {
        synchronized (this.lifecycleLock) {
            if (this.shutdownFuture != null) {
                return this.shutdownFuture;
            }
            this.shutdownFuture = new CompletableFuture<>();
            if (this.pendingOperations == 0) {
                this.shutdownFuture.complete(null);
            }
            return this.shutdownFuture;
        }
    }

    private CompletableFuture<Void> submit(
            String operationName,
            Supplier<CompletableFuture<Void>> operation
    ) {
        if (!this.enabled) {
            return CompletableFuture.completedFuture(null);
        }

        synchronized (this.lifecycleLock) {
            if (this.shutdownFuture != null) {
                return CompletableFuture.failedFuture(
                        new IllegalStateException("OpenPanel requester is shut down")
                );
            }
            if (!this.inFlightPermits.tryAcquire()) {
                var exception = new RejectedExecutionException(
                        "OpenPanel max in-flight operations exceeded"
                );
                logDeliveryFailure(operationName, exception.getMessage());
                return CompletableFuture.failedFuture(exception);
            }
            this.pendingOperations++;
        }

        CompletableFuture<Void> source;
        try {
            source = Objects.requireNonNull(operation.get(), "operation future");
        } catch (Throwable throwable) {
            source = CompletableFuture.failedFuture(throwable);
        }

        var result = new CompletableFuture<Void>();
        source.whenComplete((ignored, throwable) -> {
            this.inFlightPermits.release();
            CompletableFuture<Void> completedShutdown = null;
            synchronized (this.lifecycleLock) {
                this.pendingOperations--;
                if (this.pendingOperations == 0 && this.shutdownFuture != null) {
                    completedShutdown = this.shutdownFuture;
                }
            }
            if (completedShutdown != null) {
                completedShutdown.complete(null);
            }
            if (throwable == null) {
                result.complete(null);
            } else {
                result.completeExceptionally(throwable);
            }
        });
        return result;
    }

    /**
     * Sends an event; one with a {@code profileId} waits for the identification attempt of that profile.
     */
    private CompletableFuture<Void> sendIdentified(
            String type,
            Object payload,
            @Nullable String profileId,
            @Nullable OpenPanelProfileSupplier profileSupplier,
            @Nullable String clientIp,
            @Nullable String userAgent
    ) {
        if (profileId == null) {
            return send(type, payload, clientIp, userAgent);
        }
        return ensureIdentifiedAsync(profileId, profileSupplier, clientIp, userAgent)
                .thenCompose(ignored -> send(type, payload, clientIp, userAgent));
    }

    private CompletableFuture<Void> ensureIdentifiedAsync(
            String profileId,
            @Nullable OpenPanelProfileSupplier profileSupplier,
            @Nullable String clientIp,
            @Nullable String userAgent
    ) {
        if (this.identified.getIfPresent(profileId) != null) {
            return CompletableFuture.completedFuture(null);
        }

        if (profileSupplier == null) {
            return CompletableFuture.completedFuture(null);
        }

        var identification = new CompletableFuture<Void>();
        CompletableFuture<Void> inFlight = this.identificationsInFlight.putIfAbsent(
                profileId, identification
        );
        if (inFlight != null) {
            return inFlight;
        }

        if (this.identified.getIfPresent(profileId) != null
                || this.identifyCooldown.asMap().putIfAbsent(profileId, true) != null) {
            finishIdentification(profileId, identification, null);
            return identification;
        }

        try {
            CompletableFuture.supplyAsync(() -> {
                try {
                    return profileSupplier.get();
                } catch (Exception exception) {
                    throw new CompletionException(exception);
                }
            }, this.profileLookupExecutor).thenCompose(profile -> {
                if (profile == null) {
                    return CompletableFuture.completedFuture(null);
                }
                if (!profileId.equals(profile.profileId())) {
                    throw new IllegalArgumentException(
                            "Profile supplier returned profileId " + profile.profileId()
                                    + " for event profileId " + profileId
                    );
                }
                return send("identify", profile, clientIp, userAgent)
                        .thenRun(() -> this.identified.put(profileId, true));
            }).whenComplete((ignored, throwable) ->
                    finishIdentification(profileId, identification, throwable));
        } catch (Throwable throwable) {
            finishIdentification(profileId, identification, throwable);
        }
        return identification;
    }

    private void finishIdentification(
            String profileId,
            CompletableFuture<Void> identification,
            @Nullable Throwable throwable
    ) {
        if (throwable != null) {
            Throwable cause = unwrap(throwable);
            if (!(cause instanceof OpenPanelRequestException)
                    && !(cause instanceof CancellationException)) {
                logDeliveryFailure("identify", cause.toString());
            }
        }
        this.identificationsInFlight.remove(profileId, identification);
        identification.complete(null);
    }

    private CompletableFuture<Void> send(
            String type,
            Object payload,
            @Nullable String clientIp,
            @Nullable String userAgent
    ) {
        var request = MutableRequest.POST(
                        this.trackUri, new OpenPanelRequest<>(type, payload), MediaType.APPLICATION_JSON
                )
                .timeout(REQUEST_TIMEOUT)
                .header(CLIENT_ID_HEADER, this.clientId)
                .header(CLIENT_SECRET_HEADER, this.clientSecret);

        if (clientIp != null && !clientIp.isBlank()) {
            request.header(CLIENT_IP_HEADER, clientIp);
        }
        if (userAgent != null && !userAgent.isBlank()) {
            request.header("User-Agent", userAgent);
        }

        return HTTP_CLIENT.sendAsync(request.build(), ERROR_BODY_HANDLER)
                .thenAccept(response -> {
                    if (response.statusCode() >= 300) {
                        throw new OpenPanelRequestException(response.statusCode(), response.body());
                    }
                })
                .whenComplete((ignored, throwable) -> {
                    if (throwable == null) {
                        return;
                    }
                    Throwable cause = unwrap(throwable);
                    if (cause instanceof CancellationException) {
                        return;
                    }
                    if (cause instanceof OpenPanelRequestException requestException) {
                        logDeliveryFailure(type, "HTTP " + requestException.statusCode()
                                + " " + requestException.responseBody());
                    } else {
                        logDeliveryFailure(type, cause.toString());
                    }
                });
    }

    private void logDeliveryFailure(String type, String detail) {
        long now = System.nanoTime();
        long last = this.lastFailureWarnNanos.get();
        if (now - last >= FAILURE_WARN_THROTTLE_NANOS
                && this.lastFailureWarnNanos.compareAndSet(last, now)) {
            RequesterUtils.LOGGER.warn("OpenPanel delivery failed ({}): {}", type, detail);
        } else {
            RequesterUtils.LOGGER.debug("OpenPanel delivery failed ({}): {}", type, detail);
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException) && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String requireNonBlank(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }

    private record OpenPanelRequest<T>(String type, T payload) {
    }
}
