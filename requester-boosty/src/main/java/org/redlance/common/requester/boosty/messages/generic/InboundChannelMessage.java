package org.redlance.common.requester.boosty.messages.generic;

import tools.jackson.databind.node.ObjectNode;

/**
 * Publication in a subscribed channel.
 *
 * @param channel Channel name, e.g. {@code users#<user id>}.
 * @param data    Published event: {@code {"data": {"type": ..., "data": {...}}}}, see
 *                {@link org.redlance.common.requester.boosty.handler.BoostyBuiltInHandler}.
 */
public record InboundChannelMessage(String channel, ObjectNode data) {
}
