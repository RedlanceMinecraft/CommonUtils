package org.redlance.common.emotecraft.serializer;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;

public class CompletableFutureAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public <R> TypeAdapter<R> create(Gson gson, TypeToken<R> typeToken) {
        Type type = typeToken.getType();
        Class<?> rawType = typeToken.getRawType();

        if (!CompletableFuture.class.isAssignableFrom(rawType)) {
            return null;
        }

        if (!(type instanceof ParameterizedType)) {
            return null;
        }

        Type innerType = ((ParameterizedType) type).getActualTypeArguments()[0];
        TypeAdapter<?> delegateAdapter = gson.getAdapter(TypeToken.get(innerType));

        return new CompletableFutureAdapter(delegateAdapter);
    }

    private static class CompletableFutureAdapter<T> extends TypeAdapter<CompletableFuture<T>> {
        private final TypeAdapter<T> delegate;

        public CompletableFutureAdapter(TypeAdapter<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(JsonWriter out, CompletableFuture<T> value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }

            if (!value.isDone()) {
                out.nullValue();
                return;
            }

            try {
                delegate.write(out, value.get());
            } catch (Exception e) {
                out.nullValue();
            }
        }

        @Override
        public CompletableFuture<T> read(JsonReader in) throws IOException {
            T val = delegate.read(in);
            return CompletableFuture.completedFuture(val);
        }
    }
}
