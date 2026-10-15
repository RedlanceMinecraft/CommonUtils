package org.redlance.common.requester.boosty.obj;

import org.jetbrains.annotations.Nullable;
import org.redlance.common.requester.RequesterUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reads a value nothing depends on, leaving it out when Boosty sends a shape this wrapper does not expect, so that it
 * does not fail the whole response. A list or a map loses only the elements that do not fit.
 * <p>
 * Apply it to such a property: {@code @JsonDeserialize(using = LenientDeserializer.class)}.
 */
public class LenientDeserializer extends ValueDeserializer<Object> {
    private final @Nullable JavaType type;

    public LenientDeserializer() {
        this(null);
    }

    private LenientDeserializer(@Nullable JavaType type) {
        this.type = type;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        return new LenientDeserializer(property != null ? property.getType() : ctxt.getContextualType());
    }

    @Override
    public @Nullable Object deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JavaType type = Objects.requireNonNull(this.type, "LenientDeserializer was not contextualized");
        JsonNode node = ctxt.readTree(p);

        if (type.isCollectionLikeType() && node.isArray()) {
            List<Object> values = new ArrayList<>();
            for (JsonNode element : node) {
                Object value = read(ctxt, element, type.getContentType());
                if (value != null) {
                    values.add(value);
                }
            }

            return values;
        }

        if (type.isMapLikeType() && node instanceof ObjectNode object) {
            Map<Object, Object> values = new LinkedHashMap<>();
            for (Map.Entry<String, JsonNode> entry : object.properties()) {
                // Read one entry at a time, so that its key goes through the map's own key deserializer
                ObjectNode single = JsonNodeFactory.instance.objectNode().set(entry.getKey(), entry.getValue());
                if (read(ctxt, single, type) instanceof Map<?, ?> value) {
                    values.putAll(value);
                }
            }

            return values;
        }

        return read(ctxt, node, type);
    }

    private static @Nullable Object read(DeserializationContext ctxt, JsonNode node, JavaType type) {
        try {
            return ctxt.readTreeAsValue(node, type);
        } catch (JacksonException e) {
            RequesterUtils.LOGGER.debug("Left out {} that does not fit {}: {}", node, type, e.getOriginalMessage());
            return null;
        }
    }
}
