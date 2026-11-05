package org.redlance.common.requester.openpanel.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Payload of an OpenPanel {@code ai} event: one step of a request to an AI model, see
 * <a href="https://openpanel.dev/docs/ai-observability/manual-capture">manual capture</a>.
 * <p>
 * The events of one request share a {@link #traceId()} and form a tree through {@link #spanId()} and
 * {@link #parentId()}. OpenPanel rejects a {@link Kind#GENERATION} without a {@link #model()}, a {@link #provider()},
 * {@link AiUsage#inputTokens()} and {@link AiUsage#outputTokens()}, and a {@link Kind#EMBEDDING} without a model, a
 * provider and {@link AiUsage#inputTokens()}; so does this record. Start one with {@link #generation},
 * {@link #embedding}, {@link #span} or {@link #trace}.
 * <p>
 * Ids ({@code traceId}, {@code spanId}, {@code parentId}, {@code conversationId}) take up to 128 letters, digits and
 * {@code - _ ~ . @ ( ) ! ' : |}, names up to 200 characters, and a {@code parentId} must differ from the
 * {@code spanId}; OpenPanel checks these rules, this record does not.
 *
 * @param kind               What the event describes.
 * @param traceId            Groups every event of one request.
 * @param spanId             Id of this event, which its children name as their {@code parentId}; OpenPanel uses the
 *                           event id without one.
 * @param parentId           The {@code spanId} of the parent event, or the {@code traceId} for a top-level one.
 * @param name               Display name of the step or trace, e.g. {@code summarize}; {@code ai_name} in reports.
 * @param conversationId     Groups traces into a conversation or thread.
 * @param model              Model, e.g. {@code gpt-5-mini}; OpenPanel looks the cost up by it and the provider.
 * @param provider           Provider of the model, e.g. {@code openai}.
 * @param input              Messages for a generation, strings for an embedding, any JSON for a span or trace; sent
 *                           even when empty and cut at 200 KB.
 * @param output             What the step produced, in the same shapes as {@code input}; sent even when empty and cut
 *                           at 200 KB.
 * @param tools              The tools the model could call; sent even when empty and cut at 200 KB.
 * @param usage              Token counts.
 * @param cost               Cost in USD, stored as given instead of computed; send it for a model OpenPanel's
 *                           catalog does not know, which otherwise costs nothing.
 * @param latencyMs          Duration of the step in milliseconds.
 * @param timeToFirstTokenMs Milliseconds until the first token of the answer.
 * @param error              Failure of the step; its presence marks the event as failed.
 * @param stopReason         Why the model stopped, e.g. {@code stop}, {@code length} or {@code tool-calls}.
 * @param stream             Whether the answer was streamed.
 * @param modelParameters    Parameters of the call: {@code temperature}, {@code maxTokens}, {@code topP},
 *                           {@code topK}, {@code frequencyPenalty}, {@code presencePenalty}, {@code seed},
 *                           {@code stopSequences} and any others.
 * @param privacyMode        Whether OpenPanel drops {@code input}, {@code output} and {@code tools} before storing
 *                           the event.
 * @param properties         Custom properties, filterable like those of any event; OpenPanel drops keys starting
 *                           with {@code ai_} and keeps the first 100.
 * @param profileId          Profile the step belongs to.
 * @param groups             Groups the step belongs to, up to 100.
 * @param deviceId           Device the step belongs to, up to 64 characters.
 * @param timestamp          When the step started, to which OpenPanel back-dates the event.
 */
public record AiPayload(
        Kind kind,
        String traceId,
        @Nullable String spanId,
        @Nullable String parentId,
        @Nullable String name,
        @Nullable String conversationId,
        @Nullable String model,
        @Nullable String provider,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Nullable JsonNode input,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Nullable JsonNode output,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Nullable JsonNode tools,
        @Nullable AiUsage usage,
        @Nullable Double cost,
        @Nullable Long latencyMs,
        @Nullable Long timeToFirstTokenMs,
        @Nullable AiError error,
        @Nullable String stopReason,
        @Nullable Boolean stream,
        @Nullable ObjectNode modelParameters,
        @Nullable Boolean privacyMode,
        ObjectNode properties,
        @Nullable String profileId,
        List<String> groups,
        @Nullable String deviceId,
        @Nullable Instant timestamp
) {
    public AiPayload {
        Objects.requireNonNull(kind, "kind");
        if (Objects.requireNonNull(traceId, "traceId").isBlank()) {
            throw new IllegalArgumentException("traceId must not be blank");
        }
        if (kind == Kind.GENERATION || kind == Kind.EMBEDDING) {
            require(model != null && !model.isBlank(), "model", kind);
            require(provider != null && !provider.isBlank(), "provider", kind);
            require(usage != null && usage.inputTokens() != null, "usage.inputTokens", kind);
        }
        if (kind == Kind.GENERATION) {
            require(usage != null && usage.outputTokens() != null, "usage.outputTokens", kind);
        }

        input = input == null ? null : input.deepCopy();
        output = output == null ? null : output.deepCopy();
        tools = tools == null ? null : tools.deepCopy();
        modelParameters = modelParameters == null ? null : modelParameters.deepCopy();
        properties = Objects.requireNonNull(properties, "properties").deepCopy();
        groups = List.copyOf(Objects.requireNonNull(groups, "groups"));
    }

    /**
     * Starts a {@link Kind#GENERATION}.
     *
     * @param usage Token counts with both {@link AiUsage#inputTokens()} and {@link AiUsage#outputTokens()}.
     */
    public static Builder generation(String traceId, String model, String provider, AiUsage usage) {
        return new Builder(Kind.GENERATION, traceId).model(model).provider(provider).usage(usage);
    }

    /**
     * Starts a {@link Kind#EMBEDDING}.
     *
     * @param usage Token counts with {@link AiUsage#inputTokens()}.
     */
    public static Builder embedding(String traceId, String model, String provider, AiUsage usage) {
        return new Builder(Kind.EMBEDDING, traceId).model(model).provider(provider).usage(usage);
    }

    /**
     * Starts a {@link Kind#SPAN}.
     */
    public static Builder span(String traceId) {
        return new Builder(Kind.SPAN, traceId);
    }

    /**
     * Starts a {@link Kind#TRACE}.
     */
    public static Builder trace(String traceId) {
        return new Builder(Kind.TRACE, traceId);
    }

    private static void require(boolean present, String field, Kind kind) {
        if (!present) {
            throw new IllegalArgumentException(field + " is required for " + kind);
        }
    }

    /**
     * What an {@link AiPayload} describes.
     */
    public enum Kind {
        /**
         * One call of a model that generates an answer.
         */
        @JsonProperty("generation")
        GENERATION,

        /**
         * One call of an embedding model.
         */
        @JsonProperty("embedding")
        EMBEDDING,

        /**
         * A step that is not a model call, e.g. a tool call or a retrieval.
         */
        @JsonProperty("span")
        SPAN,

        /**
         * The root of a trace with its name, total latency, input and output. Optional: OpenPanel builds the trace
         * from its other events.
         */
        @JsonProperty("trace")
        TRACE
    }

    /**
     * Collects the components of an {@link AiPayload} after its kind and trace: each method sets the component of its
     * name. {@link #build()} checks them.
     */
    public static final class Builder {
        private final Kind kind;
        private final String traceId;
        private @Nullable String spanId;
        private @Nullable String parentId;
        private @Nullable String name;
        private @Nullable String conversationId;
        private @Nullable String model;
        private @Nullable String provider;
        private @Nullable JsonNode input;
        private @Nullable JsonNode output;
        private @Nullable JsonNode tools;
        private @Nullable AiUsage usage;
        private @Nullable Double cost;
        private @Nullable Long latencyMs;
        private @Nullable Long timeToFirstTokenMs;
        private @Nullable AiError error;
        private @Nullable String stopReason;
        private @Nullable Boolean stream;
        private @Nullable ObjectNode modelParameters;
        private @Nullable Boolean privacyMode;
        private ObjectNode properties = JsonNodeFactory.instance.objectNode();
        private @Nullable String profileId;
        private List<String> groups = List.of();
        private @Nullable String deviceId;
        private @Nullable Instant timestamp;

        private Builder(Kind kind, String traceId) {
            this.kind = kind;
            this.traceId = traceId;
        }

        public Builder spanId(@Nullable String spanId) {
            this.spanId = spanId;
            return this;
        }

        public Builder parentId(@Nullable String parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder name(@Nullable String name) {
            this.name = name;
            return this;
        }

        public Builder conversationId(@Nullable String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder model(@Nullable String model) {
            this.model = model;
            return this;
        }

        public Builder provider(@Nullable String provider) {
            this.provider = provider;
            return this;
        }

        public Builder input(@Nullable JsonNode input) {
            this.input = input;
            return this;
        }

        public Builder output(@Nullable JsonNode output) {
            this.output = output;
            return this;
        }

        public Builder tools(@Nullable JsonNode tools) {
            this.tools = tools;
            return this;
        }

        public Builder usage(@Nullable AiUsage usage) {
            this.usage = usage;
            return this;
        }

        public Builder cost(double cost) {
            this.cost = cost;
            return this;
        }

        public Builder latencyMs(long latencyMs) {
            this.latencyMs = latencyMs;
            return this;
        }

        public Builder timeToFirstTokenMs(long timeToFirstTokenMs) {
            this.timeToFirstTokenMs = timeToFirstTokenMs;
            return this;
        }

        public Builder error(@Nullable AiError error) {
            this.error = error;
            return this;
        }

        public Builder stopReason(@Nullable String stopReason) {
            this.stopReason = stopReason;
            return this;
        }

        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder modelParameters(@Nullable ObjectNode modelParameters) {
            this.modelParameters = modelParameters;
            return this;
        }

        public Builder privacyMode(boolean privacyMode) {
            this.privacyMode = privacyMode;
            return this;
        }

        public Builder properties(ObjectNode properties) {
            this.properties = properties;
            return this;
        }

        public Builder profileId(@Nullable String profileId) {
            this.profileId = profileId;
            return this;
        }

        public Builder groups(List<String> groups) {
            this.groups = groups;
            return this;
        }

        public Builder deviceId(@Nullable String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder timestamp(@Nullable Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Builds the payload.
         *
         * @throws IllegalArgumentException If a component the kind requires is missing.
         */
        public AiPayload build() {
            return new AiPayload(
                    this.kind, this.traceId, this.spanId, this.parentId, this.name, this.conversationId,
                    this.model, this.provider, this.input, this.output, this.tools, this.usage, this.cost,
                    this.latencyMs, this.timeToFirstTokenMs, this.error, this.stopReason, this.stream,
                    this.modelParameters, this.privacyMode, this.properties, this.profileId, this.groups,
                    this.deviceId, this.timestamp
            );
        }
    }
}
