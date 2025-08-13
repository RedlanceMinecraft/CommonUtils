package org.redlance.common.tests;

import com.google.gson.reflect.TypeToken;
import io.github.kosmx.emotes.common.SerializableConfig;
import io.github.kosmx.emotes.server.config.Serializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redlance.common.emotecraft.EmoteCraftInstance;
import org.redlance.common.utils.LambdaExceptionUtils;
import org.redlance.common.utils.requester.mojang.MojangRequester;
import org.redlance.common.utils.requester.mojang.obj.BaseMojangProfile;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CompletableFutureAdapterFactoryTest {
    private static final Type TYPE = TypeToken.getParameterized(CompletableFuture.class, BaseMojangProfile.class).getType();

    @BeforeAll
    public static void init() {
        EmoteCraftInstance.tryInitializeInstance(SerializableConfig::new, SerializableConfig.class);
    }

    @Test
    public void testNull() {
        CompletableFuture<BaseMojangProfile> profileCompletableFuture = CompletableFuture.supplyAsync(LambdaExceptionUtils.rethrowSupplier(() -> {
            Thread.sleep(10000);
            return MojangRequester.getBaseByName("dima_dencep");
        }));
        Assertions.assertFalse(profileCompletableFuture.isDone());

        String json = Serializer.getSerializer().toJson(profileCompletableFuture, TYPE);
        Assertions.assertEquals("null", json);
    }

    @Test
    public void testThrow() {
        CompletableFuture<BaseMojangProfile> profileCompletableFuture = CompletableFuture.supplyAsync(LambdaExceptionUtils.rethrowSupplier(() -> {
            throw new RuntimeException();
        }));
        Assertions.assertThrows(RuntimeException.class, profileCompletableFuture::join);

        String json = Serializer.getSerializer().toJson(profileCompletableFuture, TYPE);
        Assertions.assertEquals("null", json);
    }

    @Test
    public void test() throws ExecutionException, InterruptedException {
        CompletableFuture<BaseMojangProfile> profileCompletableFuture = CompletableFuture.supplyAsync(LambdaExceptionUtils.rethrowSupplier(
                () -> MojangRequester.getBaseByName("dima_dencep")
        ));
        profileCompletableFuture.join(); // Wait

        String json = Serializer.getSerializer().toJson(profileCompletableFuture, TYPE);

        CompletableFuture<BaseMojangProfile> deserialized = Serializer.getSerializer().fromJson(json, TYPE);
        Assertions.assertEquals(deserialized.get(), profileCompletableFuture.get());

        Assertions.assertEquals(deserialized.get().name(), profileCompletableFuture.get().name());
    }
}
