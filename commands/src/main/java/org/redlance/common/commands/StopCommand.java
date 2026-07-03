package org.redlance.common.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(name = "stop", aliases = {"exit", "quit"})
public final class StopCommand implements Runnable {
    private final Runnable onStop;

    @Spec
    CommandSpec spec;

    public StopCommand(Runnable onStop) {
        this.onStop = onStop;
    }

    @Override
    public void run() {
        this.spec.commandLine().getOut().println("Stopping server...");
        this.spec.commandLine().getOut().flush();
        this.onStop.run();
    }
}
