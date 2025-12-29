package org.redlance.common.requester.boosty.messages.auth;

import org.jetbrains.annotations.NotNull;
import tools.jackson.databind.node.ObjectNode;

public record InboundAuthMessage(int id, ErrorObject error, ObjectNode result) {
    public record ErrorObject(int code, String message) {
        @Override
        public @NotNull String toString() {
            return String.format("%s: %s", code(), message());
        }
    }
}
