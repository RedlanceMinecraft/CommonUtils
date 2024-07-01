package org.redlance.common.adventure;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;

import java.text.MessageFormat;
import java.util.Locale;

public class AdventureUtils {
    /**
     * <a href="https://docs.advntr.dev/migration/bungeecord-chat-api.html#chatcolor-stripcolor">...</a>
     */
    public static String stripColor(String message) {
        return PlainTextComponentSerializer.plainText()
                .serialize(LegacyComponentSerializer.legacySection()
                        .deserialize(message.trim())
                );
    }

    /**
     * Serialize the {@link Component} into a {@link String},
     * and translates {@link TranslatableComponent} if there is a
     * translation in {@link GlobalTranslator}
     */
    public static String serializeToString(Component component, Locale locale, Object... args) {
        if (component instanceof TranslatableComponent translatable) {
            MessageFormat translated = GlobalTranslator.translator().translate(translatable.key(), locale);
            if (translated != null) {
                return translated.format(args);
            }

            return translatable.fallback();
        }

        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
