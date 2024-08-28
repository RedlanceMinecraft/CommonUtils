package org.redlance.common.utils.requester.boosty.handler;

import com.google.gson.JsonObject;
import org.redlance.common.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class BoostyBuiltInHandler implements BiConsumer<String, JsonObject> {
    private final Map<String, List<BoostyListener<?>>> listeners = new HashMap<>();

    @Override
    public void accept(String channel, JsonObject jsonObject) {
        CommonUtils.LOGGER.info("Handling {}!", jsonObject);

        String type = jsonObject.getAsJsonObject("data").get("type").getAsString();
        if (type == null) {
            return;
        }

        List<BoostyListener<?>> listenerList = this.listeners.getOrDefault(type, this.listeners.get(""));
        if (listenerList == null) {
            CommonUtils.LOGGER.warn("No listeners for {}!", type);

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
