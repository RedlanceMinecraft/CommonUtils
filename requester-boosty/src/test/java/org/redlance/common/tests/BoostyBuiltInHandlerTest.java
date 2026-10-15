package org.redlance.common.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redlance.common.jackson.JacksonMappers;
import org.redlance.common.requester.boosty.handler.BoostyBuiltInHandler;
import org.redlance.common.requester.boosty.handler.BoostyListener;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BoostyBuiltInHandlerTest {
    private final BoostyBuiltInHandler handler = new BoostyBuiltInHandler();

    @Test
    public void testEventsWithoutType() {
        Recording listener = new Recording();
        this.handler.addListener(listener);

        for (String event : List.of("{}", "{\"data\": {}}", "{\"data\": {\"type\": null}}", "{\"data\": {\"type\": 5}}")) {
            Assertions.assertDoesNotThrow(() -> this.handler.onEvent("users#1", event(event)), event);
        }

        Assertions.assertEquals(List.of(), listener.types);
    }

    @Test
    public void testFailingListener() {
        Recording next = new Recording();
        this.handler.addListener("dialog_message", new Failing());
        this.handler.addListener("dialog_message", next);

        this.handler.onEvent("dialogs#1", event("{\"data\": {\"type\": \"dialog_message\"}}"));

        Assertions.assertEquals(List.of("dialog_message"), next.types);
    }

    @Test
    public void testListenersWithoutType() {
        Recording typed = new Recording();
        Recording untyped = new Recording();
        this.handler.addListener("upload", typed);
        this.handler.addListener(untyped);

        this.handler.onEvent("users#1", event("{\"data\": {\"type\": \"upload\"}}"));
        this.handler.onEvent("users#1", event("{\"data\": {\"type\": \"blog_subscriber_stat\"}}"));

        Assertions.assertEquals(List.of("upload"), typed.types);
        Assertions.assertEquals(List.of("blog_subscriber_stat"), untyped.types);
    }

    private static ObjectNode event(String json) {
        return (ObjectNode) JacksonMappers.OBJECT_MAPPER.readTree(json);
    }

    private static final class Recording implements BoostyListener<ObjectNode> {
        private final List<String> types = new CopyOnWriteArrayList<>();

        @Override
        public void handle(String channel, ObjectNode object) {
            this.types.add(object.path("data").path("type").asString());
        }

        @Override
        public Class<ObjectNode> parseObject() {
            return ObjectNode.class;
        }
    }

    private static final class Failing implements BoostyListener<ObjectNode> {
        @Override
        public void handle(String channel, ObjectNode object) {
            throw new IllegalStateException("Listener failure");
        }

        @Override
        public Class<ObjectNode> parseObject() {
            return ObjectNode.class;
        }
    }
}
