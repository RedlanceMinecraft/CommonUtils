package org.redlance.common.utils;

import io.github.kosmx.emotes.executor.EmoteInstance;
import org.redlance.common.CommonUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Properties;

public class ServerUtils {
    public static InetSocketAddress getAddressFromProperties() {
        Properties prop = new Properties();

        try (BufferedReader reader = Files.newBufferedReader(
                EmoteInstance.instance.getGameDirectory().resolve("server.properties")
        )) {
            prop.load(reader);
        } catch (IOException e) {
            CommonUtils.LOGGER.error("Failed to load server.properties:", e);
        }

        return new InetSocketAddress(
                prop.getProperty("server-ip", "0.0.0.0"),
                Integer.parseInt(prop.getProperty("server-port", "25566"))
        );
    }
}
