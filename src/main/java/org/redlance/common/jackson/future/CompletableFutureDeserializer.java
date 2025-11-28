package org.redlance.common.jackson.future;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("rawtypes")
public class CompletableFutureDeserializer extends JsonDeserializer<CompletableFuture> implements ContextualDeserializer {
    private final JavaType valueType;
    private final JsonDeserializer<Object> valueDeserializer;

    public CompletableFutureDeserializer() {
        this(null, null);
    }

    public CompletableFutureDeserializer(JavaType valueType, JsonDeserializer<Object> valueDeserializer) {
        this.valueType = valueType;
        this.valueDeserializer = valueDeserializer;
    }

    @Override
    public CompletableFuture deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonDeserializer<Object> deser = this.valueDeserializer;
        JavaType vt = this.valueType;

        if (deser == null) {
            if (vt == null) {
                JavaType contextualType = ctxt.getContextualType();
                if (contextualType != null && contextualType.hasGenericTypes()) {
                    vt = contextualType.containedTypeOrUnknown(0);
                }
            }
            if (vt != null) {
                deser = ctxt.findContextualValueDeserializer(vt, null);
            } else {
                Object val = ctxt.readValue(p, Object.class);
                return CompletableFuture.completedFuture(val);
            }
        }

        Object value = deser.deserialize(p, ctxt);
        return CompletableFuture.completedFuture(value);
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        JavaType type = (property != null) ? property.getType() : ctxt.getContextualType();

        if (type != null && type.hasGenericTypes()) {
            JavaType innerType = type.containedTypeOrUnknown(0);
            JsonDeserializer<Object> deser = ctxt.findContextualValueDeserializer(innerType, property);
            return new CompletableFutureDeserializer(innerType, deser);
        }

        return this;
    }
}
