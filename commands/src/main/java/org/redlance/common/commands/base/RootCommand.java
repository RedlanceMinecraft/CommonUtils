package org.redlance.common.commands.base;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(name = "", subcommands = { HelpCommand.class })
public final class RootCommand implements Runnable {
    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        this.spec.commandLine().usage(this.spec.commandLine().getOut());
    }
}

