package org.redlance.common.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import io.github.kosmx.emotes.common.SerializableConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redlance.common.CommonUtils;
import org.redlance.common.emotecraft.EmoteCraftInstance;
import org.redlance.common.utils.LambdaExceptionUtils;
import org.redlance.common.utils.requester.mojang.MojangRequester;
import org.redlance.common.utils.requester.mojang.obj.BaseMojangProfile;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CompletableFutureAdapterFactoryTest {
    private static final JavaType TYPE = CommonUtils.OBJECT_MAPPER.getTypeFactory().constructParametricType(CompletableFuture.class, BaseMojangProfile.class);

    @BeforeAll
    public static void init() {
        EmoteCraftInstance.tryInitializeInstance(SerializableConfig::new, SerializableConfig.class);
    }

    @Test
    public void testNull() throws JsonProcessingException {
        CompletableFuture<BaseMojangProfile> profileCompletableFuture = CompletableFuture.supplyAsync(LambdaExceptionUtils.rethrowSupplier(() -> {
            Thread.sleep(10000);
            return MojangRequester.getBaseByName("dima_dencep");
        }));
        Assertions.assertFalse(profileCompletableFuture.isDone());

        String json = CommonUtils.OBJECT_MAPPER.writerFor(TYPE).writeValueAsString(profileCompletableFuture);
        Assertions.assertEquals("null", json);
    }

    @Test
    public void testThrow() throws JsonProcessingException {
        CompletableFuture<BaseMojangProfile> profileCompletableFuture = CompletableFuture.supplyAsync(LambdaExceptionUtils.rethrowSupplier(() -> {
            throw new RuntimeException();
        }));
        Assertions.assertThrows(RuntimeException.class, profileCompletableFuture::join);

        String json = CommonUtils.OBJECT_MAPPER.writerFor(TYPE).writeValueAsString(profileCompletableFuture);
        Assertions.assertEquals("null", json);
    }

    @Test
    public void test() throws ExecutionException, InterruptedException, JsonProcessingException {
        CompletableFuture<BaseMojangProfile> profileCompletableFuture = CompletableFuture.supplyAsync(LambdaExceptionUtils.rethrowSupplier(
                () -> MojangRequester.getBaseByName("dima_dencep")
        ));
        profileCompletableFuture.join(); // Wait

        String json = CommonUtils.OBJECT_MAPPER.writerFor(TYPE).writeValueAsString(profileCompletableFuture);

        CompletableFuture<BaseMojangProfile> deserialized = CommonUtils.OBJECT_MAPPER.readValue(json, TYPE);
        Assertions.assertEquals(deserialized.get(), profileCompletableFuture.get());

        Assertions.assertEquals(deserialized.get().name(), profileCompletableFuture.get().name());
    }
}
