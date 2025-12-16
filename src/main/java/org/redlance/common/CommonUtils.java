package org.redlance.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class CommonUtils {
    static {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
    }

    public static final Logger LOGGER = LoggerFactory.getLogger("Redlance CommonUtils");

    private static final List<Module> JACKSON_MODULES = ObjectMapper.findModules(CommonUtils.class.getClassLoader());

    public static final ObjectMapper OBJECT_MAPPER = configureMapper(JsonMapper.builder())
            .addModules(JACKSON_MODULES)
            .build();

    public static final ObjectMapper SMILE_MAPPER = configureMapper(SmileMapper.builder())
            .addModules(JACKSON_MODULES)

            .disable(SmileGenerator.Feature.ENCODE_BINARY_AS_7BIT)
            .enable(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES)
            .build();

    @SuppressWarnings("unused")
    public static void main(String... args) { // Used for testing things
    }

    public static ScheduledExecutorService createScheduledExecutor(int corePoolSize, String prefix) {
        return Executors.newScheduledThreadPool(corePoolSize, Thread.ofVirtual()
                .name(prefix, 0)
                .factory()
        );
    }

    public static ExecutorService createExecutor(String prefix) {
        return Executors.newCachedThreadPool(Thread.ofVirtual()
                .name(prefix, 0)
                .factory()
        );
    }

    public static JavaType constructType(Type type) {
        return CommonUtils.OBJECT_MAPPER.constructType(type);
    }

    public static <T extends MapperBuilder<?, T>> T configureMapper(T mapper) {
        return mapper
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .defaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, JsonInclude.Include.NON_EMPTY))
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .defaultPrettyPrinter(null);
    }
}
