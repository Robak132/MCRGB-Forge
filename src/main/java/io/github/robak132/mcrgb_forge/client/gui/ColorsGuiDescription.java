package io.github.robak132.mcrgb_forge.client.gui;

import static io.github.robak132.mcrgb_forge.MCRGBMod.MOD_ID;
import static io.github.robak132.mcrgb_forge.client.utils.Utils.stringToInt;
import static io.github.robak132.mcrgb_forge.config.MCRGBConfig.EMI_LOADED;
import static io.github.robak132.mcrgb_forge.config.MCRGBConfig.SLIDER_CONSTANT_UPDATE;

import io.github.robak132.libgui_forge.widget.WButton;
import io.github.robak132.libgui_forge.widget.WColorWheel;
import io.github.robak132.libgui_forge.widget.WGradientSlider;
import io.github.robak132.libgui_forge.widget.WGridPanel;
import io.github.robak132.libgui_forge.widget.WLabel;
import io.github.robak132.libgui_forge.widget.WPlainPanel;
import io.github.robak132.libgui_forge.widget.WSlider;
import io.github.robak132.libgui_forge.widget.WTextField;
import io.github.robak132.libgui_forge.widget.WToggleButton;
import io.github.robak132.libgui_forge.widget.WWidget;
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
import io.github.robak132.mcrgb_forge.client.analysis.TextureNoise;
import io.github.robak132.mcrgb_forge.client.gui.widgets.WButtonWithTooltip;
import io.github.robak132.mcrgb_forge.client.gui.widgets.WColorGuiHotbar;
import io.github.robak132.mcrgb_forge.client.gui.widgets.WColorGuiSlot;
import io.github.robak132.mcrgb_forge.client.gui.widgets.WColorScrollBar;
import io.github.robak132.mcrgb_forge.client.gui.widgets.WSmartTextField;
import io.github.robak132.mcrgb_forge.client.integration.EmiIntegration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class ColorsGuiDescription extends AbstractGuiDescription {

    private static final Pattern METRIC_INPUT_PATTERN = Pattern.compile("\\d{0,3}");
    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 250;
    private static final int PANEL_HALF_WIDTH = PANEL_WIDTH / 2;
    private static final int COLOR_SLIDERS_X = PANEL_HALF_WIDTH;
    private static final int CHANNEL_LABEL_Y = 56;
    private static final int WHEEL_LABEL_Y = 136;
    private static final int CHANNEL_VALUE_Y = 178;
    private static final int SLIDER_TOP_Y = 72;
    private static final int SLIDER_HEIGHT = 93;
    private static final int SLIDER_AREA_WIDTH = 180;
    private static final int COLOR_MODEL_BUTTON_WIDTH = 30;
    private static final int COLOR_MODEL_BUTTON_Y = 205;
    private static final int HOTBAR_GRID_Y = SLOTS_HEIGHT + 5;
    private static final int NOISE_SLIDER_X = 308;
    private static final int SPATIAL_SLIDER_X = 344;
    private static final int SLIDER_SCROLL_STEP = 10;
    private static final int FILTER_LABEL_WIDTH = 36;
    private static final int METRIC_INPUT_WIDTH = 26;
    private static final int RIGHT_COLUMN_GRID_X = 21;
    private static final int COLOR_WHEEL_Y = 100;
    private static final int COLOR_WHEEL_SIZE = 64;
    private static final int SLOW_GUI_REFRESH_MILLIS = 50;
    private static String rememberedSearch = "";

    private final List<ItemStack> stacks = new ArrayList<>();
    private final List<WColorGuiSlot> wColorGuiSlots = new ArrayList<>();
    private Map<Block, List<SpriteDetails>> blockSpriteMap;
    private final WLabel rLabel = new WLabel(Component.translatable(Localisation.UI_R), 0xFFFF0000);
    private final WSlider rSlider = new WSlider(0, 255, Direction.Plane.VERTICAL);
    private final WSmartTextField rInput = new WSmartTextField(Component.empty());
    private final WLabel gLabel = new WLabel(Component.translatable(Localisation.UI_G), 0xFF00FF00);
    private final WSlider gSlider = new WSlider(0, 255, Direction.Plane.VERTICAL);
    private final WSmartTextField gInput = new WSmartTextField(Component.empty());
    private final WLabel bLabel = new WLabel(Component.translatable(Localisation.UI_B), 0xFF0000FF);
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
    private final WLabel blockProgress = new WLabel(Component.empty());
    private final WSlider noiseSlider = new WSlider(0, 100, Direction.Plane.VERTICAL);
    private final WSlider spatialSlider = new WSlider(0, 100, Direction.Plane.VERTICAL);
    private final List<WSlider> sliders = List.of(rSlider, gSlider, bSlider, noiseSlider, spatialSlider);
    private final WSmartTextField noiseInput = new WSmartTextField();
    private final WSmartTextField spatialInput = new WSmartTextField();
    private final WPlainPanel sliderArea = new WPlainPanel();
    private final WGridPanel armourSlots = new WGridPanel();
    private final WColorScrollBar scrollBar = new WColorScrollBar(this::placeSlots);
    private boolean initialized = false;
    private int targetNoise;
    private int targetSpatial;

    public ColorsGuiDescription(RGB launchColor, TextureNoise launchNoise,
            Map<Block, List<SpriteDetails>> blockSpriteMap) {
        this(launchColor, launchNoise, blockSpriteMap, 0);
    }

    ColorsGuiDescription(RGB launchColor, TextureNoise launchNoise,
            Map<Block, List<SpriteDetails>> blockSpriteMap, int launchScrollPosition) {
        this.blockSpriteMap = blockSpriteMap;

        WButtonWithTooltip refreshButton = new WButtonWithTooltip(
                new TextureIcon(ResourceLocation.fromNamespaceAndPath(MOD_ID, "refresh.png")),
                Component.translatable(Localisation.UI_REFRESH_INFO));

        setRootPanel(root);
        root.add(mainPanel, 0, 0);
        mainPanel.setSize(PANEL_WIDTH, PANEL_HEIGHT);
        mainPanel.setInsets(Insets.ROOT_PANEL);

        addColorSliders();
        mainPanel.add(hexInput, 11, 1, 5, 1);
        hexInput.setLocation(PANEL_HALF_WIDTH, hexInput.getY());
        configureTextureSlider(
                new WLabel(Component.translatable(Localisation.UI_NOISE)), noiseSlider, noiseInput, NOISE_SLIDER_X);
        configureTextureSlider(new WLabel(Component.translatable(Localisation.UI_SPATIAL)), spatialSlider,
                spatialInput, SPATIAL_SLIDER_X);
        mainPanel.add(colorDisplay, 16, 1, 2, 2);
        colorDisplay.setLocation(291, colorDisplay.getAbsoluteY() - 1);
        mainPanel.add(scrollBar, 9, 1, 1, SLOTS_HEIGHT - 1);

        mainPanel.add(refreshButton, RIGHT_COLUMN_GRID_X, 11, 1, 1);
        refreshButton.setLocation(373, refreshButton.getY());
        refreshButton.setSize(20, 20);
        refreshButton.setIconSize(18);
        refreshButton.setAlignment(HorizontalAlignment.LEFT);
        refreshButton.setOnClick(() -> {
            Minecraft.getInstance().setScreen(null);
            MCRGBClient.refreshScan();
        });

        mainPanel.add(searchField, 6, 0, 5, 1);
        searchField.setText(rememberedSearch);
        searchField.setChangedListener(value -> {
            rememberedSearch = value;
            colorSort();
        });
        mainPanel.add(blockProgress, 12, 0, 4, 1);
        blockProgress.setHorizontalAlignment(HorizontalAlignment.CENTER);
        updateBlockProgress();

        mainPanel.add(rgbButton, 10, 11, 1, 1);
        rgbButton.setLocation(194, COLOR_MODEL_BUTTON_Y);
        rgbButton.setSize(COLOR_MODEL_BUTTON_WIDTH, 20);
        rgbButton.setEnabled(false);
        rgbButton.setAlignment(HorizontalAlignment.CENTER);

        mainPanel.add(hsvButton, 13, 11, 1, 1);
        hsvButton.setLocation(230, COLOR_MODEL_BUTTON_Y);
        hsvButton.setSize(COLOR_MODEL_BUTTON_WIDTH, 20);
        hsvButton.setAlignment(HorizontalAlignment.CENTER);

        mainPanel.add(hslButton, 15, 11, 1, 1);
        hslButton.setLocation(266, COLOR_MODEL_BUTTON_Y);
        hslButton.setSize(COLOR_MODEL_BUTTON_WIDTH, 20);
        hslButton.setAlignment(HorizontalAlignment.CENTER);

        if (EMI_LOADED) {
            WButton emiSearchButton = new WButton(Component.translatable(Localisation.UI_EMI));
            mainPanel.add(emiSearchButton, 18, 1, 4, 1);
            emiSearchButton.setLocation(329, 24);
            emiSearchButton.setSize(64, 20);
            emiSearchButton.setAlignment(HorizontalAlignment.CENTER);
            emiSearchButton.setOnClick(() -> EmiIntegration.openColorSearch(
                    activeColor.toHexString(), targetNoise, targetSpatial, searchField.getText()));
        }

        rgbButton.setOnClick(() -> setColor(ColorModel.RGB));
        hsvButton.setOnClick(() -> setColor(ColorModel.HSV));
        hslButton.setOnClick(() -> setColor(ColorModel.HSL));

        mainPanel.add(new WLabel(Component.translatable(Localisation.UI_HEADER)), 0, 0, 6, 1);
        mainPanel.add(savedPalettesArea, 0, SLOTS_HEIGHT);
        WColorGuiHotbar hotbar = new WColorGuiHotbar(this);
        mainPanel.add(hotbar, 0, HOTBAR_GRID_Y, 9, 1);
        mainPanel.add(rLabel, 6, 7, 1, 1);
        mainPanel.add(gLabel, 6, 7, 1, 1);
        mainPanel.add(bLabel, 6, 7, 1, 1);

        rLabel.setLocation(COLOR_SLIDERS_X, CHANNEL_LABEL_Y);
        gLabel.setLocation(COLOR_SLIDERS_X + 36, CHANNEL_LABEL_Y);
        bLabel.setLocation(COLOR_SLIDERS_X + 72, CHANNEL_LABEL_Y);
        rLabel.setHorizontalAlignment(HorizontalAlignment.CENTER);
        gLabel.setHorizontalAlignment(HorizontalAlignment.CENTER);
        bLabel.setHorizontalAlignment(HorizontalAlignment.CENTER);

        sliderArea.add(rSlider, 0, 0, 18, SLIDER_HEIGHT);
        sliderArea.add(gSlider, 36, 0, 18, SLIDER_HEIGHT);
        sliderArea.add(bSlider, 72, 0, 18, SLIDER_HEIGHT);

        WPlainPanel inputs = new WPlainPanel();
        mainPanel.add(inputs, 10, 9, 2, 1);
        inputs.setLocation(COLOR_SLIDERS_X - 4, CHANNEL_VALUE_Y);
        inputs.add(rInput, 0, 0, METRIC_INPUT_WIDTH, WTextField.DEFAULT_HEIGHT);
        inputs.add(gInput, 36, 0, METRIC_INPUT_WIDTH, WTextField.DEFAULT_HEIGHT);
        inputs.add(bInput, 72, 0, METRIC_INPUT_WIDTH, WTextField.DEFAULT_HEIGHT);

        List<WSlider> colorSliders = List.of(rSlider, gSlider, bSlider);
        for (WSlider slider : colorSliders) {
            slider.setValueChangeListener(this::onColorSliderValueChange);
        }
        for (WSlider slider : sliders) {
            slider.setDraggingFinishedListener(this::onSliderInteractionFinished);
        }

        noiseSlider.setValueChangeListener(value -> {
            targetNoise = value;
            noiseInput.setText(Integer.toString(value));
            if (SLIDER_CONSTANT_UPDATE.get()) {
                colorSort();
            }
        });
        spatialSlider.setValueChangeListener(value -> {
            targetSpatial = value;
            spatialInput.setText(Integer.toString(value));
            if (SLIDER_CONSTANT_UPDATE.get()) {
                colorSort();
            }
        });

        rInput.setCommitListener(this::onValueEntered);
        gInput.setCommitListener(this::onValueEntered);
        bInput.setCommitListener(this::onValueEntered);
        hexInput.setCommitListener(this::onHexEntered);
        noiseInput.setCommitListener(value -> onMetricEntered(noiseInput, noiseSlider));
        spatialInput.setCommitListener(value -> onMetricEntered(spatialInput, spatialSlider));

        mainPanel.add(armourSlots, RIGHT_COLUMN_GRID_X, 3);
        armourSlots.setLocation(375, armourSlots.getY());
        armourSlots.add(new WColorGuiSlot(helmet, this), 0, 0);
        armourSlots.add(new WColorGuiSlot(chestplate, this), 0, 1);
        armourSlots.add(new WColorGuiSlot(leggings, this), 0, 2);
        armourSlots.add(new WColorGuiSlot(boots, this), 0, 3);
        armourSlots.add(new WColorGuiSlot(horse, this), 0, 4);

        colorWheelToggle.setOffImage(new Texture(ResourceLocation.fromNamespaceAndPath(MOD_ID, "wheel_small.png")));
        colorWheelToggle.setOnImage(new Texture(ResourceLocation.fromNamespaceAndPath(MOD_ID, "sliders.png")));
        colorWheelToggle.setOnToggle(this::toggleColorWheel);
        mainPanel.add(colorWheelToggle, RIGHT_COLUMN_GRID_X, 10);
        colorWheelToggle.setLocation(375, 180);

        wheelValueSlider.setValueChangeListener(value -> {
            colorWheel.setOpaqueTint(new RGB(255, value, value, value).argb());
            colorWheel.pickAtCursor();
        });
        colorWheel.setColorPickListener(this::onColorPicked);

        mainPanel.validate(this);
        root.validate(this);

        Minecraft.getInstance().execute(() -> deferredInit(launchColor, launchNoise, launchScrollPosition));
    }

    private void configureTextureSlider(WLabel label, WSlider slider, WSmartTextField input, int x) {
        mainPanel.add(label, 0, 0);
        label.setLocation(x - (FILTER_LABEL_WIDTH - slider.getWidth()) / 2, CHANNEL_LABEL_Y);
        label.setSize(FILTER_LABEL_WIDTH, WTextField.DEFAULT_HEIGHT);
        label.setHorizontalAlignment(HorizontalAlignment.CENTER);

        mainPanel.add(slider, 0, 0);
        slider.setLocation(x, SLIDER_TOP_Y);
        slider.setSize(18, SLIDER_HEIGHT);

        mainPanel.add(input, 0, 0);
        input.setLocation(x - (METRIC_INPUT_WIDTH - slider.getWidth()) / 2, CHANNEL_VALUE_Y);
        input.setSize(METRIC_INPUT_WIDTH, WTextField.DEFAULT_HEIGHT);
        input.setEditable(true);
        input.setMaxLength(3);
        input.setTextPredicate(value -> METRIC_INPUT_PATTERN.matcher(value).matches());
    }

    private void addColorSliders() {
        mainPanel.add(sliderArea, 0, 0);
        sliderArea.setLocation(COLOR_SLIDERS_X, SLIDER_TOP_Y);
        sliderArea.setSize(SLIDER_AREA_WIDTH, SLIDER_HEIGHT);
    }

    private void deferredInit(RGB launchColor, TextureNoise launchNoise, int launchScrollPosition) {
        if (initialized) {
            return;
        }
        initialized = true;
        setTextureNoise(launchNoise == null ? TextureNoise.NONE : launchNoise);
        setColor(launchColor);
        scrollBar.setValue(launchScrollPosition);
        placeSlots();
    }

    public void setTextureNoise(TextureNoise noise) {
        if (noise == null) {
            return;
        }
        targetNoise = TextureNoise.clamp(noise.global());
        targetSpatial = TextureNoise.clamp(noise.kernel());
        noiseSlider.setValue(targetNoise);
        spatialSlider.setValue(targetSpatial);
        noiseInput.setText(Integer.toString(targetNoise));
        spatialInput.setText(Integer.toString(targetSpatial));
    }

    public TextureNoise getTextureNoiseTarget() {
        return new TextureNoise(targetNoise, targetSpatial);
    }

    public int getScrollPosition() {
        return scrollBar.getValue();
    }

    boolean mouseScrolled(int mouseX, int mouseY, double amount) {
        int increment = SLIDER_SCROLL_STEP * (int) Math.signum(amount);
        for (WSlider slider : sliders) {
            if (isMouseOver(slider, mouseX, mouseY)) {
                slider.setValue(slider.getValue() + increment, true);
                return true;
            }
        }
        if (mouseX >= mainPanel.getAbsoluteX()
                && mouseX < scrollBar.getAbsoluteX() + scrollBar.getWidth()
                && mouseY >= scrollBar.getAbsoluteY()
                && mouseY < scrollBar.getAbsoluteY() + scrollBar.getHeight()) {
            scrollBar.onMouseScroll(0, 0, amount);
            return true;
        }
        return false;
    }

    void updateScan(Map<Block, List<SpriteDetails>> blockSpriteMap) {
        this.blockSpriteMap = blockSpriteMap;
        updateBlockProgress();
        if (initialized) {
            colorSort();
        }
    }

    private void updateBlockProgress() {
        blockProgress.setText(Component.translatable(
                Localisation.UI_BLOCK_PROGRESS, blockSpriteMap.size(), MCRGBClient.getScanBlockCount()));
    }

    private boolean isMouseOver(WWidget widget, int mouseX, int mouseY) {
        return widget.isWithinBounds(mouseX - widget.getAbsoluteX(), mouseY - widget.getAbsoluteY());
    }

    @Override
    public void setColor(Color color, ColorModel model) {
        applyColor(color, model, true);
    }

    private void applyColor(Color color, ColorModel model, boolean sortResults) {
        super.setColor(color, model);
        lockWidgets(() -> {
            configureColorControls(activeColorModel);
            scrollBar.setValue(0);
            refreshComponents(sortResults);
        }, e -> log.error("Error refreshing color components", e));
    }

    private void configureColorControls(@NotNull ColorModel model) {
        String firstLabel;
        String secondLabel;
        String thirdLabel;
        int firstColor;
        int secondColor;
        int thirdColor;
        int firstMax;
        int secondMax;
        int thirdMax;

        switch (model) {
            case RGB -> {
                firstLabel = Localisation.UI_R;
                secondLabel = Localisation.UI_G;
                thirdLabel = Localisation.UI_B;
                firstColor = 0xFFFF0000;
                secondColor = 0xFF00FF00;
                thirdColor = 0xFF0000FF;
                firstMax = 255;
                secondMax = 255;
                thirdMax = 255;
            }
            case HSV, HSL -> {
                firstLabel = Localisation.UI_H;
                secondLabel = Localisation.UI_S;
                thirdLabel = model == ColorModel.HSV ? Localisation.UI_V : Localisation.UI_L;
                firstColor = 0xFF3F3F3F;
                secondColor = 0xFF3F3F3F;
                thirdColor = 0xFF3F3F3F;
                firstMax = 360;
                secondMax = 100;
                thirdMax = 100;
            }
            case LAB -> throw new IllegalArgumentException("OKLab is not supported by the color editor");
            default -> throw new IllegalArgumentException("Unsupported color model: " + model);
        }

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

        rgbButton.setEnabled(model != ColorModel.RGB);
        hsvButton.setEnabled(model != ColorModel.HSV);
        hslButton.setEnabled(model != ColorModel.HSL);
    }

    private void onColorSliderValueChange(int value) {
        applyColor(
                Color.create(activeColorModel, rSlider.getValue(), gSlider.getValue(), bSlider.getValue()),
                activeColorModel,
                SLIDER_CONSTANT_UPDATE.get());
    }

    private void onSliderInteractionFinished(int value) {
        if (!SLIDER_CONSTANT_UPDATE.get()) {
            colorSort();
        }
    }

    private void onMetricEntered(WSmartTextField input, WSlider slider) {
        Integer value = stringToInt(input.getText());
        if (value == null) {
            input.setText(Integer.toString(slider.getValue()));
            return;
        }
        int clamped = TextureNoise.clamp(value);
        input.setText(Integer.toString(clamped));
        slider.setValue(clamped, true);
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

    private void refreshComponents(boolean sortResults) {
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
        if (sortResults) {
            colorSort();
        }

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
        long started = System.nanoTime();
        stacks.clear();
        Map<Block, Double> blockScores = new HashMap<>();

        for (Map.Entry<Block, List<SpriteDetails>> entry : blockSpriteMap.entrySet()) {
            Block block = entry.getKey();
            List<SpriteDetails> sprites = entry.getValue();
            if (sprites == null || sprites.isEmpty()) {
                continue;
            }

            double score = ColorScoring.score(activeColor, sprites, targetNoise, targetSpatial);
            blockScores.put(block, score);

            if (block.getName().getString().toUpperCase().contains(searchField.getText().toUpperCase())) {
                stacks.add(new ItemStack(block));
            }
        }
        long scoringFinished = System.nanoTime();

        stacks.sort((a, b) -> {
            Block blA = Block.byItem(a.getItem());
            Block blB = Block.byItem(b.getItem());

            double dA = blockScores.getOrDefault(blA, Double.MAX_VALUE);
            double dB = blockScores.getOrDefault(blB, Double.MAX_VALUE);

            return Double.compare(dA, dB);
        });
        long sortingFinished = System.nanoTime();
        int rowCount = (stacks.size() + SLOTS_WIDTH - 1) / SLOTS_WIDTH;
        int visibleRows = SLOTS_HEIGHT - 1;
        scrollBar.setMaxValue(Math.max(0, rowCount - visibleRows));
        placeSlots();
        long finished = System.nanoTime();
        long totalMillis = (finished - started) / 1_000_000;
        long scoringMillis = (scoringFinished - started) / 1_000_000;
        long sortingMillis = (sortingFinished - scoringFinished) / 1_000_000;
        long layoutMillis = (finished - sortingFinished) / 1_000_000;
        log.debug("Color GUI refresh: {} ms total; scoring={} ms, sorting={} ms, layout={} ms, "
                        + "{} cached blocks, {} candidates",
                totalMillis, scoringMillis, sortingMillis, layoutMillis, blockSpriteMap.size(), stacks.size());
        if (totalMillis >= SLOW_GUI_REFRESH_MILLIS) {
            log.warn("Slow color GUI refresh: {} ms; scoring={} ms, sorting={} ms, layout={} ms, "
                            + "{} cached blocks, {} candidates",
                    totalMillis, scoringMillis, sortingMillis, layoutMillis, blockSpriteMap.size(), stacks.size());
        }
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

            mainPanel.remove(armourSlots);
            mainPanel.add(colorWheel, 11, 2, 6, 6);
            mainPanel.add(wheelValueSlider, 15, 2, 1, 6);

            wheelValueSlider.setValue(wheelValueSlider.getMaxValue());
            colorWheel.setLocation(COLOR_SLIDERS_X, COLOR_WHEEL_Y);
            colorWheel.setSize(COLOR_WHEEL_SIZE, COLOR_WHEEL_SIZE);
            wheelValueSlider.setLocation(COLOR_SLIDERS_X + 72, COLOR_WHEEL_Y);
            wheelValueSlider.setSize(18, COLOR_WHEEL_SIZE);

            rLabel.setLocation(COLOR_SLIDERS_X, WHEEL_LABEL_Y);
            gLabel.setLocation(COLOR_SLIDERS_X + 36, WHEEL_LABEL_Y);
            bLabel.setLocation(COLOR_SLIDERS_X + 72, WHEEL_LABEL_Y);
        } else {
            addColorSliders();
            mainPanel.add(armourSlots, RIGHT_COLUMN_GRID_X, 3);
            armourSlots.setLocation(375, armourSlots.getY());
            mainPanel.remove(colorWheel);
            mainPanel.remove(wheelValueSlider);

            rLabel.setLocation(COLOR_SLIDERS_X, CHANNEL_LABEL_Y);
            gLabel.setLocation(COLOR_SLIDERS_X + 36, CHANNEL_LABEL_Y);
            bLabel.setLocation(COLOR_SLIDERS_X + 72, CHANNEL_LABEL_Y);
        }

        root.validate(this);
    }

}
