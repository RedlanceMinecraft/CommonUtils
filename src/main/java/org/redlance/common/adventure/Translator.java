package org.redlance.common.adventure;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.TranslationRegistry;
import org.apache.commons.io.FilenameUtils;
import org.redlance.common.CommonUtils;
import org.redlance.common.utils.ResourceUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public class Translator {
    public static final Map<String, List<String>> LOCALE_FALLBACK = Map.of(
            "ru", List.of("ua", "kz", "by")
    );

    public static TranslationRegistry createRegistrySource(Key name, final Locale defaultLocale, Class<?> target, String knownResource) {
        final TranslationRegistry translationRegistry = TranslationRegistry.create(name);
        translationRegistry.defaultLocale(defaultLocale);

        try {
            ResourceUtils.visitResources(knownResource, target, path -> {
                CommonUtils.LOGGER.info("Loading localizations...");

                try (final Stream<Path> files = Files.walk(path)) {
                    files.filter(Files::isRegularFile).forEach(file -> {
                        final String localeName = FilenameUtils.removeExtension(file.getFileName().toString());
                        Locale locale = localeName.equals("main") ? defaultLocale : Locale.forLanguageTag(localeName);

                        CommonUtils.LOGGER.info("Loading {} ({}) localization...", localeName, locale);

                        if (LOCALE_FALLBACK.containsKey(localeName)) {
                            for (String localeFallback : LOCALE_FALLBACK.get(localeName)) {
                                translationRegistry.registerAll(Locale.forLanguageTag(localeFallback), file, false);
                            }
                        }
                        translationRegistry.registerAll(locale, file, false);
                    });
                } catch (IOException e) {
                    CommonUtils.LOGGER.error("Encountered an I/O error whilst loading translations", e);
                }
            }, "messages");
        } catch (IOException e) {
            CommonUtils.LOGGER.error("Encountered an I/O error whilst loading translations", e);
        }

        return translationRegistry;
    }
}
