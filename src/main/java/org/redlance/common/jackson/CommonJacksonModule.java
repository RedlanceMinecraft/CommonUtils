package org.redlance.common.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.zigythebird.playeranimcore.animation.Animation;
import org.redlance.common.jackson.animation.FastAnimationDeserializer;
import org.redlance.common.jackson.animation.FastAnimationSerializer;
import org.redlance.common.jackson.future.CompletableFutureDeserializer;
import org.redlance.common.jackson.future.CompletableFutureSerializer;

import java.util.concurrent.CompletableFuture;

public class CommonJacksonModule extends SimpleModule {
    public CommonJacksonModule() {
        super("CommonUtils Jackson Module");

        // CompletableFuture
        addSerializer(CompletableFuture.class, new CompletableFutureSerializer());
        addDeserializer(CompletableFuture.class, new CompletableFutureDeserializer());

        // Animation
        addSerializer(Animation.class, FastAnimationSerializer.INSTANCE);
        addDeserializer(Animation.class, FastAnimationDeserializer.INSTANCE);
    }
}
