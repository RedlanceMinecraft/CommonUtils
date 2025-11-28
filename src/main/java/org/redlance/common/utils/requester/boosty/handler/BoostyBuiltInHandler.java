package org.redlance.common.utils.requester.boosty.handler;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.redlance.common.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class BoostyBuiltInHandler implements BiConsumer<String, ObjectNode> {
    private final Map<String, List<BoostyListener<?>>> listeners = new HashMap<>();

    @Override
    public void accept(String channel, ObjectNode jsonObject) {
        CommonUtils.LOGGER.info("Handling {} from channel {}!", jsonObject, channel);

        String type = jsonObject.get("data").get("type").textValue();
        if (type == null) {
            return;
        }

        List<BoostyListener<?>> listenerList = this.listeners.getOrDefault(type, this.listeners.get(""));
        if (listenerList == null) {
            CommonUtils.LOGGER.warn("No listeners for {} in channel {}!", type, channel);

            return;
        }

        for (BoostyListener<?> listener : listenerList) {
            listener.handle(channel, jsonObject);
        }
    }

    public boolean addListener(BoostyListener<?> listener) {
        return addListener("", listener);
    }

    /**
     * @param type Event type, known: dialog_message_counters, dialog_message_read, dialog_message, standalone_notify_count, blog_subscriber_stat, upload
     */
    public boolean addListener(String type, BoostyListener<?> listener) {
        CommonUtils.LOGGER.debug("Subscribed listener ({}) for type '{}'!",
                listener.getName(), type
        );

        return this.listeners.computeIfAbsent(type, key -> new ArrayList<>())
                .add(listener);
    }
}
