package org.redlance.common.adventure;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

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

    public static String serializeToString(String string, Locale locale) {
        Component component = universalParse(string);
        if (component == null) {
            return null;
        }

        return serializeToString(component, locale);
    }

    /**
     * Serialize the {@link Component} into a {@link String},
     * and translates {@link TranslatableComponent} if there is a
     * translation in {@link GlobalTranslator}
     */
    public static String serializeToString(Component component, Locale locale) {
        return PlainTextComponentSerializer.plainText().serialize(
                GlobalTranslator.render(component, locale)
        );
    }

    public static Component universalParse(@Nullable String string) {
        if (StringUtils.isBlank(string)) {
            return null;
        }

        try {
            return JSONComponentSerializer.json().deserialize(string.trim());
        } catch(Throwable th) {
            return Component.text(string.replace("\"", "").trim());
        }
    }
}
