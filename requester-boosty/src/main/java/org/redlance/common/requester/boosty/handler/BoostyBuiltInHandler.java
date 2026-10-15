package org.redlance.common.requester.boosty.handler;

import org.redlance.common.requester.RequesterUtils;
import org.redlance.common.requester.boosty.BoostyWebsocketListener;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hands websocket events to listeners by their type, {@code {"data": {"type": ...}}}.
 * <p>
 * An event goes to the listeners added for its type or, when its type has none, to the listeners added without one.
 * Listeners may be added while events arrive.
 */
public class BoostyBuiltInHandler implements BoostyWebsocketListener.Listener {
    private final Map<String, List<BoostyListener<?>>> listeners = new ConcurrentHashMap<>();

    @Override
    public void onEvent(String channel, ObjectNode jsonObject) {
        RequesterUtils.LOGGER.info("Handling {} from channel {}!", jsonObject, channel);

        String type = jsonObject.path("data").path("type").stringValue(null);
        if (type == null) {
            RequesterUtils.LOGGER.debug("No event type in channel {}!", channel);
            return;
        }

        List<BoostyListener<?>> listenerList = this.listeners.getOrDefault(type, this.listeners.get(""));
        if (listenerList == null) {
            RequesterUtils.LOGGER.warn("No listeners for {} in channel {}!", type, channel);

            return;
        }

        for (BoostyListener<?> listener : listenerList) {
            try {
                listener.handle(channel, jsonObject);
            } catch (Throwable th) {
                RequesterUtils.LOGGER.error("Listener ({}) failed to handle {} in channel {}!",
                        listener.getName(), type, channel, th
                );
            }
        }
    }

    /**
     * Adds a listener for the events no listener was added for by type.
     */
    public boolean addListener(BoostyListener<?> listener) {
        return addListener("", listener);
    }

    /**
     * @param type Event type, known: dialog_message_counters, dialog_message_read, dialog_message, standalone_notify_count, blog_subscriber_stat, upload
     */
    public boolean addListener(String type, BoostyListener<?> listener) {
        RequesterUtils.LOGGER.debug("Subscribed listener ({}) for type '{}'!",
                listener.getName(), type
        );

        return this.listeners.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>())
                .add(listener);
    }
}
