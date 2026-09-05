package io.github.robak132.mcrgb_forge.client.utils;

import static io.github.robak132.mcrgb_forge.MCRGBMod.MOD_ID;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

@Slf4j(topic = MOD_ID)
public abstract class ChatUtils {

    private static final Minecraft MC = Minecraft.getInstance();

    public static void displayClientLocalisedMessage(String format, Object... args) {
        String localisedText = I18n.get(format, args);
        log.info(localisedText);
        MC.player.displayClientMessage(Component.literal(localisedText), false);
    }

    public static void displayClientMessage(String format, Object... args) {
        String text = String.format(format, args);
        log.info(text);
        MC.player.displayClientMessage(Component.literal(text), false);
    }

}
