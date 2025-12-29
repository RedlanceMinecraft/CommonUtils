package org.redlance.common.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redlance.common.jackson.JacksonMappers;
import org.redlance.common.requester.mojang.MojangRequester;
import org.redlance.common.requester.mojang.obj.BaseMojangProfile;
import org.redlance.common.utils.LambdaExceptionUtils;
import tools.jackson.databind.JavaType;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CompletableFutureAdapterFactoryTest {
    private static final JavaType TYPE = JacksonMappers.OBJECT_MAPPER.getTypeFactory().constructParametricType(CompletableFuture.class, BaseMojangProfile.class);

    @Test
    public void testNull() {
        CompletableFuture<BaseMojangProfile> profileCompletableFuture = CompletableFuture.supplyAsync(LambdaExceptionUtils.rethrowSupplier(() -> {
            Thread.sleep(10000);
            return MojangRequester.getBaseByName("dima_dencep");
        }));
        Assertions.assertFalse(profileCompletableFuture.isDone());

        String json = JacksonMappers.OBJECT_MAPPER.writerFor(TYPE).writeValueAsString(profileCompletableFuture);
        Assertions.assertEquals("null", json);
    }

    @Test
    public void testThrow() {
        CompletableFuture<BaseMojangProfile> profileCompletableFuture = CompletableFuture.supplyAsync(LambdaExceptionUtils.rethrowSupplier(() -> {
            throw new RuntimeException();
        }));
        Assertions.assertThrows(RuntimeException.class, profileCompletableFuture::join);

        String json = JacksonMappers.OBJECT_MAPPER.writerFor(TYPE).writeValueAsString(profileCompletableFuture);
        Assertions.assertEquals("null", json);
    }

    @Test
    public void test() throws ExecutionException, InterruptedException {
        CompletableFuture<BaseMojangProfile> profileCompletableFuture = CompletableFuture.supplyAsync(LambdaExceptionUtils.rethrowSupplier(
                () -> MojangRequester.getBaseByName("dima_dencep")
        ));
        profileCompletableFuture.join(); // Wait

        String json = JacksonMappers.OBJECT_MAPPER.writerFor(TYPE).writeValueAsString(profileCompletableFuture);

        CompletableFuture<BaseMojangProfile> deserialized = JacksonMappers.OBJECT_MAPPER.readValue(json, TYPE);
        Assertions.assertEquals(deserialized.get(), profileCompletableFuture.get());

        Assertions.assertEquals(deserialized.get().name(), profileCompletableFuture.get().name());
    }
}
