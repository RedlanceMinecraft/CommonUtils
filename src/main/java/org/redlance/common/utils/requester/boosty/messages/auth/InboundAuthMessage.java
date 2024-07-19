package org.redlance.common.utils.requester.boosty.messages.auth;

import com.google.gson.JsonObject;

public record InboundAuthMessage(int id, JsonObject result) {
}
