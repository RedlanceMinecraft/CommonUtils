package org.redlance.common.utils;

import com.sun.net.httpserver.Headers;
import io.github.kosmx.emotes.server.services.InstanceService;
import net.kyori.adventure.translation.Translator;
import org.redlance.common.CommonUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServerUtils {
    public static InetSocketAddress getAddressFromProperties() {
        Properties prop = new Properties();

        try (BufferedReader reader = Files.newBufferedReader(
                InstanceService.INSTANCE.getGameDirectory().resolve("server.properties")
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

    public static Map<String, String> getQuery(String uri) {
        return ServerUtils.getQuery(URI.create(uri));
    }

    public static Map<String, String> getQuery(URI uri) {
        String query = uri.getQuery();
        if (query == null) return Collections.emptyMap();
        return ServerUtils.getQuery(uri);
    }

    public static Map<String, String> getQueryRaw(String query) {
        return Stream.of(query.split("&"))
                .map(string -> string.split("="))
                .filter(strings -> strings.length > 1)
                .collect(Collectors.toMap(values -> values[0], values -> values[1]));
    }

    public static Optional<String> getLastSegment(String uri) {
        return getLastSegment(URI.create(uri));
    }

    public static Optional<String> getLastSegment(URI uri) {
        String path = uri.getPath();
        if (path == null) return Optional.empty();
        return getLastSegmentRaw(path);
    }

    public static Optional<String> getLastSegmentRaw(String path) {
        String[] segments = path.split("/");
        if (segments.length == 0) return Optional.empty();

        return Optional.ofNullable(segments[segments.length - 1]);
    }

    public static Locale findLocale(Headers headers, Supplier<Locale> fallback) {
        return findLocale(headers::getFirst, fallback);
    }

    public static Locale findLocale(Function<String, String> headers, Supplier<Locale> fallback) {
        String userLocale = headers.apply("Accept-Language");
        if (userLocale != null && !userLocale.isBlank()) {
            for (Locale.LanguageRange range : Locale.LanguageRange.parse(userLocale.replace("_", "-"))) {
                return Objects.requireNonNullElseGet(Translator.parseLocale(
                        range.getRange().replace("-", "_")
                ), fallback);
            }
        }

        String country = headers.apply("CF-IPCountry");
        if (userLocale != null && !userLocale.isBlank()) {
            return Objects.requireNonNullElseGet(Translator.parseLocale(country), fallback);
        }

        return fallback.get();
    }
}
