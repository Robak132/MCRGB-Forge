package io.github.robak132.mcrgb_forge.client.gui;

import static io.github.robak132.mcrgb_forge.MCRGBMod.MOD_ID;
import static io.github.robak132.mcrgb_forge.client.utils.Utils.hexToInt;

import io.github.robak132.libgui_forge.gui.LightweightGuiDescription;
import io.github.robak132.libgui_forge.widget.WGridPanel;
import io.github.robak132.libgui_forge.widget.WSprite;
import io.github.robak132.libgui_forge.widget.data.colors.Color;
import io.github.robak132.libgui_forge.widget.data.colors.Color.ColorModel;
import io.github.robak132.libgui_forge.widget.data.colors.RGB;
import io.github.robak132.mcrgb_forge.client.Localisation;
import io.github.robak132.mcrgb_forge.client.MCRGBClient;
import io.github.robak132.mcrgb_forge.client.gui.widgets.WSavedPalettesArea;
import io.github.robak132.mcrgb_forge.client.gui.widgets.WSmartTextField;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractGuiDescription extends LightweightGuiDescription {

    static final int SLOTS_HEIGHT = 7;
    static final int SLOTS_WIDTH = 9;
    private static final Pattern HEX_INPUT_PATTERN = Pattern.compile("#?[0-9A-Fa-f]{0,6}");
    public final WGridPanel root = new WGridPanel();
    public final WGridPanel mainPanel = new WGridPanel();
    public final WSavedPalettesArea savedPalettesArea = new WSavedPalettesArea(this, SLOTS_WIDTH, SLOTS_HEIGHT);
    public final WSmartTextField hexInput = new WSmartTextField(Component.literal("#FFFFFF"));
    public final WSprite colorDisplay = new WSprite(ResourceLocation.fromNamespaceAndPath(MOD_ID, "rect.png"));
    public ItemStack cursorStack = ItemStack.EMPTY;
    public Color activeColor = new RGB(255, 255, 255);
    public ColorModel activeColorModel = ColorModel.RGB;
    protected boolean refreshing = false;

    protected AbstractGuiDescription() {
        hexInput.setMaxLength(7);
        hexInput.setTextPredicate(value -> HEX_INPUT_PATTERN.matcher(value).matches());
        hexInput.setChangedListener(value -> {
            String uppercase = value.toUpperCase(Locale.ROOT);
            if (!uppercase.equals(value)) {
                hexInput.setText(uppercase);
            }
        });
    }

    public void setColor(Color color) {
        setColor(color, this.activeColorModel);
    }

    public void setColor(ColorModel model) {
        setColor(this.activeColor, model);
    }

    public void setColor(Color color, ColorModel model) {
        lockWidgets(() -> {
            this.activeColorModel = model;
            this.activeColor = color.toModel(this.activeColorModel);
            this.colorDisplay.setOpaqueTint(color.argb());
            this.hexInput.setText(activeColor.toHexString());
        });
    }

    protected Integer normalizeHexInput(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.toUpperCase(Locale.ROOT);
        if (!normalized.startsWith("#")) {
            normalized = "#" + normalized;
        }
        hexInput.setText(normalized);
        return hexToInt(normalized);
    }

    public void onColorPicked(int color, int button) {
        RGB rgb = new RGB(color);
        if (button == 2) {
            String hex = rgb.toHexString();
            Minecraft.getInstance().keyboardHandler.setClipboard(hex);
            MCRGBClient.showToast(
                    Component.translatable(Localisation.TOAST_COPIED_HEX_TO_CLIPBOARD)
                            .append(Component.literal("⬛").withStyle(Style.EMPTY.withColor(color)))
                            .append(Component.literal(hex)));
        } else if (button == 0 || button == 1) {
            setColor(rgb);
        }
    }

    protected void lockWidgets(Runnable action) {
        lockWidgets(action, null);
    }

    protected void lockWidgets(Runnable action, Consumer<Exception> exceptionHandler) {
        if (refreshing) {
            return;
        }
        refreshing = true;
        try {
            action.run();
        } catch (Exception e) {
            if (exceptionHandler != null) {
                exceptionHandler.accept(e);
            }
        } finally {
            refreshing = false;
        }
    }
}
