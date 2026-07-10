package org.redlance.common.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@SuppressWarnings("unused") // API
@Command(name = "stop", aliases = {"exit", "quit"}, description = "Stop the server")
public class StopCommand implements Runnable {
    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        this.spec.commandLine().getOut().println("Stopping server...");
        this.spec.commandLine().getOut().flush();
        System.exit(0);
    }
}
