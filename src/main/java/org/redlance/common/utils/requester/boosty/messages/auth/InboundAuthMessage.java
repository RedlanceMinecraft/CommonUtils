package org.redlance.common.utils.requester.boosty.messages.auth;

import com.fasterxml.jackson.databind.node.ObjectNode;

public record InboundAuthMessage(int id, ErrorObject error, ObjectNode result) {
    public record ErrorObject(int code, String message) {
        @Override
        public String toString() {
            return String.format("%s: %s", code(), message());
        }
    }
}
