package org.redlance.common.commands;

import org.redlance.common.commands.utils.SelfUpdater;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

@Command(name = "update", description = "Apply a staged update (update/server.jar): validate, then swap the jar on exit")
public final class UpdateCommand implements Runnable {
    private final Path stagedJar;
    private final Runnable onRestart;

    @Option(names = {"-c", "--check"}, description = "Only validate the staged jar, without restarting")
    boolean checkOnly;

    @Spec
    CommandSpec spec;

    public UpdateCommand(Path stagedJar, Runnable onRestart) {
        this.stagedJar = stagedJar;
        this.onRestart = onRestart;
    }

    @Override
    public void run() {
        PrintWriter out = this.spec.commandLine().getOut();
        try {
            if (!validate(out)) return;
            if (this.checkOnly) {
                out.println("Validation only (--check) — restart not requested.");
                return;
            }
            SelfUpdater.schedule(this.stagedJar);
            out.println("Shutting down to update: graceful drain → jar swap → auto-restart…");
        } finally {
            out.flush();
        }
        this.onRestart.run();
    }

    private boolean validate(PrintWriter out) {
        Path current = SelfUpdater.currentJar();
        if (current == null) {
            out.println("The server was not launched from a jar (IDE?) — update unavailable.");
            return false;
        }
        if (!Files.isRegularFile(this.stagedJar)) {
            out.println("Staged jar not found: " + this.stagedJar);
            out.println("Upload the new jar there via scp while the server is running and retry.");
            return false;
        }
        String error = SelfUpdater.validate(this.stagedJar, current);
        if (error != null) {
            out.println("Staged jar failed validation: " + error);
            return false;
        }
        long size;
        try {
            size = Files.size(this.stagedJar);
        } catch (IOException e) {
            out.println("Failed to read staged jar: " + e);
            return false;
        }
        out.println("Staged jar ok: " + size + " bytes, will replace " + current.getFileName());
        return true;
    }
}
