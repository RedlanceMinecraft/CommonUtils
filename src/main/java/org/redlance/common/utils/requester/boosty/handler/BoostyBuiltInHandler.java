package org.redlance.common.utils.requester.boosty.handler;

import com.google.gson.JsonObject;
import org.redlance.common.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class BoostyBuiltInHandler implements Consumer<JsonObject> {
    private final Map<String, List<BoostyListener<?>>> listeners = new HashMap<>();

    @Override
    public void accept(JsonObject jsonObject) {
        CommonUtils.LOGGER.info("Handling {}!", jsonObject);

        String type = jsonObject.getAsJsonObject("data").get("type").getAsString();
        if (type == null) {
            return;
        }

        if (!this.listeners.containsKey(type)) {
            CommonUtils.LOGGER.warn("No listeners for {}!", type);

            return;
        }

        for (BoostyListener<?> listener : this.listeners.get(type)) {
            listener.handle(jsonObject);
        }
    }

    /**
     * @param type Event type, known: dialog_message_counters, dialog_message_read, dialog_message, standalone_notify_count, blog_subscriber_stat, upload
     */
    public void addListener(String type, BoostyListener<?> listener) {
        this.listeners.computeIfAbsent(type, key -> new ArrayList<>())
                .add(listener);
    }
}
