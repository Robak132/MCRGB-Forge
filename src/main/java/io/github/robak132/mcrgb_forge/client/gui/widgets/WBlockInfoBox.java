package io.github.robak132.mcrgb_forge.client.gui.widgets;

import io.github.robak132.libgui_forge.client.BackgroundPainter;
import io.github.robak132.libgui_forge.widget.WBox;
import io.github.robak132.libgui_forge.widget.WClickableLabel;
import io.github.robak132.libgui_forge.widget.data.InputResult;
import io.github.robak132.libgui_forge.widget.data.Insets;
import io.github.robak132.libgui_forge.widget.data.colors.RGB;
import io.github.robak132.mcrgb_forge.client.ColorInfoLines;
import io.github.robak132.mcrgb_forge.client.Localisation;
import io.github.robak132.mcrgb_forge.client.MCRGBClient;
import io.github.robak132.mcrgb_forge.client.analysis.SpriteColor;
import io.github.robak132.mcrgb_forge.client.analysis.SpriteDetails;
import io.github.robak132.mcrgb_forge.client.analysis.TextureNoise;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class WBlockInfoBox extends WBox {

    int lineCount = 0;

    public WBlockInfoBox(Direction.Plane axis, Item item, BiConsumer<Integer, TextureNoise> onClick) {
        super(axis);
        setInsets(Insets.ROOT_PANEL);
        Block block = Block.byItem(item);

        Map<Block, List<SpriteDetails>> scan = MCRGBClient.getLastScan();
        if (scan == null) {
            return;
        }

        List<SpriteDetails> list = scan.get(block);
        if (list == null || list.isEmpty()) {
            return;
        }

        for (SpriteDetails details : list) {
            List<Component> lines = ColorInfoLines.linesFor(details);
            for (int j = 0; j < lines.size(); j++) {
                Component line = lines.get(j);
                Font textRenderer = Minecraft.getInstance().font;
                int width = textRenderer.width(line);
                WClickableLabel newLabel = new WClickableLabel(line, createHoveredText(line));
                if (ColorInfoLines.isColorLine(details, j)) {
                    SpriteColor spriteColor = details.getColors()
                            .get(j - ColorInfoLines.firstColorLineIndex(details));
                    int color = spriteColor.color().argb();
                    newLabel.setOnClick(button -> {
                        if (button == 2) {
                            copyColorToClipboard(color);
                        } else {
                            onClick.accept(color, details.getNoise());
                        }
                        return InputResult.PROCESSED;
                    });
                }
                add(newLabel, width, 1);
                lineCount++;
            }
        }
        setSize(10, this.getWidth());
    }

    private static Component createHoveredText(Component text) {
        List<Component> styledComponents = text.toFlatList(Style.EMPTY.withItalic(true).withUnderlined(true));
        List<Component> baseComponents = text.toFlatList(Style.EMPTY);
        if (styledComponents.isEmpty() || baseComponents.isEmpty()) {
            return text;
        }

        styledComponents.set(0, baseComponents.get(0));
        MutableComponent hoveredText = Component.empty();
        styledComponents.forEach(hoveredText::append);
        return hoveredText;
    }

    private static void copyColorToClipboard(int color) {
        String hex = new RGB(color).toHexString();
        Minecraft.getInstance().keyboardHandler.setClipboard(hex);
        MCRGBClient.showToast(Component.translatable(Localisation.TOAST_COPIED_HEX_TO_CLIPBOARD)
                .append(Component.literal("⬛").withStyle(Style.EMPTY.withColor(color)))
                .append(Component.literal(hex)));
    }

    @Override
    public void paint(GuiGraphics context, int x, int y, int mouseX, int mouseY) {
        setBackgroundPainter(BackgroundPainter.VANILLA);
        super.paint(context, x, y, mouseX, mouseY);
    }
}
