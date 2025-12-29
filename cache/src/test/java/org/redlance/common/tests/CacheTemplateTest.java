package org.redlance.common.tests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redlance.common.cache.CacheTemplate;
import org.redlance.common.jackson.JacksonMappers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class CacheTemplateTest {
    public static final Path PATH = Path.of("offline-servers-is-shit.json");

    private static CacheTemplate<String, CompletableFuture<String>> createTemplate() {
        return new CacheTemplate<>(
                PATH, JacksonMappers.OBJECT_MAPPER, true,
                JacksonMappers.OBJECT_MAPPER.getTypeFactory().constructType(String.class),
                JacksonMappers.OBJECT_MAPPER.getTypeFactory().constructParametricType(CompletableFuture.class, String.class)
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
