package org.redlance.common.utils.requester.boosty.messages.auth;

import com.google.gson.JsonElement;

public record OutboundAuthMessage(int id, int method, JsonElement params) {
    public record Auth(String name, String token) {
    }

    public record Subscribe(String channel) {
    }
}
