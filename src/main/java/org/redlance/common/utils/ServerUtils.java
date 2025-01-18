package org.redlance.common.utils;

import io.github.kosmx.emotes.executor.EmoteInstance;
import org.redlance.common.CommonUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static String getQuery(String query, String key, String defaultValue) {
        return getQuery(query).getOrDefault(key, defaultValue);
    }

    public static Map<String, String> getQuery(String query) {
        return Stream.of(query.split("&"))
                .map(string -> string.split("="))
                .filter(strings -> strings.length > 1)
                .collect(Collectors.toMap(values -> values[0], values -> values[1]));
    }
}
