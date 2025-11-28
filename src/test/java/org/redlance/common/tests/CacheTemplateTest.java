package org.redlance.common.tests;

import io.github.kosmx.emotes.server.services.InstanceService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redlance.common.CommonUtils;
import org.redlance.common.utils.CacheTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class CacheTemplateTest {
    public static final Path PATH = InstanceService.INSTANCE.getGameDirectory().resolve("offline-servers-is-shit.json");

    private static CacheTemplate<String, CompletableFuture<String>> createTemplate() {
        return new CacheTemplate<>(
                PATH, CommonUtils.OBJECT_MAPPER, true,
                CommonUtils.OBJECT_MAPPER.getTypeFactory().constructType(String.class),
                CommonUtils.OBJECT_MAPPER.getTypeFactory().constructParametricType(CompletableFuture.class, String.class)
        );
    }

    @Test
    public void test() {
        CacheTemplate<String, CompletableFuture<String>> cacheTemplateFirst = createTemplate();
        cacheTemplateFirst.write("completed", CompletableFuture.completedFuture("completed"));
        cacheTemplateFirst.write("failed", CompletableFuture.failedFuture(new Throwable("failed")));
        cacheTemplateFirst.write("null", CompletableFuture.completedFuture(null));
        cacheTemplateFirst.save();

        CacheTemplate<String, CompletableFuture<String>> cacheTemplateSecond = createTemplate();
        cacheTemplateSecond.read();
        Assertions.assertEquals("completed", cacheTemplateSecond.getValueByKey("completed").join());

        Assertions.assertFalse(cacheTemplateSecond.hasKey("failed"));
        Assertions.assertNull(cacheTemplateSecond.getValueByKey("failed"));

        Assertions.assertFalse(cacheTemplateSecond.hasKey("null"));
        Assertions.assertNull(cacheTemplateSecond.getValueByKey("null"));
    }


    @AfterAll
    public static void clean() throws IOException {
        Files.deleteIfExists(PATH);
    }
}
