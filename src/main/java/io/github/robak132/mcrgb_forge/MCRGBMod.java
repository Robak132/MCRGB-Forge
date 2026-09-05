package io.github.robak132.mcrgb_forge;

import io.github.robak132.mcrgb_forge.config.MCRGBConfig;
import lombok.extern.slf4j.Slf4j;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MCRGBMod.MOD_ID)
@Slf4j(topic = MCRGBMod.MOD_ID)
public class MCRGBMod {

    public static final String MOD_ID = "mcrgb_forge";

    public MCRGBMod(FMLJavaModLoadingContext context) {
        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.CLIENT, MCRGBConfig.GENERAL_SPEC, "mcrgb_forge.toml");
    }

}
