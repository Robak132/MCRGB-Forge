package io.github.robak132.mcrgb_forge.client;

import static io.github.robak132.mcrgb_forge.MCRGBMod.MOD_ID;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Bus.MOD, value = Dist.CLIENT)
public final class MCRGBKeybindings {

    public static final KeyMapping OPEN_GUI = new KeyMapping(
            Localisation.KEY_OPEN_GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, Localisation.KEY_CATEGORY);
    public static final KeyMapping QUICK_SEARCH = new KeyMapping(
            Localisation.KEY_QUICK_SEARCH, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, Localisation.KEY_CATEGORY);

    private MCRGBKeybindings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GUI);
        event.register(QUICK_SEARCH);
    }
}
