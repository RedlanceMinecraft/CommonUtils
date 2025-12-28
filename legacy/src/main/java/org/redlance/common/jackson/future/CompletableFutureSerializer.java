package org.redlance.common.jackson.future;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("rawtypes")
public class CompletableFutureSerializer extends ValueSerializer<CompletableFuture> {
    private final ValueSerializer<Object> valueSerializer;

    public CompletableFutureSerializer() {
        this(null);
    }

    public CompletableFutureSerializer(ValueSerializer<Object> valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    @Override
    public void serialize(CompletableFuture value, JsonGenerator gen, SerializationContext ctx) throws JacksonException {
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

        ValueSerializer<Object> ser = this.valueSerializer;
        if (ser == null) {
            ser = ctx.findContentValueSerializer(inner.getClass(), null);
        }

        ser.serialize(inner, gen, ctx);
    }

    @Override
    public Class<CompletableFuture> handledType() {
        return CompletableFuture.class;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext prov, BeanProperty property) {
        if (property == null) {
            return this;
        }

        var type = property.getType();
        if (type.hasGenericTypes()) {
            var innerType = type.containedTypeOrUnknown(0);
            ValueSerializer<Object> ser = prov.findContentValueSerializer(innerType, property);
            return new CompletableFutureSerializer(ser);
        }

        return this;
    }

    @Override
    public boolean isEmpty(SerializationContext provider, CompletableFuture value) {
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

