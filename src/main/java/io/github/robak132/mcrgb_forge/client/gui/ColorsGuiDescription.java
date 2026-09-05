package io.github.robak132.mcrgb_forge.client.gui;

import static io.github.robak132.mcrgb_forge.MCRGBMod.MOD_ID;
import static io.github.robak132.mcrgb_forge.client.utils.TypeConversionUtils.stringToInt;

import io.github.robak132.libgui_forge.widget.WButton;
import io.github.robak132.libgui_forge.widget.WColorWheel;
import io.github.robak132.libgui_forge.widget.WGradientSlider;
import io.github.robak132.libgui_forge.widget.WGridPanel;
import io.github.robak132.libgui_forge.widget.WLabel;
import io.github.robak132.libgui_forge.widget.WPlainPanel;
import io.github.robak132.libgui_forge.widget.WSlider;
import io.github.robak132.libgui_forge.widget.WTextField;
import io.github.robak132.libgui_forge.widget.WToggleButton;
import io.github.robak132.libgui_forge.widget.data.HorizontalAlignment;
import io.github.robak132.libgui_forge.widget.data.Insets;
import io.github.robak132.libgui_forge.widget.data.Texture;
import io.github.robak132.libgui_forge.widget.data.colors.Color;
import io.github.robak132.libgui_forge.widget.data.colors.Color.ColorModel;
import io.github.robak132.libgui_forge.widget.data.colors.RGB;
import io.github.robak132.libgui_forge.widget.icon.TextureIcon;
import io.github.robak132.mcrgb_forge.client.Localisation;
import io.github.robak132.mcrgb_forge.client.MCRGBClient;
import io.github.robak132.mcrgb_forge.client.analysis.ColorScoring;
import io.github.robak132.mcrgb_forge.client.analysis.SpriteDetails;
import io.github.robak132.mcrgb_forge.client.gui.widgets.WButtonWithTooltip;
import io.github.robak132.mcrgb_forge.client.gui.widgets.WColorGuiHotbar;
import io.github.robak132.mcrgb_forge.client.gui.widgets.WColorGuiSlot;
import io.github.robak132.mcrgb_forge.client.gui.widgets.WColorScrollBar;
import io.github.robak132.mcrgb_forge.client.gui.widgets.WSmartTextField;
import io.github.robak132.mcrgb_forge.client.integration.ClothConfigIntegration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

@Slf4j
public class ColorsGuiDescription extends AbstractGuiDescription {

