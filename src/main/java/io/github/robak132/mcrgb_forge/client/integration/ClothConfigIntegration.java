package io.github.robak132.mcrgb_forge.client.integration;

import static io.github.robak132.mcrgb_forge.MCRGBMod.MOD_ID;
import static io.github.robak132.mcrgb_forge.config.MCRGBConfig.ALWAYS_SHOW_TOOLTIPS;
import static io.github.robak132.mcrgb_forge.config.MCRGBConfig.BYPASS_OP;
import static io.github.robak132.mcrgb_forge.config.MCRGBConfig.COLOR_CALCULATION_MODE;
import static io.github.robak132.mcrgb_forge.config.MCRGBConfig.GENERAL_SPEC;
import static io.github.robak132.mcrgb_forge.config.MCRGBConfig.GIVE_COMMAND;
import static io.github.robak132.mcrgb_forge.config.MCRGBConfig.ITEM_SPAWNING_MODE;
import static io.github.robak132.mcrgb_forge.config.MCRGBConfig.MAX_TOOLTIP_LINES;
import static io.github.robak132.mcrgb_forge.config.MCRGBConfig.READ_JSON_FILE;
import static io.github.robak132.mcrgb_forge.config.MCRGBConfig.SLIDER_CONSTANT_UPDATE;
import static me.shedaniel.clothconfig2.api.Requirement.isValue;

import io.github.robak132.mcrgb_forge.client.Localisation;
import io.github.robak132.mcrgb_forge.client.MCRGBClient;
import io.github.robak132.mcrgb_forge.config.MCRGBConfig.ColorCalculationMode;
import io.github.robak132.mcrgb_forge.config.MCRGBConfig.ItemSpawningMode;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.ValueHolder;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.widget.SearchFieldEntry;
import me.shedaniel.clothconfig2.impl.builders.BooleanToggleBuilder;
import me.shedaniel.clothconfig2.impl.builders.StringFieldBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Bus.MOD, value = Dist.CLIENT)
@Slf4j(topic = MOD_ID)
public final class ClothConfigIntegration {

    private static final Function<Boolean, Component> alwaysShowToolTipsTextSupplier = value -> Component.translatable(
            value ? Localisation.OPTIONS_ALL_CONTEXTS : Localisation.OPTIONS_PICKER_ONLY);
    private static final Function<Boolean, Component> sliderConstantUpdateTextSupplier = value -> Component.translatable(
            value ? Localisation.OPTIONS_WHILE_SCROLLING : Localisation.OPTIONS_AFTER_SCROLLING);

    private ClothConfigIntegration() {
    }

