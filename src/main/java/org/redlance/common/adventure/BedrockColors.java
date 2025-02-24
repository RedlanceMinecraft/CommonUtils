package org.redlance.common.adventure;

import static net.kyori.adventure.text.serializer.legacy.CharacterAndFormat.characterAndFormat;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.CharacterAndFormat;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.redlance.common.CommonUtils;
import org.redlance.common.utils.ReflectUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BedrockColors {
    public static final CharacterAndFormat MINECOIN_GOLD = characterAndFormat('g', TextColor.color(221, 214, 5), true);
    public static final CharacterAndFormat MATERIAL_QUARTZ = characterAndFormat('h', TextColor.color(227, 212, 209), true);
    public static final CharacterAndFormat MATERIAL_IRON = characterAndFormat('i', TextColor.color(206, 202, 202), true);
    public static final CharacterAndFormat MATERIAL_NETHERITE = characterAndFormat('j', TextColor.color(68, 58, 59), true);
    public static final CharacterAndFormat MATERIAL_REDSTONE = characterAndFormat('m', TextColor.color(151, 22, 7), true);
    public static final CharacterAndFormat MATERIAL_COPPER = characterAndFormat('n', TextColor.color(180, 104, 77), true);
    public static final CharacterAndFormat MATERIAL_GOLD = characterAndFormat('p', TextColor.color(222, 177, 45), true);
    public static final CharacterAndFormat MATERIAL_EMERALD = characterAndFormat('q', TextColor.color(17, 160, 54), true);
    public static final CharacterAndFormat MATERIAL_DIAMOND = characterAndFormat('s', TextColor.color(44, 186, 168), true);
    public static final CharacterAndFormat MATERIAL_LAPIS = characterAndFormat('t', TextColor.color(33, 73, 123), true);
    public static final CharacterAndFormat MATERIAL_AMETHYST = characterAndFormat('u', TextColor.color(154, 92, 198), true);

    public static final List<CharacterAndFormat> BEDROCK_COLORS = createBedrockColors();

    @SuppressWarnings("unchecked")
    protected static void inject() throws ReflectiveOperationException {
        // Inject bedrock colors
        Class<?> defaultsClass = Class.forName("net.kyori.adventure.text.serializer.legacy.CharacterAndFormatImpl$Defaults");
        Field defaultsField = defaultsClass.getDeclaredField("DEFAULTS");

        List<CharacterAndFormat> formats = new ArrayList<>(33);
        formats.addAll((List<CharacterAndFormat>) ReflectUtils.getStaticField(defaultsField));
        formats.addAll(BEDROCK_COLORS);
        ReflectUtils.setStaticField(defaultsField, Collections.unmodifiableList(formats));

        // Some classload verify
        Class<?> characterAndFormatSetClass = Class.forName("net.kyori.adventure.text.serializer.legacy.CharacterAndFormatSet");
        Field defaultField = characterAndFormatSetClass.getDeclaredField("DEFAULT");

        Object characterAndFormatSet = ReflectUtils.getStaticField(defaultField);

        Field colorsField = characterAndFormatSetClass.getDeclaredField("colors");
        colorsField.setAccessible(true);

        List<TextColor> colors = (List<TextColor>) colorsField.get(characterAndFormatSet);
        for (CharacterAndFormat format : BEDROCK_COLORS) {
            if (!colors.contains((TextColor) format.format())) {
                CommonUtils.LOGGER.warn("{} is already loaded!", characterAndFormatSet);

                Method ofMethod = characterAndFormatSetClass.getDeclaredMethod("of", List.class);
                ofMethod.setAccessible(true);

                Object newFormats = ofMethod.invoke(null, formats);
                ReflectUtils.setStaticField(defaultField, newFormats);

                checkSerializer(LegacyComponentSerializer.legacyAmpersand(), characterAndFormatSet, newFormats);
                checkSerializer(LegacyComponentSerializer.legacySection(), characterAndFormatSet, newFormats);
                break;
            }
        }

        CommonUtils.LOGGER.debug("Bedrock colors injected!");
    }

    private static List<CharacterAndFormat> createBedrockColors() {
        return List.of(
                BedrockColors.MINECOIN_GOLD,
                BedrockColors.MATERIAL_QUARTZ,
                BedrockColors.MATERIAL_IRON,
                BedrockColors.MATERIAL_NETHERITE,
                BedrockColors.MATERIAL_REDSTONE,
                BedrockColors.MATERIAL_COPPER,
                BedrockColors.MATERIAL_GOLD,
                BedrockColors.MATERIAL_EMERALD,
                BedrockColors.MATERIAL_DIAMOND,
                BedrockColors.MATERIAL_LAPIS,
                BedrockColors.MATERIAL_AMETHYST
        );
    }

    private static void checkSerializer(LegacyComponentSerializer serializer, Object oldFormats, Object newFormats) throws ReflectiveOperationException {
        Field formatsField = serializer.getClass().getDeclaredField("formats");
        formatsField.setAccessible(true);

        if (formatsField.get(serializer) == oldFormats) {
            CommonUtils.LOGGER.warn("Replacing formats in {}!", serializer);
            formatsField.set(serializer, newFormats);
        }
    }
}
