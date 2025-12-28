package org.redlance.common.jackson.future;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("rawtypes")
public class CompletableFutureDeserializer extends ValueDeserializer<CompletableFuture> {
    private final JavaType valueType;
    private final ValueDeserializer<Object> valueDeserializer;

    public CompletableFutureDeserializer() {
        this(null, null);
    }

    public CompletableFutureDeserializer(JavaType valueType, ValueDeserializer<Object> valueDeserializer) {
        this.valueType = valueType;
        this.valueDeserializer = valueDeserializer;
    }

    @Override
    public CompletableFuture deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        ValueDeserializer<Object> deser = this.valueDeserializer;
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
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        JavaType type = (property != null) ? property.getType() : ctxt.getContextualType();

        if (type != null && type.hasGenericTypes()) {
            JavaType innerType = type.containedTypeOrUnknown(0);
            ValueDeserializer<Object> deser = ctxt.findContextualValueDeserializer(innerType, property);
            return new CompletableFutureDeserializer(innerType, deser);
        }

        return this;
    }
}
