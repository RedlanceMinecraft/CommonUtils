package org.redlance.common.utils.requester.boosty.messages.auth;

import com.google.gson.JsonObject;

public record InboundAuthMessage(int id, ErrorObject error, JsonObject result) {
    public record ErrorObject(int code, String message) {
        @Override
        public String toString() {
            return String.format("%s: %s", code(), message());
        }
    }
}
