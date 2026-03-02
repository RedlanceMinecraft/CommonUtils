package org.redlance.common.services;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ServiceUtils {
    private static final Comparator<AdvancedService> COMPARATOR = Comparator.comparingInt(AdvancedService::getPriority);
    private static final boolean DEBUG_SERVICES = Boolean.getBoolean("commonutils.debug.services");

    public static final int DEFAULT_PRIORITY = 0;
    public static final int HIGHEST_PRIORITY = 1000;
    public static final int LOWEST_PRIORITY = -1000;

    public static <T extends AdvancedService> Stream<T> loadServices(Class<T> serviceClass) {
        ModuleLayer layer = serviceClass.getModule().getLayer(); // NeoForge compat?

        ServiceLoader<T> loader = layer == null ? ServiceLoader.load(serviceClass,
                serviceClass.getClassLoader()
        ) : ServiceLoader.load(layer, serviceClass);

        Iterator<T> it = loader.iterator();
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, 0) {
            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                while (true) {
                    try {
                        if (!it.hasNext()) return false;
                        action.accept(it.next());
                        return true;
                    } catch (ServiceConfigurationError | NoClassDefFoundError ex) {
                        if (DEBUG_SERVICES) ex.printStackTrace();
                    }
                }
            }
        }, false).filter(AdvancedService::isActive);
    }

    public static <T extends AdvancedService> Stream<T> loadServicesSorted(Class<T> serviceClass) {
        return ServiceUtils.loadServices(serviceClass).sorted(COMPARATOR.reversed());
    }

    public static <T extends AdvancedService> T loadService(Class<T> serviceClass, Supplier<? extends T> defaultService) {
        return ServiceUtils.loadOptionalService(serviceClass).orElseGet(defaultService);
    }

    public static <T extends AdvancedService> T loadService(Class<T> serviceClass) {
        return ServiceUtils.loadOptionalService(serviceClass).orElseThrow();
    }

    public static <T extends AdvancedService> Optional<T> loadOptionalService(Class<T> serviceClass) {
        return ServiceUtils.loadServices(serviceClass).max(COMPARATOR);
    }
}