    private final List<ItemStack> stacks = new ArrayList<>();
    private final List<WColorGuiSlot> wColorGuiSlots = new ArrayList<>();
    private final Map<Block, List<SpriteDetails>> blockSpriteMap;
    private final WLabel rLabel = new WLabel(Component.translatable(Localisation.UI_R_FOR_RED), 0xFFFF0000);
    private final WSlider rSlider = new WSlider(0, 255, Direction.Plane.VERTICAL);
    private final WSmartTextField rInput = new WSmartTextField(Component.empty());
    private final WLabel gLabel = new WLabel(Component.translatable(Localisation.UI_G_FOR_GREEN), 0xFF00FF00);
    private final WSlider gSlider = new WSlider(0, 255, Direction.Plane.VERTICAL);
    private final WSmartTextField gInput = new WSmartTextField(Component.empty());
    private final WLabel bLabel = new WLabel(Component.translatable(Localisation.UI_B_FOR_BLUE), 0xFF0000FF);
    private final WSlider bSlider = new WSlider(0, 255, Direction.Plane.VERTICAL);
    private final WSmartTextField bInput = new WSmartTextField(Component.empty());
    private final WButton rgbButton = new WButton(Component.translatable(Localisation.UI_RGB));
    private final WButton hsvButton = new WButton(Component.translatable(Localisation.UI_HSV));
    private final WButton hslButton = new WButton(Component.translatable(Localisation.UI_HSL));
    private final ItemStack helmet = new ItemStack(Items.LEATHER_HELMET);
    private final ItemStack chestplate = new ItemStack(Items.LEATHER_CHESTPLATE);
    private final ItemStack leggings = new ItemStack(Items.LEATHER_LEGGINGS);
    private final ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
    private final ItemStack horse = new ItemStack(Items.LEATHER_HORSE_ARMOR);
    private final WColorWheel colorWheel = new WColorWheel(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "wheel.png"), 0, 0, 1, 1,
            new Texture(ResourceLocation.fromNamespaceAndPath(MOD_ID, "circle4.png")));
    private final WToggleButton colorWheelToggle = new WToggleButton();
    private final WGradientSlider wheelValueSlider = new WGradientSlider(
            0, 255, Direction.Plane.VERTICAL,
            new Texture(ResourceLocation.fromNamespaceAndPath(MOD_ID, "value_slider.png")),
            new Texture(ResourceLocation.fromNamespaceAndPath(MOD_ID, "circle4.png")));
    private final WTextField searchField = new WTextField(Component.translatable(Localisation.UI_REFINE), 11);
    private final WPlainPanel sliderArea = new WPlainPanel();
    private final WPlainPanel inputs = new WPlainPanel();
    private final WGridPanel armourSlots = new WGridPanel();
    private final WColorScrollBar scrollBar = new WColorScrollBar(this::placeSlots);
    private boolean initialized = false;

    public ColorsGuiDescription(RGB launchColor, Map<Block, List<SpriteDetails>> blockSpriteMap) {
        this.blockSpriteMap = Map.copyOf(blockSpriteMap);

        WButtonWithTooltip refreshButton = new WButtonWithTooltip(new TextureIcon(ResourceLocation.fromNamespaceAndPath(MOD_ID, "refresh.png")),
                Component.translatable(Localisation.UI_REFRESH_INFO));

        WButton settingsButton = new WButton(new TextureIcon(ResourceLocation.fromNamespaceAndPath(MOD_ID, "settings.png")));

        setRootPanel(root);
        root.add(mainPanel, 0, 0);
        mainPanel.setSize(320, 240);
        mainPanel.setInsets(Insets.ROOT_PANEL);

        mainPanel.add(hexInput, 11, 1, 5, 1);
        mainPanel.add(colorDisplay, 16, 1, 2, 2);
        colorDisplay.setLocation(colorDisplay.getAbsoluteX() + 1, colorDisplay.getAbsoluteY() - 1);
        mainPanel.add(scrollBar, 9, 1, 1, SLOTS_HEIGHT - 1);

        mainPanel.add(refreshButton, 17, 11, 1, 1);
        refreshButton.setSize(20, 20);
        refreshButton.setIconSize(18);
        refreshButton.setAlignment(HorizontalAlignment.LEFT);
        refreshButton.setOnClick(() -> {
            Minecraft.getInstance().setScreen(null);
            MCRGBClient.refreshScan();
        });

        mainPanel.add(searchField, 6, 0, 4, 1);
        searchField.setChangedListener(v -> colorSort());

        mainPanel.add(settingsButton, 17, 0, 1, 1);
        settingsButton.setSize(20, 20);
        settingsButton.setIconSize(18);
        settingsButton.setAlignment(HorizontalAlignment.LEFT);

        if (ModList.get().isLoaded("cloth_config")) {
            settingsButton.setOnClick(() -> Minecraft.getInstance().setScreen(ClothConfigIntegration.getConfigScreen(Minecraft.getInstance().screen)));
        } else {
            settingsButton.setOnClick(() -> MCRGBClient.showToast(
                    Component.translatable(Localisation.WARNING_NO_CLOTH_CONFIG)));
        }

        mainPanel.add(rgbButton, 10, 11, 1, 1);
        rgbButton.setLocation(201, 205);
        rgbButton.setSize(26, 20);
        rgbButton.setEnabled(false);
        rgbButton.setAlignment(HorizontalAlignment.CENTER);

        mainPanel.add(hsvButton, 13, 11, 1, 1);
        hsvButton.setLocation(237, 205);
        hsvButton.setSize(26, 20);
        hsvButton.setAlignment(HorizontalAlignment.CENTER);

        mainPanel.add(hslButton, 15, 11, 1, 1);
        hslButton.setLocation(273, 205);
        hslButton.setSize(26, 20);
        hslButton.setAlignment(HorizontalAlignment.CENTER);

        rgbButton.setOnClick(() -> setColor(ColorModel.RGB));
        hsvButton.setOnClick(() -> setColor(ColorModel.HSV));
        hslButton.setOnClick(() -> setColor(ColorModel.HSL));

        mainPanel.add(new WLabel(Component.translatable(Localisation.UI_HEADER)), 0, 0, 2, 1);
        mainPanel.add(savedPalettesArea, 0, SLOTS_HEIGHT);
        WColorGuiHotbar hotbar = new WColorGuiHotbar(this);
        mainPanel.add(hotbar, 0, SLOTS_HEIGHT + 5, 9, 1);
        mainPanel.add(sliderArea, 11, 2, 6, 7);

        mainPanel.add(rLabel, 6, 7, 1, 1);
        mainPanel.add(gLabel, 6, 7, 1, 1);
        mainPanel.add(bLabel, 6, 7, 1, 1);

        rLabel.setLocation(211, 50);
        gLabel.setLocation(247, 50);
        bLabel.setLocation(283, 50);

        sliderArea.add(rSlider, 0, 18, 18, 108);
        sliderArea.add(gSlider, 36, 18, 18, 108);
        sliderArea.add(bSlider, 72, 18, 18, 108);

        mainPanel.add(inputs, 10, 9, 2, 1);
        inputs.add(rInput, 14, 9, 26, WTextField.DEFAULT_HEIGHT);
        inputs.add(gInput, 50, 9, 26, WTextField.DEFAULT_HEIGHT);
        inputs.add(bInput, 86, 9, 26, WTextField.DEFAULT_HEIGHT);

        rSlider.setValueChangeListener(this::onSliderValueChange);
        gSlider.setValueChangeListener(this::onSliderValueChange);
        bSlider.setValueChangeListener(this::onSliderValueChange);

        rSlider.setDraggingFinishedListener(v -> colorSort());
        gSlider.setDraggingFinishedListener(v -> colorSort());
        bSlider.setDraggingFinishedListener(v -> colorSort());

        rInput.setCommitListener(this::onValueEntered);
        gInput.setCommitListener(this::onValueEntered);
        bInput.setCommitListener(this::onValueEntered);
        hexInput.setCommitListener(this::onHexEntered);

        mainPanel.add(armourSlots, 17, 3);
        armourSlots.add(new WColorGuiSlot(helmet, this), 0, 0);
        armourSlots.add(new WColorGuiSlot(chestplate, this), 0, 1);
        armourSlots.add(new WColorGuiSlot(leggings, this), 0, 2);
        armourSlots.add(new WColorGuiSlot(boots, this), 0, 3);
        armourSlots.add(new WColorGuiSlot(horse, this), 0, 4);

        colorWheelToggle.setOffImage(new Texture(ResourceLocation.fromNamespaceAndPath(MOD_ID, "wheel_small.png")));
        colorWheelToggle.setOnImage(new Texture(ResourceLocation.fromNamespaceAndPath(MOD_ID, "sliders.png")));
        colorWheelToggle.setOnToggle(this::toggleColorWheel);
        mainPanel.add(colorWheelToggle, 17, 10);
        colorWheelToggle.setLocation(314, 180);

        wheelValueSlider.setValueChangeListener(value -> {
            colorWheel.setOpaqueTint(new RGB(255, value, value, value).argb());
            colorWheel.pickAtCursor();
        });
        colorWheel.setColorPickListener(this::onColorPicked);

        mainPanel.validate(this);
        root.validate(this);

        Minecraft.getInstance().execute(() -> deferredInit(launchColor));
    }

    private void deferredInit(RGB launchColor) {
        if (initialized) {
            return;
        }
        initialized = true;
        setColor(launchColor);
    }

    @Override
    public void setColor(Color color, ColorModel model) {
        super.setColor(color, model);
        lockWidgets(() -> {
            switch (activeColorModel) {
                case RGB -> {
                    configureColorControls(
                            Localisation.UI_R_FOR_RED, 0xFFFF0000,
                            Localisation.UI_G_FOR_GREEN, 0xFF00FF00,
                            Localisation.UI_B_FOR_BLUE, 0xFF0000FF,
                            255, 255, 255);
                    rgbButton.setEnabled(false);
                    hsvButton.setEnabled(true);
                    hslButton.setEnabled(true);
                }
                case HSV -> {
                    configureColorControls(
                            Localisation.UI_H_FOR_HUE_HSV, 0xFF3F3F3F,
                            Localisation.UI_S_FOR_SAT_HSV, 0xFF3F3F3F,
                            Localisation.UI_V_FOR_VAL_HSV, 0xFF3F3F3F,
                            360, 100, 100);
                    rgbButton.setEnabled(true);
                    hsvButton.setEnabled(false);
                    hslButton.setEnabled(true);
                }
                case HSL -> {
                    configureColorControls(
                            Localisation.UI_H_FOR_HUE_HSL, 0xFF3F3F3F,
                            Localisation.UI_S_FOR_SAT_HSL, 0xFF3F3F3F,
                            Localisation.UI_L_FOR_LIT_HSL, 0xFF3F3F3F,
                            360, 100, 100);
                    rgbButton.setEnabled(true);
                    hsvButton.setEnabled(true);
                    hslButton.setEnabled(false);
                }
                case LAB -> throw new IllegalArgumentException("OKLab is not supported by the color editor");
            }
            scrollBar.setValue(0);
            refreshComponents();
        }, e -> log.error("Error refreshing color components", e));
    }

    private void configureColorControls(
            String firstLabel, int firstColor,
            String secondLabel, int secondColor,
            String thirdLabel, int thirdColor,
            int firstMax, int secondMax, int thirdMax) {
        rLabel.setText(Component.translatable(firstLabel));
        rLabel.setColor(firstColor);
        gLabel.setText(Component.translatable(secondLabel));
        gLabel.setColor(secondColor);
        bLabel.setText(Component.translatable(thirdLabel));
        bLabel.setColor(thirdColor);

        rSlider.setMinValue(0);
        gSlider.setMinValue(0);
        bSlider.setMinValue(0);
        rSlider.setMaxValue(firstMax);
        gSlider.setMaxValue(secondMax);
        bSlider.setMaxValue(thirdMax);
    }

    private void onSliderValueChange(int value) {
        setColor(Color.create(activeColorModel, rSlider.getValue(), gSlider.getValue(), bSlider.getValue()));
    }

    private void onValueEntered(String value) {
        Integer r = stringToInt(rInput.getText());
        Integer g = stringToInt(gInput.getText());
        Integer b = stringToInt(bInput.getText());

        if (r == null || g == null || b == null) {
            return;
        }

        switch (activeColorModel) {
            case RGB -> {
                r = Mth.clamp(r, 0, 255);
                g = Mth.clamp(g, 0, 255);
                b = Mth.clamp(b, 0, 255);
            }
            case HSV, HSL -> {
                r = Mth.clamp(r, 0, 360);
                g = Mth.clamp(g, 0, 100);
                b = Mth.clamp(b, 0, 100);
            }
            case LAB -> throw new IllegalStateException("OKLab is not supported by the color editor");
        }

        setColor(Color.create(activeColorModel, r, g, b));
    }

    private void onHexEntered(String value) {
        Integer v = normalizeHexInput(value);
        if (v != null) {
            setColor(new RGB(v));
        }
    }

    private void refreshComponents() {
        if (!rSlider.isDragging()) {
            rSlider.setValue(activeColor.ch0().intValue());
        }
        if (!gSlider.isDragging()) {
            gSlider.setValue(activeColor.ch1().intValue());
        }
        if (!bSlider.isDragging()) {
            bSlider.setValue(activeColor.ch2().intValue());
        }

        if (!rInput.isFocused()) {
            rInput.setText(String.valueOf(activeColor.ch0()));
        }
        if (!gInput.isFocused()) {
            gInput.setText(String.valueOf(activeColor.ch1()));
        }
        if (!bInput.isFocused()) {
            bInput.setText(String.valueOf(activeColor.ch2()));
        }

        if (!scrollBar.isFocused()) {
            scrollBar.setValue(0);
        }
        if (!hexInput.isFocused()) {
            hexInput.setText(activeColor.toHexString());
        }

        updateArmour();
        colorSort();

        if (colorWheelToggle.getToggle()) {
            RGB rgb = activeColor.toRGB();
            int val = Math.max(Math.max(rgb.red(), rgb.green()), rgb.blue());
            colorWheel.setOpaqueTint(new RGB(val, val, val).argb());
        }
    }

    private void updateArmour() {
        final String DISPLAY = "display";
        final String COLOR = "color";

        int hexInt = activeColor.argb();
        helmet.getOrCreateTagElement(DISPLAY).putInt(COLOR, hexInt);
        chestplate.getOrCreateTagElement(DISPLAY).putInt(COLOR, hexInt);
        leggings.getOrCreateTagElement(DISPLAY).putInt(COLOR, hexInt);
        boots.getOrCreateTagElement(DISPLAY).putInt(COLOR, hexInt);
        horse.getOrCreateTagElement(DISPLAY).putInt(COLOR, hexInt);
    }

    private void colorSort() {
        stacks.clear();
        Map<Block, Double> blockScores = new HashMap<>();

        ForgeRegistries.BLOCKS.forEach(block -> {
            List<SpriteDetails> sprites = blockSpriteMap.get(block);
            if (sprites == null || sprites.isEmpty()) {
                return;
            }

            double score = ColorScoring.score(activeColor, sprites);
            blockScores.put(block, score);

            if (block.getName().getString().toUpperCase().contains(searchField.getText().toUpperCase())) {
                stacks.add(new ItemStack(block));
            }
        });

        stacks.sort((a, b) -> {
            Block blA = Block.byItem(a.getItem());
            Block blB = Block.byItem(b.getItem());

            double dA = blockScores.getOrDefault(blA, Double.MAX_VALUE);
            double dB = blockScores.getOrDefault(blB, Double.MAX_VALUE);

            return Double.compare(dA, dB);
        });
        scrollBar.setMaxValue(stacks.size() / SLOTS_WIDTH + SLOTS_WIDTH);
        placeSlots();
    }

    private void placeSlots() {
        wColorGuiSlots.forEach(mainPanel::remove);

        int index = SLOTS_WIDTH * scrollBar.getValue();

        for (int j = 1; j < SLOTS_HEIGHT; j++) {
            for (int i = 0; i < SLOTS_WIDTH; i++) {
                if (index >= stacks.size()) {
                    break;
                }

                WColorGuiSlot slot = new WColorGuiSlot(stacks.get(index), this);

                if (wColorGuiSlots.size() <= index) {
                    wColorGuiSlots.add(slot);
                } else {
                    wColorGuiSlots.set(index, slot);
                }

                mainPanel.add(slot, i, j);
                index++;
            }
        }

        mainPanel.validate(this);
    }

    private void toggleColorWheel(boolean isToggled) {
        if (isToggled) {
            mainPanel.remove(sliderArea);

            mainPanel.remove(rLabel);
            mainPanel.remove(gLabel);
            mainPanel.remove(bLabel);
            mainPanel.add(rLabel, 1, 1, 1, 1);
            mainPanel.add(gLabel, 1, 1, 1, 1);
            mainPanel.add(bLabel, 1, 1, 1, 1);

            mainPanel.remove(inputs);
            mainPanel.add(inputs, 10, 9, 2, 1);

            mainPanel.remove(armourSlots);
            mainPanel.add(colorWheel, 11, 2, 6, 6);
            mainPanel.add(wheelValueSlider, 17, 2, 1, 6);

            wheelValueSlider.setValue(wheelValueSlider.getMaxValue());
            colorWheel.setLocation(198, 47);
            wheelValueSlider.setLocation(314, 47);
            wheelValueSlider.setSize(18, 128);

            rLabel.setLocation(211, 165);
            gLabel.setLocation(247, 165);
            bLabel.setLocation(283, 165);
        } else {
            mainPanel.add(sliderArea, 11, 2, 6, 7);
            mainPanel.add(armourSlots, 17, 3);
            mainPanel.remove(colorWheel);
            mainPanel.remove(wheelValueSlider);

            rLabel.setLocation(211, 50);
            gLabel.setLocation(247, 50);
            bLabel.setLocation(283, 50);
        }

        root.validate(this);
    }

}
