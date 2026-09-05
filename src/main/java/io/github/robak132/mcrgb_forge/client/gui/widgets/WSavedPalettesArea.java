package io.github.robak132.mcrgb_forge.client.gui.widgets;

import static io.github.robak132.mcrgb_forge.MCRGBMod.MOD_ID;

import io.github.robak132.libgui_forge.client.BackgroundPainter;
import io.github.robak132.libgui_forge.widget.WButton;
import io.github.robak132.libgui_forge.widget.WColorSwatch;
import io.github.robak132.libgui_forge.widget.WLabel;
import io.github.robak132.libgui_forge.widget.WListPanel;
import io.github.robak132.libgui_forge.widget.WPlainPanel;
import io.github.robak132.libgui_forge.widget.data.HorizontalAlignment;
import io.github.robak132.libgui_forge.widget.data.VerticalAlignment;
import io.github.robak132.libgui_forge.widget.data.colors.RGB;
import io.github.robak132.libgui_forge.widget.icon.TextureIcon;
import io.github.robak132.mcrgb_forge.client.Localisation;
import io.github.robak132.mcrgb_forge.client.MCRGBClient;
import io.github.robak132.mcrgb_forge.client.analysis.Palette;
import io.github.robak132.mcrgb_forge.client.gui.AbstractGuiDescription;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class WSavedPalettesArea extends WPlainPanel {

    WLabel savedColorsLabel = new WLabel(Component.translatable(Localisation.UI_SAVED_COLORS));
    ResourceLocation colorIdentifier = ResourceLocation.fromNamespaceAndPath(MOD_ID, "square.png");
    List<WColorSwatch> savedColors = new ArrayList<>();
    ResourceLocation savePaletteIdentifier = ResourceLocation.fromNamespaceAndPath(MOD_ID, "save.png");
    TextureIcon savePaletteIcon = new TextureIcon(savePaletteIdentifier);
    WButton savePaletteButton = new WButton(savePaletteIcon);

    WPaletteWidget editingPalette = null;
    WListPanel<Palette, WPaletteWidget> paletteList;

    AbstractGuiDescription cg;

    public WSavedPalettesArea(AbstractGuiDescription gui, int slotsWidth, int slotsHeight) {
        this.cg = gui;
        BiConsumer<Palette, WPaletteWidget> configurator = (Palette p, WPaletteWidget pwig) -> {
            pwig.cg = cg;
            pwig.palette = p;
            pwig.buildPaletteWidget(cg);
            for (int i = 0; i < pwig.savedColors.size(); i++) {
                String hex = p.getColor(i).toHexString().replace("#", "");
                int c = Integer.parseInt(hex, 16);
                pwig.savedColors.get(i).setColor(c | 0xFF000000);
            }
        };

        this.add(savedColorsLabel, 0, slotsHeight, 2, 1);
        savedColorsLabel.setVerticalAlignment(VerticalAlignment.BOTTOM);

        for (int i = 0; i < slotsWidth; i++) {
            savedColors.add(new WColorSwatch(colorIdentifier,
                    () -> cg.activeColor.argb(), color -> cg.setColor(new RGB(color))));

            this.add(savedColors.get(i), i * 17, slotsHeight + 5, 18, 18);
        }

        this.add(savePaletteButton, slotsWidth * 18, slotsHeight + 1, 20, 20);
        savePaletteButton.setSize(20, 20);
        savePaletteButton.setIconSize(18);
        savePaletteButton.setAlignment(HorizontalAlignment.LEFT);

        paletteList = new WListPanel<>(MCRGBClient.getPalettes(), WPaletteWidget::new, configurator);

        paletteList.setBackgroundPainter(BackgroundPainter.createColorful(0x999999));
        paletteList.setListItemHeight(19);
        this.add(paletteList, 0, 2, 10, 3);
        paletteList.setLocation(0, 36);
        paletteList.setSize(10 * 18, (int) (2.8f * 18));

        savePaletteButton.setOnClick(this::savePalette);

    }

    Palette createPalette() {
        Palette newPallet = new Palette();
        for (WColorSwatch savedColor : savedColors) {
            newPallet.addColor(new RGB(savedColor.getColor()));
        }
        return newPallet;
    }

    void updatePalette(WPaletteWidget updatingPalette) {
        for (int i = 0; i < savedColors.size(); i++) {
            updatingPalette.savedColors.get(i).setColor(savedColors.get(i).getColor());
            updatingPalette.palette.setColor(i, new RGB(savedColors.get(i).getColor()));
        }
    }

    void savePalette() {
        if (editingPalette == null) {
            MCRGBClient.addPalette(createPalette());
        } else {
            updatePalette(editingPalette);
            editingPalette = null;
        }
        for (WColorSwatch savedColor : savedColors) {
            savedColor.setColor(0xffffffff);
        }
        MCRGBClient.savePalettes();
        cg.mainPanel.validate(cg);
    }

    public void deletePalette(WPaletteWidget pwig) {

        MCRGBClient.removePalette(pwig.palette);
        editingPalette = null;
        cg.root.validate(cg);
        MCRGBClient.savePalettes();
    }

    public void editPalette(WPaletteWidget pwig) {
        if (editingPalette == pwig) {
            editingPalette = null;
            for (WColorSwatch savedColor : savedColors) {
                savedColor.setColor(0xffffffff);
            }
            return;
        }
        for (int i = 0; i < savedColors.size(); i++) {
            savedColors.get(i).setColor(pwig.savedColors.get(i).getColor());
        }
        editingPalette = pwig;
    }
}
