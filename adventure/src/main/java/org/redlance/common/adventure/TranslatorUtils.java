package org.redlance.common.adventure;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;
import net.kyori.adventure.translation.Translator;
import org.redlance.common.utils.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class TranslatorUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("TranslatorUtils");

    /**
     * Related locales: a locale listed under a key reads the key's language for what its own language lacks, and a
     * locale of the key's own language reads the key before the other files of that language. See
     * {@link #createTranslationStore(Key, Locale, Class, String)}.
     */
    public static final Map<String, List<String>> LOCALE_FALLBACK = Map.ofEntries(
            // Eastern European and Russian-speaking countries
            Map.entry("ru_ru", List.of("tt_ru", "uk_ua", "kk_kz", "be_by", "ba_ru", "ky_kg", "tg_tj", "tk_tm", "uz_uz")),

            // Spanish-speaking countries
            Map.entry("es_es", List.of("es_419", "es_mx", "es_ar", "es_co", "es_pe", "es_ve", "es_cl")),

            // German-speaking countries
            Map.entry("de_de", List.of("de_at", "de_ch")),

            // French-speaking countries
            Map.entry("fr_fr", List.of("fr_ca", "fr_be", "fr_ch")),

            // Portuguese-speaking countries - only keep pt_pt as key
            Map.entry("pt_pt", List.of("pt_br")),

            // Other European languages
            Map.entry("it_it", List.of("it_ch")),
            Map.entry("nl_nl", List.of("nl_be")),
            Map.entry("pl_pl", List.of("cs_cz")),
            Map.entry("tr_tr", List.of("az_az")),

            // Asian languages
            Map.entry("zh_cn", List.of("zh_tw")),
            Map.entry("zh_tw", List.of("zh_hk")),
            Map.entry("id_id", List.of("ms_my")),

            // Nordic languages
            Map.entry("sv_se", List.of("no_no", "da_dk")),
            Map.entry("fi_fi", List.of("et_ee"))
    );
    public static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("en-us");

    // LOCALE_FALLBACK parsed, its keys in alphabetical order so that no result depends on the order of a map
    private static final Map<Locale, List<Locale>> FALLBACKS = new TreeMap<>(LOCALE_FALLBACK).entrySet().stream().collect(Collectors.toMap(
            fallback -> Translator.parseLocale(fallback.getKey()),
            fallback -> fallback.getValue().stream().map(Translator::parseLocale).toList(),
            (first, second) -> first,
            LinkedHashMap::new
    ));

    public static TranslationStore.StringBased<MessageFormat> createTranslationStore(Key name, Class<?> target, String knownResource) {
        return createTranslationStore(name, DEFAULT_LOCALE, target, knownResource);
    }

    /**
     * Loads the {@code messages} directory next to {@code knownResource}: a {@code .properties} file per locale, named
     * after it the Minecraft way ({@code ru_ru}) or by language alone ({@code ru}), with {@code main} standing for
     * {@code defaultLocale}. Of two files naming the same locale only the first by name is loaded.
     * <p>
     * A locale reads each key from the first of these files that has it: its own; one {@link #LOCALE_FALLBACK} lists it
     * under in its own language; the files of its language; the files of every other language {@link #LOCALE_FALLBACK}
     * lists it under. The files of a language go in this order: the one named by the language alone, the one of
     * {@code defaultLocale}, the ones {@link #LOCALE_FALLBACK} lists other locales under, then the rest by name. A key none
     * of them has is read from {@code defaultLocale}.
     */
    public static TranslationStore.StringBased<MessageFormat> createTranslationStore(Key name, Locale defaultLocale, Class<?> target, String knownResource) {
        final TranslationStore.StringBased<MessageFormat> translationStore = TranslationStore.messageFormat(name);
        translationStore.defaultLocale(defaultLocale);

        try {
            ResourceUtils.visitResources(knownResource, target, path -> {
                TranslatorUtils.LOGGER.info("Loading localizations...");
                registerLocalizations(translationStore, defaultLocale, path);
            }, "messages");
        } catch (IOException e) {
            TranslatorUtils.LOGGER.error("Encountered an I/O error whilst loading translations", e);
        }

        return translationStore;
    }

    static void registerLocalizations(TranslationStore.StringBased<MessageFormat> translationStore, Locale defaultLocale, Path directory) {
        final Map<Locale, ResourceBundle> files = readLocalizations(defaultLocale, directory);

        // Every locale a lookup can stop at: a file's, its language, one LOCALE_FALLBACK lists and the default,
        // each registered once with everything its files hold, so no translation is ever registered twice
        final Set<Locale> locales = new LinkedHashSet<>(files.keySet());
        files.keySet().forEach(locale -> locales.add(Locale.of(locale.getLanguage())));
        FALLBACKS.values().forEach(locales::addAll);
        locales.add(defaultLocale);

        for (final Locale locale : locales) {
            final Map<String, String> translations = new HashMap<>();
            for (final Locale source : sources(locale, defaultLocale, files.keySet())) {
                final ResourceBundle bundle = files.get(source);
                bundle.keySet().forEach(key -> translations.putIfAbsent(key, bundle.getString(key)));
            }
            translationStore.registerAll(locale, translations.keySet(), key -> new MessageFormat(translations.get(key), locale));
        }
    }

    private static Map<Locale, ResourceBundle> readLocalizations(Locale defaultLocale, Path directory) {
        final Map<Locale, ResourceBundle> files = new LinkedHashMap<>();
        try (final Stream<Path> paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile).sorted().forEach(file -> {
                final String localeName = TranslatorUtils.removeExtension(file.getFileName().toString());
                final Locale locale = localeName.equals("main") ? defaultLocale : Translator.parseLocale(localeName);
                if (locale == null) return;

                if (files.containsKey(locale)) {
                    TranslatorUtils.LOGGER.error("Skipping {}: another file already holds the {} localization", file, locale);
                    return;
                }

                TranslatorUtils.LOGGER.info("Loading {} ({}) localization...", localeName, locale);
                try (final BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    files.put(locale, new PropertyResourceBundle(reader));
                } catch (IOException e) {
                    TranslatorUtils.LOGGER.error("Encountered an I/O error whilst loading {}", file, e);
                }
            });
        } catch (IOException e) {
            TranslatorUtils.LOGGER.error("Encountered an I/O error whilst loading translations", e);
        }
        return files;
    }

    // The files a locale reads, in the order createTranslationStore describes
    private static Set<Locale> sources(Locale locale, Locale defaultLocale, Set<Locale> files) {
        final List<Locale> listedUnder = FALLBACKS.entrySet().stream()
                .filter(fallback -> fallback.getValue().contains(locale))
                .map(Map.Entry::getKey)
                .toList();

        final Set<Locale> sources = new LinkedHashSet<>();
        sources.add(locale);
        listedUnder.stream().filter(source -> source.getLanguage().equals(locale.getLanguage())).forEach(sources::add);
        sources.addAll(languageFiles(locale.getLanguage(), defaultLocale, files));
        listedUnder.stream().filter(source -> !source.getLanguage().equals(locale.getLanguage()))
                .forEach(source -> sources.addAll(languageFiles(source.getLanguage(), defaultLocale, files)));
        sources.retainAll(files);
        return sources;
    }

    // The locales of a language whose files are read, in the order createTranslationStore describes
    private static Set<Locale> languageFiles(String language, Locale defaultLocale, Set<Locale> files) {
        final Set<Locale> ordered = new LinkedHashSet<>();
        ordered.add(Locale.of(language));
        if (defaultLocale.getLanguage().equals(language)) ordered.add(defaultLocale);
        FALLBACKS.keySet().stream().filter(source -> source.getLanguage().equals(language)).forEach(ordered::add);
        files.stream().filter(file -> file.getLanguage().equals(language)).forEach(ordered::add);
        return ordered;
    }

    public static Locale parseLocale(String localeCode, Supplier<Locale> fallback) {
        if (localeCode.contains("-")) localeCode = localeCode.replace("-", "_");
        if (!localeCode.contains("_") && localeCode.length() == 2) {
            localeCode = localeCode + "_" + localeCode;
        }
        return Objects.requireNonNullElseGet(Translator.parseLocale(localeCode), fallback);
    }

    public static Translator findTranslatorSource(Component component, Locale locale) {
        if (component instanceof TranslatableComponent translatable) {
            return findTranslatorSource(translatable.key(), locale);
        }

        return null;
    }

    public static Translator findTranslatorSource(String key, Locale locale) {
        for (Translator source : GlobalTranslator.translator().sources()) {
            if (source.canTranslate(key, locale)) {
                return source;
            }
        }

        return null;
    }

    private static String removeExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return fileName;
        return fileName.substring(0, fileName.lastIndexOf("."));
    }
}
