package org.redlance.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class CommonUtils {
    public static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual()
            .name("common-executor")
            .factory()
    );

    static {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
    }

    public static final Logger LOGGER = LogManager.getLogger("Redlance CommonUtils");

    public static void main(String... args) { // Used for testing things
    }
}
