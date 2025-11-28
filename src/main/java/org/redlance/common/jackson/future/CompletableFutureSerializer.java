package org.redlance.common.jackson.future;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("rawtypes")
public class CompletableFutureSerializer extends JsonSerializer<CompletableFuture> implements ContextualSerializer {
    private final JsonSerializer<Object> valueSerializer;

    public CompletableFutureSerializer() {
        this(null);
    }

    public CompletableFutureSerializer(JsonSerializer<Object> valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    @Override
    public void serialize(CompletableFuture value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        if (!value.isDone()) {
            gen.writeNull();
            return;
        }

        Object inner;
        try {
            inner = value.get();
        } catch (Exception e) {
            gen.writeNull();
            return;
        }

        if (inner == null) {
            gen.writeNull();
            return;
        }

        JsonSerializer<Object> ser = this.valueSerializer;
        if (ser == null) {
            ser = serializers.findValueSerializer(inner.getClass(), null);
        }

        ser.serialize(inner, gen, serializers);
    }

    @Override
    public Class<CompletableFuture> handledType() {
        return CompletableFuture.class;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        if (property == null) {
            return this;
        }

        var type = property.getType();
        if (type.hasGenericTypes()) {
            var innerType = type.containedTypeOrUnknown(0);
            JsonSerializer<Object> ser = prov.findValueSerializer(innerType, property);
            return new CompletableFutureSerializer(ser);
        }

        return this;
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, CompletableFuture value) {
        if (super.isEmpty(provider, value)) return true;
        if (!value.isDone()) return true;
        if (value.isCompletedExceptionally()) return true;

        try {
            return value.get() == null;
        } catch (Exception e) {
            return true;
        }
    }
}

