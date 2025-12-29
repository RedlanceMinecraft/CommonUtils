package org.redlance.common.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class CommonExecutors {
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
