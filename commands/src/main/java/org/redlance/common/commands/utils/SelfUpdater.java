package org.redlance.common.commands.utils;

import org.redlance.common.commands.base.ServerConsole;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public final class SelfUpdater {
    private static volatile Path pending;

    private SelfUpdater() {
    }

    public static Path currentJar() {
        try {
            Path location = Path.of(SelfUpdater.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return Files.isRegularFile(location) ? location : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static String validate(Path staged, Path currentJar) {
        String currentMain = mainClass(currentJar);
        if (currentMain == null) {
            return "failed to read the Main-Class of our own jar " + currentJar;
        }
        try (JarFile jar = new JarFile(staged.toFile())) {
            Manifest manifest = jar.getManifest();
            String main = manifest == null
                    ? null
                    : manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
            if (!currentMain.equals(main)) {
                return "not a server jar (Main-Class: " + main + ")";
            }
        } catch (IOException e) {
            return "jar is not readable (incomplete upload?): " + e.getMessage();
        }
        return null;
    }

    public static void schedule(Path staged) {
        pending = staged;
    }

    public static void applyIfScheduled() {
        Path staged = pending;
        if (staged == null) return;

        Path jar = currentJar();
        if (jar == null) {
            ServerConsole.LOGGER.error("Update scheduled, but the server was not launched from a jar — skipping");
            return;
        }

        try {
            Files.move(jar, jar.resolveSibling(jar.getFileName() + ".old"), StandardCopyOption.REPLACE_EXISTING);
            Files.move(staged, jar, StandardCopyOption.REPLACE_EXISTING);
            ServerConsole.LOGGER.info("Update applied: {}", jar);
        } catch (IOException e) {
            ServerConsole.LOGGER.error("Failed to replace jar — update not applied", e);
        }
    }

    private static String mainClass(Path jarPath) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Manifest manifest = jar.getManifest();
            return manifest == null ? null : manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
        } catch (IOException e) {
            return null;
        }
    }
}
