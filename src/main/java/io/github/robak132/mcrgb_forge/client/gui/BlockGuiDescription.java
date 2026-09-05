package io.github.robak132.mcrgb_forge.client.gui;

import static io.github.robak132.mcrgb_forge.MCRGBMod.MOD_ID;
import static io.github.robak132.mcrgb_forge.client.analysis.ColorScanner.getSprites;

import io.github.robak132.libgui_forge.widget.WButton;
import io.github.robak132.libgui_forge.widget.WGridPanel;
import io.github.robak132.libgui_forge.widget.WLabel;
import io.github.robak132.libgui_forge.widget.WPickableTexture;
import io.github.robak132.libgui_forge.widget.WScrollPanel;
import io.github.robak132.libgui_forge.widget.data.HorizontalAlignment;
import io.github.robak132.libgui_forge.widget.data.Insets;
import io.github.robak132.libgui_forge.widget.data.colors.RGB;
import io.github.robak132.libgui_forge.widget.icon.TextureIcon;
import io.github.robak132.mcrgb_forge.client.Localisation;
import io.github.robak132.mcrgb_forge.client.MCRGBClient;
import io.github.robak132.mcrgb_forge.client.gui.widgets.WBlockInfoBox;
import io.github.robak132.mcrgb_forge.client.gui.widgets.WButtonWithTooltip;
import io.github.robak132.mcrgb_forge.client.gui.widgets.WTextureThumbnail;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class BlockGuiDescription extends AbstractGuiDescription {

    private final List<TextureAtlasSprite> sprites;
    private WPickableTexture blockTexture;

    public BlockGuiDescription(ItemStack stack, RGB launchColor) {
        ResourceLocation backIdentifier = ResourceLocation.fromNamespaceAndPath(MOD_ID, "back.png");
        TextureIcon backIcon = new TextureIcon(backIdentifier);
        WButton backButton = new WButtonWithTooltip(backIcon, Component.translatable(Localisation.UI_BACK_INFO));

        setRootPanel(root);
        root.add(mainPanel, 0, 0);
        mainPanel.setSize(320, 220);
        mainPanel.setInsets(Insets.ROOT_PANEL);
        mainPanel.add(hexInput, 11, 1, 5, 1);
        hexInput.setCommitListener(this::onHexEntered);
        mainPanel.add(colorDisplay, 16, 1, 2, 2);
        colorDisplay.setLocation(colorDisplay.getAbsoluteX() + 1, colorDisplay.getAbsoluteY() - 1);

        WLabel label = new WLabel(Component.translatable(Localisation.UI_HEADER));
        mainPanel.add(label, 0, 0, 2, 1);
        label.setText(stack.getHoverName());

        mainPanel.add(backButton, 17, 0, 1, 1);
        backButton.setSize(20, 20);
        backButton.setIconSize(18);
        backButton.setAlignment(HorizontalAlignment.LEFT);
        backButton.setOnClick(this::back);

        WBlockInfoBox infoBox = new WBlockInfoBox(Direction.Plane.VERTICAL, stack.getItem(), (color) -> this.setColor(new RGB(color)));
        WScrollPanel infoScrollPanel = new WScrollPanel(infoBox);

        mainPanel.add(infoScrollPanel, 11, 3, 7, 9);
        mainPanel.add(savedPalettesArea, 0, 7);

        setColor(launchColor);

        sprites = getSprites(((BlockItem) stack.getItem()).getBlock()).stream().toList();
        if (sprites.isEmpty()) {
            return;
        }

        WGridPanel textureThumbnails = new WGridPanel();
        for (int i = 0; i < sprites.size(); i++) {
            WTextureThumbnail thumbnail = new WTextureThumbnail(sprites.get(i).atlasLocation(), sprites.get(i).getU0(), sprites.get(i).getV0(),
                    sprites.get(i).getU1(), sprites.get(i).getV1(), i, this::changeSprite);
            textureThumbnails.add(thumbnail, i % 3, Math.floorDiv(i, 3));
        }
        WScrollPanel textureScrollPanel = new WScrollPanel(textureThumbnails);
        TextureAtlasSprite firstSprite = sprites.get(0);
        blockTexture = new WPickableTexture(firstSprite.atlasLocation(), firstSprite.getU0(), firstSprite.getV0(),
                firstSprite.getU1(), firstSprite.getV1());
        blockTexture.setColorPickListener(this::onColorPicked);
        mainPanel.add(blockTexture, 0, 1, 6, 6);
        mainPanel.add(textureScrollPanel, 7, 1, 4, 6);
        root.validate(this);
    }

    public void changeSprite(int i) {
        blockTexture.setImage(sprites.get(i).atlasLocation());
        blockTexture.setUv(sprites.get(i).getU0(), sprites.get(i).getV0(), sprites.get(i).getU1(), sprites.get(i).getV1());
        root.validate(this);
    }

    public void onHexEntered(String value) {
        Integer valueInt = normalizeHexInput(value);
        if (valueInt != null) {
            setColor(new RGB(valueInt));
        }
    }

    private void back() {
        Minecraft.getInstance().setScreen(new MCRGBClientScreen(new ColorsGuiDescription(activeColor.toRGB(), MCRGBClient.getLastScan())));
    }
}
