package org.redlance.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class CommonUtils {
    static {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
    }

    public static final Logger LOGGER = LogManager.getLogger("Redlance CommonUtils");

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
}
