package io.github.robak132.mcrgb_forge.client.gui.widgets;

import static io.github.robak132.mcrgb_forge.MCRGBMod.MOD_ID;

import io.github.robak132.libgui_forge.client.BackgroundPainter;
import io.github.robak132.libgui_forge.widget.WColorSwatch;
import io.github.robak132.libgui_forge.widget.WPlainPanel;
import io.github.robak132.libgui_forge.widget.data.HorizontalAlignment;
import io.github.robak132.libgui_forge.widget.data.colors.RGB;
import io.github.robak132.libgui_forge.widget.icon.TextureIcon;
import io.github.robak132.mcrgb_forge.client.Localisation;
import io.github.robak132.mcrgb_forge.client.analysis.Palette;
import io.github.robak132.mcrgb_forge.client.gui.AbstractGuiDescription;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class WPaletteWidget extends WPlainPanel {

    private static final int SLOTS_WIDTH = 9;
    private static final int EDITING_BORDER_COLOR = 0xFF00AA00;

    final List<WColorSwatch> savedColors = new ArrayList<>();
    private final WButtonWithTooltip editButton = new WButtonWithTooltip(
            new TextureIcon(ResourceLocation.fromNamespaceAndPath(MOD_ID, "edit.png")),
            Component.translatable(Localisation.UI_EDIT_PALETTE_INFO));
    private final WButtonWithTooltip deleteButton = new WButtonWithTooltip(createDeleteIcon(),
            Component.translatable(Localisation.UI_DELETE_PALETTE_INFO));
    Palette palette;
    private AbstractGuiDescription cg;
    private boolean built;

    private static TextureIcon createDeleteIcon() {
        TextureIcon icon = new TextureIcon(ResourceLocation.fromNamespaceAndPath(MOD_ID, "delete.png"));
        icon.setColor(0xFF_FC5454);
        return icon;
    }

    public void buildPaletteWidget(AbstractGuiDescription cg) {
        this.cg = cg;
        if (built) {
            return;
        }
        built = true;

        this.setBackgroundPainter(BackgroundPainter.createColorful(0xFFFFFF));
        for (int i = 0; i < SLOTS_WIDTH; i++) {
            savedColors.add(new WColorSwatch(ResourceLocation.fromNamespaceAndPath(MOD_ID, "square.png"),
                    () -> this.cg.activeColor.argb(), color -> this.cg.setColor(new RGB(color))));
            savedColors.get(i).setInteractable(false);
            this.add(savedColors.get(i), i * 17, 0, 18, 18);
        }
        this.add(editButton, (int) (8.6f * 18), 0, 10, 10);
        editButton.setSize(10, 10);
        editButton.setIconSize(9);
        editButton.setAlignment(HorizontalAlignment.LEFT);
        editButton.setOnClick(() -> this.cg.savedPalettesArea.editPalette(this));

        this.add(deleteButton, (int) (8.6f * 18), 9, 1, 1);
        deleteButton.setSize(10, 10);
        deleteButton.setIconSize(9);
        deleteButton.setAlignment(HorizontalAlignment.LEFT);
        deleteButton.setOnClick(() -> this.cg.savedPalettesArea.deletePalette(this));
    }

    @Override
    public void paint(GuiGraphics context, int x, int y, int mouseX, int mouseY) {
        super.paint(context, x, y, mouseX, mouseY);
        if (cg != null && cg.savedPalettesArea.editingPalette == this) {
            context.fill(x, y, x + width, y + 1, EDITING_BORDER_COLOR);
            context.fill(x, y + height - 1, x + width, y + height, EDITING_BORDER_COLOR);
            context.fill(x, y + 1, x + 1, y + height - 1, EDITING_BORDER_COLOR);
            context.fill(x + width - 1, y + 1, x + width, y + height - 1, EDITING_BORDER_COLOR);
        }
    }
}
