package io.github.robak132.mcrgb_forge.config;

import static io.github.robak132.mcrgb_forge.MCRGBMod.MOD_ID;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;

@Getter
@Setter
@Slf4j(topic = MOD_ID)
public final class MCRGBConfig {

    public static final boolean EMI_LOADED = ModList.get().isLoaded("emi");
    public static final ForgeConfigSpec GENERAL_SPEC;
    public static final ForgeConfigSpec.BooleanValue BYPASS_OP;
    public static final ForgeConfigSpec.BooleanValue ALWAYS_SHOW_TOOLTIPS;
    public static final ForgeConfigSpec.BooleanValue SLIDER_CONSTANT_UPDATE;
    public static final ForgeConfigSpec.ConfigValue<String> GIVE_COMMAND;
    public static final ForgeConfigSpec.IntValue MAX_TOOLTIP_LINES;
    public static final ForgeConfigSpec.EnumValue<ColorCalculationMode> COLOR_CALCULATION_MODE;
    public static final ForgeConfigSpec.EnumValue<ItemSpawningMode> ITEM_SPAWNING_MODE;

    static {
        ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();
        ALWAYS_SHOW_TOOLTIPS = configBuilder.define("alwaysShowToolTips", false);
        SLIDER_CONSTANT_UPDATE = configBuilder.define("sliderConstantUpdate", true);
        BYPASS_OP = configBuilder.define("bypassOP", false);
        GIVE_COMMAND = configBuilder.define("command", "give %p %i%c %q");
        MAX_TOOLTIP_LINES = configBuilder.defineInRange("maxTooltipLines", 15, 1, 100);
        COLOR_CALCULATION_MODE = configBuilder.defineEnum("colorCalculationMode", ColorCalculationMode.OKLAB);
        ITEM_SPAWNING_MODE = configBuilder.defineEnum("itemSpawningMode", ItemSpawningMode.CREATIVE_DRAG);
        GENERAL_SPEC = configBuilder.build();
    }

    public enum ColorCalculationMode {
        OKLAB,
        FABRIC,
        MEAN,
        MEDIAN
    }

    public enum ItemSpawningMode {
        CREATIVE_DRAG,
        GIVE_COMMAND,
        VIEW_ONLY
    }
}