    public static Screen getConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable(Localisation.CONFIG_TITLE))
                .setDoesConfirmSave(true);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory configs = builder.getOrCreateCategory(Component.translatable(Localisation.CONFIG_CATEGORY));

        // Always Show Tooltips toggle
        BooleanToggleBuilder alwaysShowToolTip = entryBuilder.startBooleanToggle(Component.translatable(Localisation.OPTION_ALWAYS_SHOW_TOOLTIPS),
                ALWAYS_SHOW_TOOLTIPS.get());
        alwaysShowToolTip.setDefaultValue(ALWAYS_SHOW_TOOLTIPS.getDefault()).setYesNoTextSupplier(alwaysShowToolTipsTextSupplier)
                .setSaveConsumer(ALWAYS_SHOW_TOOLTIPS::set).setTooltip(Component.translatable(Localisation.TOOLTIP_ALWAYS_SHOW_TOOLTIPS)).build();
        configs.addEntry(alwaysShowToolTip.build());

        configs.addEntry(entryBuilder.startIntField(Component.translatable(Localisation.OPTION_MAX_TOOLTIP_LINES), MAX_TOOLTIP_LINES.get())
                .setDefaultValue(MAX_TOOLTIP_LINES.getDefault()).setMin(1).setMax(100).setSaveConsumer(MAX_TOOLTIP_LINES::set)
                .setTooltip(Component.translatable(Localisation.TOOLTIP_MAX_TOOLTIP_LINES)).build());

        // Slider Constant Update toggle
        BooleanToggleBuilder sliderConstantUpdate = entryBuilder.startBooleanToggle(Component.translatable(Localisation.OPTION_SLIDER_CONSTANT_UPDATE),
                SLIDER_CONSTANT_UPDATE.get());
        sliderConstantUpdate.setDefaultValue(SLIDER_CONSTANT_UPDATE.getDefault()).setYesNoTextSupplier(sliderConstantUpdateTextSupplier)
                .setSaveConsumer(SLIDER_CONSTANT_UPDATE::set).setTooltip(Component.translatable(Localisation.TOOLTIP_SLIDER_CONSTANT_UPDATE)).build();
        configs.addEntry(sliderConstantUpdate.build());

        configs.addEntry(entryBuilder.startBooleanToggle(Component.translatable(Localisation.OPTION_READ_JSON_FILE), READ_JSON_FILE.get())
                .setDefaultValue(READ_JSON_FILE.getDefault()).setSaveConsumer(READ_JSON_FILE::set)
                .setTooltip(Component.translatable(Localisation.TOOLTIP_READ_JSON_FILE)).build());

        ColorCalculationMode initialCalculationMode = COLOR_CALCULATION_MODE.get();
        configs.addEntry(
                entryBuilder.startEnumSelector(Component.translatable(Localisation.OPTION_COLOR_MODE), ColorCalculationMode.class, COLOR_CALCULATION_MODE.get())
                        .setDefaultValue(COLOR_CALCULATION_MODE.getDefault())
                        .setEnumNameProvider(value -> Component.translatable(Localisation.colorMode(value))).setSaveConsumer(COLOR_CALCULATION_MODE::set)
                        .setTooltip(Component.translatable(Localisation.TOOLTIP_COLOR_MODE)).build());

        var itemSpawningMode = entryBuilder.startEnumSelector(Component.translatable(Localisation.OPTION_ITEM_SPAWNING_MODE), ItemSpawningMode.class,
                        ITEM_SPAWNING_MODE.get()).setDefaultValue(ITEM_SPAWNING_MODE.getDefault())
                .setEnumNameProvider(value -> Component.translatable(Localisation.itemSpawningMode(value))).setSaveConsumer(ITEM_SPAWNING_MODE::set)
                .setTooltip(Component.translatable(Localisation.TOOLTIP_ITEM_SPAWNING_MODE)).build();
        configs.addEntry(itemSpawningMode);

        // Give Command string field
        StringFieldBuilder commandString = entryBuilder.startStrField(Component.translatable(Localisation.OPTION_GIVE_COMMAND), GIVE_COMMAND.get());
        commandString.setDefaultValue(GIVE_COMMAND.getDefault()).setSaveConsumer(GIVE_COMMAND::set)
                .setTooltip(Component.translatable(Localisation.TOOLTIP_GIVE_COMMAND));

        // Bypass OP toggle
        BooleanToggleBuilder bypassOP = entryBuilder.startBooleanToggle(Component.translatable(Localisation.OPTION_BYPASS_OP), BYPASS_OP.get());
        bypassOP.setDefaultValue(BYPASS_OP.getDefault()).setSaveConsumer(BYPASS_OP::set).setTooltip(Component.translatable(Localisation.TOOLTIP_BYPASS_OP));

        requireGiveCommandMode(itemSpawningMode, commandString, bypassOP);
        configs.addEntry(commandString.build());
        configs.addEntry(bypassOP.build());

        builder.setSavingRunnable(() -> {
            GENERAL_SPEC.save();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null && COLOR_CALCULATION_MODE.get() != initialCalculationMode) {
                MCRGBClient.refreshScan();
            }
        });
        builder.setAfterInitConsumer(screen -> {
            if (screen instanceof ClothConfigScreen configScreen) {
                configScreen.listWidget.children().removeIf(SearchFieldEntry.class::isInstance);
            }
        });

        return builder.build();
    }

    @SuppressWarnings("UnstableApiUsage")
    private static void requireGiveCommandMode(ValueHolder<ItemSpawningMode> itemSpawningMode, StringFieldBuilder commandString,
            BooleanToggleBuilder bypassOP) {
        var giveCommandMode = isValue(itemSpawningMode, ItemSpawningMode.GIVE_COMMAND);
        commandString.setRequirement(giveCommandMode);
        bypassOP.setRequirement(giveCommandMode);
    }

    private static boolean isClothConfigLoaded() {
        if (!ModList.get().isLoaded("cloth_config")) {
            return false;
        }

        try {
            Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
            return true;
        } catch (ClassNotFoundException exception) {
            log.error("Cloth Config is installed but ConfigBuilder class was not found", exception);
            return false;
        }
    }

    @SubscribeEvent
    public static void init(FMLClientSetupEvent event) {
        if (!isClothConfigLoaded()) {
            log.warn("Cloth Config not found, config screen will be unavailable.");
            return;
        }
        ModList.get().getModContainerById(MOD_ID).orElseThrow().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> getConfigScreen(parent)));
        log.info("Cloth Config detected, registering config screen.");
    }
}
