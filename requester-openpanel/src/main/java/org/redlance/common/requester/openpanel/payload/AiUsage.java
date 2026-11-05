package org.redlance.common.requester.openpanel.payload;

import org.jetbrains.annotations.Nullable;

/**
 * Token counts of an {@link AiPayload}, from which OpenPanel computes its cost.
 *
 * @param inputTokens      Tokens the model read, cached ones included; required for a generation and an embedding.
 * @param outputTokens     Tokens the model wrote, reasoning ones included; required for a generation.
 * @param cacheReadTokens  Input tokens read from the provider's prompt cache.
 * @param cacheWriteTokens Input tokens written to the provider's prompt cache.
 * @param reasoningTokens  Output tokens spent on reasoning.
 */
public record AiUsage(
        @Nullable Long inputTokens,
        @Nullable Long outputTokens,
        @Nullable Long cacheReadTokens,
        @Nullable Long cacheWriteTokens,
        @Nullable Long reasoningTokens
) {
    /**
     * Usage of a generation without the cache and reasoning breakdown.
     */
    public AiUsage(long inputTokens, long outputTokens) {
        this(inputTokens, outputTokens, null, null, null);
    }

    /**
     * Usage of an embedding.
     */
    public AiUsage(long inputTokens) {
        this(inputTokens, null, null, null, null);
    }
}
