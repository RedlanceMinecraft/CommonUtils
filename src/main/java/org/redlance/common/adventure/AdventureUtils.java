package org.redlance.common.adventure;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.redlance.common.CommonUtils;

public class AdventureUtils {
    static {
        try {
            BedrockColors.inject();
        } catch (Throwable th) {
            CommonUtils.LOGGER.error("Failed to inject bedrock colors!", th);
        }
    }

    public static @NotNull TextComponent parseLegacy(@NotNull String string) {
        string = string.replace("\"", "").trim();

        if (string.contains("&")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(string);

        } else if (string.contains("§")) {
            return LegacyComponentSerializer.legacySection().deserialize(string);

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
}
