package org.redlance.common.adventure;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class AdventureUtils {
    public static @NotNull TextComponent parseLegacy(@NotNull String string) {
        string = string.replace("\"", "").trim();

        if (string.contains("§")) {
            return LegacyComponentSerializer.legacySection().deserialize(string);

        } else if (string.contains("&")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(string);

        } else {
            return Component.text(string);
        }
    }

    public static @NotNull Component universalParse(@NotNull String string) {
        try {
            return GsonComponentSerializer.gson().deserialize(string.trim());
        } catch (Throwable th) {
            return AdventureUtils.parseLegacy(string);
        }
    }

    public static int getTextColor(TextColor textColor, int color) {
        if (textColor != null) {
            int i = ARGB.alpha(color);
            int j = textColor.value();
            return ARGB.color(i, j);
        } else {
            return color;
        }
    }

    public static int getShadowColor(Style style, int textColor) {
        ShadowColor shadowColor = style.shadowColor();
        if (shadowColor != null) {
            int integer = shadowColor.value();
            float f = ARGB.alphaFloat(textColor);
            float g = ARGB.alphaFloat(integer);
            return f != 1.0F ? ARGB.color(ARGB.as8BitChannel(f * g), integer) : integer;
        } else {
            return ARGB.scaleRGB(textColor, 0.25F);
        }
    }
}
