package org.redlance.common.requester.boosty.messages.auth;

/**
 * Command sent to the pubsub websocket, see {@link org.redlance.common.requester.boosty.BoostyWebsocketListener}.
 *
 * @param id     Command id, repeated in the reply ({@link InboundAuthMessage#id()}).
 * @param method 0 - connect, 1 - channel subscribe, 2 - channel unsubscribe, 7 - ping.
 * @param params Payload: {@link Auth} to connect, {@link Subscribe} to subscribe, {@code null} to ping.
 */
public record OutboundAuthMessage(int id, int method, Object params) {
    /**
     * Payload of the connect command.
     *
     * @param name  Client name.
     * @param token Socket token, see {@link org.redlance.common.requester.boosty.BoostyRequester#requestSocketToken(String)}.
     */
    public record Auth(String name, String token) {
    }

    /**
     * Payload of the subscribe command.
     *
     * @param channel Channel name, e.g. {@code users#<user id>}.
     */
    public record Subscribe(String channel) {
    }
}
