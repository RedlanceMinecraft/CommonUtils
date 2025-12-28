package org.redlance.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.smile.SmileMapper;
import tools.jackson.dataformat.smile.SmileWriteFeature;

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

    private static final List<JacksonModule> JACKSON_MODULES = MapperBuilder.findModules(CommonUtils.class.getClassLoader());

    public static final ObjectMapper OBJECT_MAPPER = configureMapper(JsonMapper.builder())
            .addModules(JACKSON_MODULES)
            .build();

    public static final ObjectMapper SMILE_MAPPER = configureMapper(SmileMapper.builder())
            .addModules(JACKSON_MODULES)

            .disable(SmileWriteFeature.ENCODE_BINARY_AS_7BIT)
            .enable(SmileWriteFeature.CHECK_SHARED_STRING_VALUES)
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
                .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_EMPTY))
                .changeDefaultPropertyInclusion(incl -> incl.withContentInclusion(JsonInclude.Include.NON_EMPTY))
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .defaultPrettyPrinter(null);
    }
}
