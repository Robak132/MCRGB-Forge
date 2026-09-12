package io.github.robak132.mcrgb_forge.client;

import io.github.robak132.mcrgb_forge.client.analysis.SpriteColor;
import io.github.robak132.mcrgb_forge.client.analysis.SpriteDetails;
import io.github.robak132.mcrgb_forge.client.analysis.TextureNoise;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class ColorInfoLines {

    private ColorInfoLines() {
    }

    public static List<Component> linesFor(SpriteDetails details) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(details.getName() + ":").withStyle(ChatFormatting.DARK_GRAY));

        TextureNoise noise = details.getNoise();
        if (noise != null) {
            lines.add(metric(Localisation.UI_NOISE, noise.global()));
            lines.add(metric(Localisation.UI_SPATIAL, noise.kernel()));
        }

        for (SpriteColor color : details.getColors()) {
            lines.add(colorLine(color));
        }
        return lines;
    }

    public static Component metric(String translationKey, int value) {
        MutableComponent marker = Component.literal("⬛").withStyle(ChatFormatting.DARK_GRAY);
        return marker.append(Component.translatable(translationKey)
                .append(Component.literal(": " + value))
                .withStyle(ChatFormatting.GRAY));
    }

    public static Component colorLine(SpriteColor color) {
        MutableComponent label = Component.literal(color.color().toHexString() + "  " + color.weight() + "%")
                .withStyle(ChatFormatting.GRAY);
        MutableComponent colorBox = Component.literal("⬛")
                .withStyle(Style.EMPTY.withColor(color.color().argb()));
        return colorBox.append(label);
    }

    public static int firstColorLineIndex(SpriteDetails details) {
        return 1 + (details.getNoise() == null ? 0 : 2);
    }

    public static boolean isColorLine(SpriteDetails details, int index) {
        return index >= firstColorLineIndex(details);
    }
}
