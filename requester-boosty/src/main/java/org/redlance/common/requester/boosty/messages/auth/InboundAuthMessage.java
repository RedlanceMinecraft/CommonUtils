package org.redlance.common.requester.boosty.messages.auth;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.node.ObjectNode;

/**
 * Message from the pubsub websocket: a reply to a command, or a publication in a subscribed channel.
 *
 * @param id     Id of the {@link OutboundAuthMessage} replied to; {@code 0} for a publication.
 * @param error  Why the command failed.
 * @param result Reply to the command, or the publication, see
 *               {@link org.redlance.common.requester.boosty.messages.generic.InboundChannelMessage}.
 */
public record InboundAuthMessage(int id, @Nullable ErrorObject error, @Nullable ObjectNode result) {
    public record ErrorObject(int code, String message) {
        @Override
        public @NotNull String toString() {
            return String.format("%s: %s", code(), message());
        }
    }
}
