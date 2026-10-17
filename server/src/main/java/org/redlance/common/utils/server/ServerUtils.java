package org.redlance.common.utils.server;

import com.sun.net.httpserver.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class ServerUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("ServerUtils");

    public static InetSocketAddress getAddressFromProperties() {
        Properties prop = new Properties();

        try (BufferedReader reader = Files.newBufferedReader(Path.of("server.properties"))) {
            prop.load(reader);
        } catch (IOException e) {
            ServerUtils.LOGGER.error("Failed to load server.properties:", e);
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
        return ServerUtils.getQueryRaw(query);
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

    public static Locale findLocale(Headers headers, LocaleParser parser, Supplier<Locale> fallback) {
        return findLocale(headers::getFirst, parser, fallback);
    }

    public static Locale findLocale(Function<String, String> headers, LocaleParser parser, Supplier<Locale> fallback) {
        String userLocale = headers.apply("Accept-Language");
        if (userLocale != null && !userLocale.isBlank()) {
            List<Locale.LanguageRange> ranges;
            try {
                ranges = Locale.LanguageRange.parse(userLocale.replace("_", "-"));
            } catch (IllegalArgumentException e) {
                ranges = List.of();
            }
            for (Locale.LanguageRange range : ranges) {
                Locale locale = parser.parseLocale(range.getRange(), () -> null);
                if (locale != null) return locale;
            }
        }

        String country = headers.apply("CF-IPCountry");
        if (country != null && !country.isBlank()) {
            String countryInLower = country.toLowerCase(Locale.ROOT);
            // Cloudflare's code for a visitor it has no country for, and for one coming through Tor
            if (countryInLower.equals("xx") || countryInLower.equals("t1")) return fallback.get();

            // The language a visitor from each country gets, by its ISO 3166-1 code; English for a country not listed
            String language = switch (countryInLower) {
                case "et" -> "am";
                case "ae", "bh", "dz", "eg", "eh", "iq", "jo", "kw", "lb", "ly", "ma", "mr", "om", "ps", "qa", "sa", "sd",
                     "sy", "tn", "ye" -> "ar";
                case "az" -> "az";
                case "by" -> "be";
                case "bg" -> "bg";
                case "bd" -> "bn";
                case "ba" -> "bs";
                case "ad" -> "ca";
                case "cz" -> "cs";
                case "dk", "gl" -> "da";
                case "at", "ch", "de", "li" -> "de";
                case "cy", "gr" -> "el";
                case "ar", "bo", "cl", "co", "cr", "cu", "do", "ec", "es", "gq", "gt", "hn", "mx", "ni", "pa", "pe", "pr",
                     "py", "sv", "uy", "ve" -> "es";
                case "ee" -> "et";
                case "af", "ir" -> "fa";
                case "fi" -> "fi";
                case "ph" -> "fil";
                case "fo" -> "fo";
                case "be", "bf", "bi", "bj", "bl", "cd", "cf", "cg", "ci", "cm", "dj", "fr", "ga", "gf", "gn", "gp", "ht",
                     "km", "lu", "mc", "mf", "mg", "ml", "mq", "nc", "ne", "pf", "pm", "re", "sn", "td", "tg", "wf",
                     "yt" -> "fr";
                case "il" -> "he";
                case "in" -> "hi";
                case "hr" -> "hr";
                case "hu" -> "hu";
                case "am" -> "hy";
                case "id" -> "id";
                case "is" -> "is";
                case "it", "sm", "va" -> "it";
                case "jp" -> "ja";
                case "ge" -> "ka";
                case "kz" -> "kk";
                case "kh" -> "km";
                case "kp", "kr" -> "ko";
                case "kg" -> "ky";
                case "la" -> "lo";
                case "lt" -> "lt";
                case "lv" -> "lv";
                case "mk" -> "mk";
                case "mn" -> "mn";
                case "bn", "my" -> "ms";
                case "mt" -> "mt";
                case "mm" -> "my";
                case "np" -> "ne";
                case "aw", "bq", "cw", "nl", "sr" -> "nl";
                case "no" -> "no";
                case "pl" -> "pl";
                case "ao", "br", "cv", "gw", "mz", "pt", "st", "tl" -> "pt";
                case "md", "ro" -> "ro";
                case "ru" -> "ru";
                case "rw" -> "rw";
                case "lk" -> "si";
                case "sk" -> "sk";
                case "si" -> "sl";
                case "so" -> "so";
                case "al", "xk" -> "sq";
                case "me", "rs" -> "sr";
                case "ax", "se" -> "sv";
                case "tz" -> "sw";
                case "tj" -> "tg";
                case "th" -> "th";
                case "er" -> "ti";
                case "tm" -> "tk";
                case "tr" -> "tr";
                case "ua" -> "uk";
                case "pk" -> "ur";
                case "uz" -> "uz";
                case "vn" -> "vi";
                case "cn", "hk", "mo", "tw" -> "zh";
                default -> "en";
            };

            return parser.parseLocale(language + "_" + countryInLower, fallback);
        }

        return fallback.get();
    }
}
