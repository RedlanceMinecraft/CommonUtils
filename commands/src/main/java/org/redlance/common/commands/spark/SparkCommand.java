package org.redlance.common.commands.spark;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.platform.PlatformInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.redlance.common.commands.base.ServerConsole;
import org.redlance.common.commands.utils.RawArgs;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.stream.Stream;

@CommandLine.Command(name = "spark")
public final class SparkCommand implements Runnable, SparkPlugin, PlatformInfo {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(
            Thread.ofPlatform().daemon().name("spark-", 0).factory()
    );

    private final SparkPlatform platform = new SparkPlatform(this);
    private final OnlineEmotesCommandSender sender = new OnlineEmotesCommandSender();
    private final Path profilerDir;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @CommandLine.Parameters(
            arity = "0..*",
            paramLabel = "ARGS",
            parameterConsumer = RawArgs.class
    )
    String[] args = new String[0];

    private boolean enabled;

    public SparkCommand(Path profilerDir) {
        this.profilerDir = profilerDir;
    }

    @Override
    public void run() {
        if (!this.enabled) {
            this.platform.enable();
            this.enabled = true;
        }

        this.platform.executeCommand(this.sender, this.args);
    }

    @Override
    public Path getPluginDirectory() {
        return this.profilerDir;
    }

    @Override
    public String getCommandName() {
        return this.spec.name();
    }

    @Override
    public Stream<? extends CommandSender> getCommandSenders() {
        return Stream.of(this.sender);
    }

    @Override
    public void executeAsync(Runnable runnable) {
        EXECUTOR.submit(runnable);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return this;
    }

    @Override
    public void log(Level level, String s) {
        ServerConsole.LOGGER.info(s);
    }

    @Override
    public void log(Level level, String s, Throwable throwable) {
        ServerConsole.LOGGER.error(s, throwable);
    }

    @Override
    public Type getType() {
        return Type.SERVER;
    }

    @Override
    public String getName() {
        return "CommonUtils";
    }

    @Override
    public String getBrand() {
        return "RedlanceMinecraft";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getMinecraftVersion() {
        return "1.21.10";
    }

    public class OnlineEmotesCommandSender implements CommandSender {
        public static final UUID SENDER_ID = UUID.randomUUID();

        private OnlineEmotesCommandSender() {
        }

        @Override
        public String getName() {
            return SparkCommand.this.getName();
        }

        @Override
        public UUID getUniqueId() {
            return SENDER_ID;
        }

        @Override
        public void sendMessage(Component component) {
            ServerConsole.LOGGER.info(LegacyComponentSerializer.legacyAmpersand().serialize(component)
                    .replaceAll("&.", "")
            );
        }

        @Override
        public boolean hasPermission(String s) {
            return true;
        }
    }
}
