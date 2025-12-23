package org.redlance.common.utils.requester.boosty.messages.auth;

import org.jspecify.annotations.NonNull;
import tools.jackson.databind.node.ObjectNode;

public record InboundAuthMessage(int id, ErrorObject error, ObjectNode result) {
    public record ErrorObject(int code, String message) {
        @Override
        public @NonNull String toString() {
            return String.format("%s: %s", code(), message());
        }
    }
}
