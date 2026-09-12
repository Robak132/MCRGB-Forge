package io.github.robak132.mcrgb_forge.client.gui;

import static io.github.robak132.mcrgb_forge.MCRGBMod.MOD_ID;

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
import io.github.robak132.mcrgb_forge.client.analysis.Sprite;
import io.github.robak132.mcrgb_forge.client.analysis.SpriteDetails;
import io.github.robak132.mcrgb_forge.client.analysis.TextureNoise;
import io.github.robak132.mcrgb_forge.client.gui.widgets.WBlockInfoBox;
import io.github.robak132.mcrgb_forge.client.gui.widgets.WButtonWithTooltip;
import io.github.robak132.mcrgb_forge.client.gui.widgets.WTextureThumbnail;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class BlockGuiDescription extends AbstractGuiDescription {

    private final List<Sprite> sprites;
    private final Map<String, TextureNoise> spriteNoise = new HashMap<>();
    private final int initialColor;
    private final TextureNoise initialNoise;
    private final int returnScrollPosition;
    private WPickableTexture blockTexture;
    private TextureNoise selectedNoise;
    private int selectedSpriteIndex;

    public BlockGuiDescription(ItemStack stack, RGB launchColor) {
        this(stack, launchColor, TextureNoise.NONE, 0);
    }

    public BlockGuiDescription(ItemStack stack, RGB launchColor, TextureNoise launchNoise) {
        this(stack, launchColor, launchNoise, 0);
    }

    public BlockGuiDescription(
            ItemStack stack, RGB launchColor, TextureNoise launchNoise, int returnScrollPosition) {
        initialColor = launchColor.rgb();
        initialNoise = launchNoise == null ? TextureNoise.NONE : launchNoise;
        selectedNoise = initialNoise;
        this.returnScrollPosition = returnScrollPosition;
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

        WBlockInfoBox infoBox = new WBlockInfoBox(Direction.Plane.VERTICAL, stack.getItem(), this::setColorAndNoise);
        WScrollPanel infoScrollPanel = new WScrollPanel(infoBox);

        mainPanel.add(infoScrollPanel, 11, 3, 7, 9);
        infoScrollPanel.setLocation(infoScrollPanel.getAbsoluteX(), infoScrollPanel.getAbsoluteY() + 4);
        mainPanel.add(savedPalettesArea, 0, 7);

        setColor(launchColor);

        var block = ((BlockItem) stack.getItem()).getBlock();
        Map<Block, List<SpriteDetails>> scan = MCRGBClient.getLastScan();
        List<SpriteDetails> details = scan == null ? null : scan.get(block);
        if (details != null) {
            details.forEach(detail -> spriteNoise.put(detail.getName(), detail.getNoise()));
        }
        sprites = Sprite.getSprites(block).stream().toList();
        if (sprites.isEmpty()) {
            return;
        }

        WGridPanel textureThumbnails = new WGridPanel();
        for (int i = 0; i < sprites.size(); i++) {
            WTextureThumbnail thumbnail = new WTextureThumbnail(sprites.get(i).atlasLocation(), sprites.get(i).u0(),
                    sprites.get(i).v0(), sprites.get(i).u1(), sprites.get(i).v1(), i, this::changeSprite);
            thumbnail.setOpaqueTint(sprites.get(i).tint());
            textureThumbnails.add(thumbnail, i % 3, Math.floorDiv(i, 3));
        }
        WScrollPanel textureScrollPanel = new WScrollPanel(textureThumbnails);
        Sprite firstSprite = sprites.get(0);
        blockTexture = new WPickableTexture(firstSprite.atlasLocation(), firstSprite.u0(), firstSprite.v0(),
                firstSprite.u1(), firstSprite.v1());
        blockTexture.setOpaqueTint(firstSprite.tint());
        blockTexture.setColorPickListener(this::onColorPicked);
        mainPanel.add(blockTexture, 0, 1, 6, 6);
        mainPanel.add(textureScrollPanel, 7, 1, 4, 6);
        root.validate(this);
    }

    public void changeSprite(int i) {
        selectedSpriteIndex = i;
        blockTexture.setImage(sprites.get(i).atlasLocation());
        blockTexture.setUv(sprites.get(i).u0(), sprites.get(i).v0(), sprites.get(i).u1(),
                sprites.get(i).v1());
        blockTexture.setOpaqueTint(sprites.get(i).tint());
        root.validate(this);
    }

    @Override
    public void onColorPicked(int color, int button) {
        super.onColorPicked(color, button);
        if (button == 0 || button == 1) {
            ResourceLocation spriteName = sprites.get(selectedSpriteIndex).name();
            TextureNoise noise = spriteNoise.get(spriteName.toString());
            if (noise != null) {
                selectedNoise = noise;
            }
        }
    }

    private void setColorAndNoise(int color, TextureNoise noise) {
        setColor(new RGB(color));
        if (noise != null) {
            selectedNoise = noise;
        }
    }

    public void onHexEntered(String value) {
        Integer valueInt = normalizeHexInput(value);
        if (valueInt != null) {
            setColor(new RGB(valueInt));
        }
    }

    TextureNoise getTextureNoiseTarget() {
        return selectedNoise;
    }

    void back() {
        int scrollPosition = activeColor.toRGB().rgb() == initialColor && selectedNoise.equals(initialNoise)
                ? returnScrollPosition
                : 0;
        Minecraft.getInstance().setScreen(
                new MCRGBClientScreen(
                        new ColorsGuiDescription(
                                activeColor.toRGB(), selectedNoise, MCRGBClient.getLastScan(), scrollPosition)));
    }
}
