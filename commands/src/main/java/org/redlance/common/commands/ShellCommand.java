package org.redlance.common.commands;

import org.redlance.common.commands.base.ServerConsole;
import org.redlance.common.commands.utils.RawArgs;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Command(name = "shell", aliases = {"sh"}, description = "Run a command in the system shell (/bin/sh -c)")
public final class ShellCommand implements Runnable {
    @Parameters(
            arity = "0..*",
            paramLabel = "COMMAND",
            parameterConsumer = RawArgs.class,
            description = "Shell command and its arguments (passed verbatim)"
    )
    String[] command = new String[0];

    @Override
    public void run() {
        if (this.command.length == 0) {
            ServerConsole.LOGGER.warn("Usage: shell <command>");
            return;
        }

        String joined = String.join(" ", this.command);
        ServerConsole.LOGGER.info("Executing: {}", joined);

        try {
            Process process = new ProcessBuilder("/bin/sh", "-c", joined)
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    ServerConsole.LOGGER.info(line);
                }
            }

            int exitCode = process.waitFor();
            ServerConsole.LOGGER.info("Exit code: {}", exitCode);
        } catch (IOException e) {
            ServerConsole.LOGGER.error("Failed to execute command: {}", joined, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ServerConsole.LOGGER.error("Command interrupted: {}", joined, e);
        }
    }
}
