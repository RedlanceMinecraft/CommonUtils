package org.redlance.common.utils.requester.boosty.messages.generic;

import tools.jackson.databind.node.ObjectNode;

public record InboundChannelMessage(String channel, ObjectNode data) {
}
