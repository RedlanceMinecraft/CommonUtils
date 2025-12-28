package org.redlance.common.utils.requester.boosty.messages.auth;

public record OutboundAuthMessage(int id, int method, Object params) {
    public record Auth(String name, String token) {
    }

    public record Subscribe(String channel) {
    }
}
