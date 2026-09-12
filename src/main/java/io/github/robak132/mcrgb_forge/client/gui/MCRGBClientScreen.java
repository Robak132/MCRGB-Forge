package io.github.robak132.mcrgb_forge.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.robak132.libgui_forge.client.CottonClientScreen;
import io.github.robak132.libgui_forge.widget.WTextField;
import io.github.robak132.libgui_forge.widget.data.colors.RGB;
import io.github.robak132.mcrgb_forge.client.Localisation;
import io.github.robak132.mcrgb_forge.client.MCRGBClient;
import io.github.robak132.mcrgb_forge.client.analysis.SpriteDetails;
import io.github.robak132.mcrgb_forge.client.utils.Utils;
import java.util.List;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class MCRGBClientScreen extends CottonClientScreen {

    private final AbstractGuiDescription mcrgbDescription;

    public MCRGBClientScreen(AbstractGuiDescription description) {
        super(description);
        this.mcrgbDescription = description;
    }

    public void updateScan(Map<Block, List<SpriteDetails>> scan) {
        if (mcrgbDescription instanceof ColorsGuiDescription colorsDescription) {
            colorsDescription.updateScan(scan);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        if (!mcrgbDescription.cursorStack.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            graphics.renderItem(mcrgbDescription.cursorStack, mouseX - 8, mouseY - 8);
            graphics.renderItemDecorations(Minecraft.getInstance().font, mcrgbDescription.cursorStack, mouseX - 8,
                    mouseY - 8);
            graphics.pose().popPose();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (new KeyMapping(Localisation.KEY_QUICK_SEARCH, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O,
                Localisation.KEY_CATEGORY).matches(keyCode, scanCode)
                && !(mcrgbDescription.getFocus() instanceof WTextField)) {
            Integer color = Utils.hexToInt(Minecraft.getInstance().keyboardHandler.getClipboard());
            if (color != null) {
                mcrgbDescription.setColor(new RGB(color));
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mcrgbDescription instanceof ColorsGuiDescription colorsDescription
                && colorsDescription.mouseScrolled((int) mouseX - left, (int) mouseY - top, amount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void onClose() {
        if (mcrgbDescription instanceof BlockGuiDescription blockDescription) {
            blockDescription.back();
            return;
        }
        super.onClose();
    }

    @Override
    public void removed() {
        if (mcrgbDescription instanceof ColorsGuiDescription colorsDescription) {
            MCRGBClient.rememberPickerState(
                    colorsDescription.activeColor.toRGB(),
                    colorsDescription.getTextureNoiseTarget());
        } else if (mcrgbDescription instanceof BlockGuiDescription blockDescription) {
            MCRGBClient.rememberPickerState(
                    blockDescription.activeColor.toRGB(), blockDescription.getTextureNoiseTarget());
        }
        super.removed();
    }
}
