package org.redlance.common.utils;

import com.github.mizosoft.methanol.MediaType;
import com.sun.net.httpserver.Headers;
import io.github.kosmx.emotes.server.services.InstanceService;
import org.jetbrains.annotations.NotNull;
import org.redlance.common.CommonUtils;
import org.redlance.common.adventure.TranslatorUtils;

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

@SuppressWarnings("unused")
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

    public static Locale findLocale(Headers headers, Supplier<Locale> fallback) {
        return findLocale(headers::getFirst, fallback);
    }

    public static Locale findLocale(Function<String, String> headers, Supplier<Locale> fallback) {
        String userLocale = headers.apply("Accept-Language");
        if (userLocale != null && !userLocale.isBlank()) {
            for (Locale.LanguageRange range : Locale.LanguageRange.parse(userLocale.replace("_", "-"))) {
                Locale locale = TranslatorUtils.parseLocale(range.getRange(), () -> null);
                if (locale != null) return locale;
            }
        }

        String country = headers.apply("CF-IPCountry");
        if (country != null && !country.isBlank()) {
            String countryInLower = country.toLowerCase(Locale.ROOT);

            String lang = switch (countryInLower) {
                // Eastern European and Russian-speaking countries
                case "ru" -> "ru_ru";
                case "by" -> "be_by";
                case "kz" -> "kk_kz";
                case "ua" -> "uk_ua";
                case "rs" -> "sr_rs";
                case "bg" -> "bg_bg";
                case "ge" -> "ka_ge";

                // English-speaking countries
                case "us", "gb", "au", "nz", "ca", "sg", "za", "ie" -> "en_" + countryInLower;

                // Spanish-speaking countries
                case "es", "mx", "ar", "co", "pe", "ve", "cl", "do", "uy", "bo", "cr" -> "es_" + countryInLower;

                // Portuguese-speaking countries
                case "pt", "br", "ao", "mz" -> "pt_" + countryInLower;

                // Asian countries
                case "id" -> "id_id";
                case "vn" -> "vi_vn";
                case "jp" -> "ja_jp";
                case "kr" -> "ko_kr";
                case "cn", "tw", "hk" -> "zh_" + countryInLower;
                case "th" -> "th_th";
                case "my" -> "ms_my";
                case "in" -> "hi_in";
                case "pk" -> "ur_pk";
                case "bd" -> "bn_bd";
                case "il" -> "he_il";
                case "ph" -> "fil_ph";

                // Middle Eastern and North African countries (Arabic-speaking)
                case "ae", "eg", "tn", "sa", "ma", "dz", "lb" -> "ar_" + countryInLower;

                // European countries
                case "fr", "be", "lu", "mc" -> "fr_" + countryInLower;
                case "ch" -> "de_ch";
                case "de", "at" -> "de_" + countryInLower;
                case "it" -> "it_it";
                case "nl" -> "nl_nl";
                case "pl" -> "pl_pl";
                case "tr" -> "tr_tr";
                case "cz" -> "cs_cz";
                case "hu" -> "hu_hu";
                case "ro" -> "ro_ro";
                case "el" -> "el_gr";
                case "sk" -> "sk_sk";
                case "si" -> "sl_si";
                case "lv" -> "lv_lv";
                case "lt" -> "lt_lt";

                // Nordic countries
                case "se" -> "sv_se";
                case "no" -> "no_no";
                case "dk" -> "da_dk";
                case "fi" -> "fi_fi";
                case "is" -> "is_is";

                // Additional countries
                case "ir" -> "fa_ir";

                default -> {
                    CommonUtils.LOGGER.warn("Country code not explicitly mapped: {}, using English as fallback", country);
                    yield "en_" + countryInLower;
                }
            };

            return TranslatorUtils.parseLocale(lang, fallback);
        }

        return fallback.get();
    }

    public static boolean isTypeAccepted(String acceptHeader, @NotNull MediaType mediaType) {
        if (acceptHeader == null || acceptHeader.isBlank()) return false;
        for (String accept : acceptHeader.split(",")) {
            if (mediaType.includes(MediaType.parse(accept))) return true;
        }
        return false;
    }
}
