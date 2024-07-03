package org.redlance.common.adventure;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class AdventureUtils {
    public static final TranslatableComponentRenderer<Locale> RENDERER = TranslatableComponentRenderer.usingTranslationSource(
            GlobalTranslator.translator()
    );

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
    public static String serializeToString(Component component, Locale locale) {
        return PlainTextComponentSerializer.plainText().serialize(
                AdventureUtils.RENDERER.render(component, locale)
        );
    }

    public static Component universalParse(@Nullable String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }

        try {
            return GsonComponentSerializer.gson()
                    .deserialize(str.trim());
        } catch (Throwable ignored) {
        }

        return PlainTextComponentSerializer.plainText()
                .deserialize(str.trim());
    }
}
