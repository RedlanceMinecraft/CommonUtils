package org.redlance.common.commands;

import org.redlance.common.commands.base.ServerConsole;
import picocli.CommandLine.Command;

@Command(name = "gc", description = "Run a full garbage collection and print heap usage")
public final class GCCommand implements Runnable {
    private static final Runtime RUNTIME = Runtime.getRuntime();

    @Override
    public void run() {
        ServerConsole.LOGGER.info("Performing full GC");

        RUNTIME.gc();

        long max = RUNTIME.maxMemory() >> 20;
        long free = RUNTIME.freeMemory() >> 20;
        long total = RUNTIME.totalMemory() >> 20;
        long used = total - free;

        ServerConsole.LOGGER.info("Heap usage: {} / {} / {} MiB", used, total, max);
    }
}
