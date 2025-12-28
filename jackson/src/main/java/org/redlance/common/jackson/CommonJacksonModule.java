package org.redlance.common.jackson;

import org.redlance.common.jackson.future.CompletableFutureDeserializer;
import org.redlance.common.jackson.future.CompletableFutureSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.util.concurrent.CompletableFuture;

public class CommonJacksonModule extends SimpleModule {
    public CommonJacksonModule() {
        super("CommonUtils Jackson Module");

        // CompletableFuture
        addSerializer(CompletableFuture.class, new CompletableFutureSerializer());
        addDeserializer(CompletableFuture.class, new CompletableFutureDeserializer());
    }
}
