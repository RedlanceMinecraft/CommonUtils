package org.redlance.common.utils.requester.boosty.messages.generic;

import com.google.gson.JsonObject;

public record InboundChannelMessage(String channel, JsonObject data) {
}
