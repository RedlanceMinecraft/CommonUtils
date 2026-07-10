package org.redlance.common.commands.base;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.redlance.common.commands.GCCommand;
import org.redlance.common.commands.ShellCommand;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;
import picocli.shell.jline3.PicocliCommands.PicocliCommandsFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.function.Supplier;

public final class ServerConsole {
    public static final Logger LOGGER = LogManager.getLogger("common-commands");

    private final PicocliCommandsFactory factory = new PicocliCommandsFactory();

    private final CommandLine root;
    private final Supplier<Path> workDir;
    public final String name;
    private volatile boolean running;
    private Terminal terminal;

    public ServerConsole(Supplier<Path> workDir, String name) {
        this.root = new CommandLine(new RootCommand(), this.factory);
        this.workDir = workDir;
        this.name = name;

        register("shell", new ShellCommand());
        register("gc", new GCCommand());
    }

    public ServerConsole register(String name, Object command) {
        this.root.addSubcommand(name, command);
        return this;
    }

    public void start() {
        Thread thread = new Thread(this::run, "server-console");
        thread.setDaemon(true);
        thread.start();
    }

    @SuppressWarnings("deprecation")
    private void run() {
        DefaultParser parser = new DefaultParser();
        try {
            this.terminal = TerminalBuilder.builder().name(this.name).dumb(true).build();
            this.factory.setTerminal(this.terminal);

            PicocliCommands picocliCommands = new PicocliCommands(this.root);
            SystemRegistry registry = new SystemRegistryImpl(parser, this.terminal, this.workDir, null);
            registry.setCommandRegistries(picocliCommands);

            applyStreams(this.root,
                    IoBuilder.forLogger(ServerConsole.LOGGER).setLevel(Level.INFO).buildPrintWriter(),
                    IoBuilder.forLogger(ServerConsole.LOGGER).setLevel(Level.ERROR).buildPrintWriter()
            );

            LineReader reader = LineReaderBuilder.builder()
                    .terminal(this.terminal)
                    .completer(registry.completer())
                    .parser(parser)
                    .variable(LineReader.LIST_MAX, 50)
                    .build();

            this.running = true;
            while (this.running) {
                try {
                    registry.cleanUp();
                    registry.execute(reader.readLine());
                } catch (UserInterruptException ignored) {
                } catch (EndOfFileException e) {
                    break;
                } catch (Exception e) {
                    registry.trace(e);
                }
            }
        } catch (IOException e) {
            ServerConsole.LOGGER.error("Failed to create console!", e);
        } finally {
            close();
        }
    }

    private void close() {
        this.running = false;
        if (this.terminal != null) {
            try {
                this.terminal.close();
            } catch (IOException ignored) {}
        }
    }

    private static void applyStreams(CommandLine cmd, PrintWriter out, PrintWriter err) {
        cmd.setOut(out);
        cmd.setErr(err);

        for (CommandLine sub : cmd.getSubcommands().values()) {
            applyStreams(sub, out, err);
        }
    }
}

