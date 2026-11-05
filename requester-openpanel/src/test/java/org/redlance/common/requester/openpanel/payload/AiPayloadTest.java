package org.redlance.common.requester.openpanel.payload;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AiPayloadTest {
    @Test
    void requiresModelCallFieldsOpenPanelRejectsWithout() {
        AiUsage inputOnly = new AiUsage(10);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> AiPayload.generation("trace", "gpt-5-mini", "openai", inputOnly).build());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> AiPayload.generation("trace", " ", "openai", new AiUsage(10, 5)).build());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> AiPayload.embedding("trace", "text-embedding-3-small", "openai", inputOnly)
                        .provider(null).build());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> AiPayload.embedding("trace", "text-embedding-3-small", "openai", inputOnly)
                        .usage(null).build());

        Assertions.assertDoesNotThrow(
                () -> AiPayload.generation("trace", "gpt-5-mini", "openai", new AiUsage(10, 5)).build());
        Assertions.assertDoesNotThrow(
                () -> AiPayload.embedding("trace", "text-embedding-3-small", "openai", inputOnly).build());
    }

    @Test
    void requiresOnlyTraceIdForSpansAndTraces() {
        Assertions.assertDoesNotThrow(() -> AiPayload.span("trace").build());
        Assertions.assertDoesNotThrow(() -> AiPayload.trace("trace").build());
        Assertions.assertThrows(IllegalArgumentException.class, () -> AiPayload.span(" ").build());
        Assertions.assertThrows(NullPointerException.class, () -> AiPayload.trace(null).build());
    }
}
