package org.redlance.common.adventure;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;
import net.kyori.adventure.translation.Translator;
import org.apache.commons.io.FilenameUtils;
import org.redlance.common.CommonUtils;
import org.redlance.common.utils.ResourceUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class TranslatorUtils {
    public static final Map<String, List<String>> LOCALE_FALLBACK = Map.of(
            "ru_ru", List.of("tt_ru", "uk_ua", "kk_kz", "be_by", "ba_ru"),
            "es_es", List.of("es_419") // Discord
    );

    public static TranslationStore.StringBased<MessageFormat> createTranslationStore(Key name, Locale defaultLocale, Class<?> target, String knownResource) {
        final TranslationStore.StringBased<MessageFormat> translationStore = TranslationStore.messageFormat(name);
        translationStore.defaultLocale(defaultLocale);

        try {
            ResourceUtils.visitResources(knownResource, target, path -> {
                CommonUtils.LOGGER.info("Loading localizations...");

                try (final Stream<Path> files = Files.walk(path)) {
                    files.filter(Files::isRegularFile).forEach(file -> {
                        final String localeName = FilenameUtils.removeExtension(file.getFileName().toString());
                        Locale locale = localeName.equals("main") ? defaultLocale : Translator.parseLocale(localeName);
                        if (locale == null) return;

                        CommonUtils.LOGGER.info("Loading {} ({}) localization...", localeName, locale);

                        if (LOCALE_FALLBACK.containsKey(localeName)) {
                            for (String localeFallback : LOCALE_FALLBACK.get(localeName)) {
                                translationStore.registerAll(parseLocale(localeFallback, null), file, false);
                            }
                        }
                        translationStore.registerAll(locale, file, false);
                    });
                } catch (IOException e) {
                    CommonUtils.LOGGER.error("Encountered an I/O error whilst loading translations", e);
                }
            }, "messages");
        } catch (IOException e) {
            CommonUtils.LOGGER.error("Encountered an I/O error whilst loading translations", e);
        }

        return translationStore;
    }

    public static Locale parseLocale(String string, Supplier<Locale> fallback) {
        return Objects.requireNonNullElseGet(Translator.parseLocale(string), fallback);
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
}
