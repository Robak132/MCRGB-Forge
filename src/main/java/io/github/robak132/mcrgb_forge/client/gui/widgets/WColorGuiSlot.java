package io.github.robak132.mcrgb_forge.client.gui.widgets;

import io.github.robak132.libgui_forge.LibGui;
import io.github.robak132.libgui_forge.client.ScreenDrawing;
import io.github.robak132.libgui_forge.widget.TooltipBuilder;
import io.github.robak132.libgui_forge.widget.WWidget;
import io.github.robak132.libgui_forge.widget.data.InputResult;
import io.github.robak132.mcrgb_forge.client.ColorInfoLines;
import io.github.robak132.mcrgb_forge.client.Localisation;
import io.github.robak132.mcrgb_forge.client.MCRGBClient;
import io.github.robak132.mcrgb_forge.client.analysis.SpriteDetails;
import io.github.robak132.mcrgb_forge.client.gui.BlockGuiDescription;
import io.github.robak132.mcrgb_forge.client.gui.ColorsGuiDescription;
import io.github.robak132.mcrgb_forge.client.gui.MCRGBClientScreen;
import io.github.robak132.mcrgb_forge.config.MCRGBConfig;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

@Setter
public class WColorGuiSlot extends WWidget {

    public static final ResourceLocation SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(LibGui.MOD_ID,
            "textures/widget/item_slot.png");
    private final ColorsGuiDescription parentGui;
    private final int hotbarSlot;
    private ItemStack stack;

    public WColorGuiSlot(ItemStack stack, ColorsGuiDescription parentGui) {
        this(stack, parentGui, -1);
    }

    public WColorGuiSlot(ItemStack stack, ColorsGuiDescription parentGui, int hotbarSlot) {
        this.stack = stack;
        this.parentGui = parentGui;
        this.hotbarSlot = hotbarSlot;
    }

    @Override
    public void paint(GuiGraphics context, int x, int y, int mouseX, int mouseY) {
        ScreenDrawing.texturedRect(context, x, y, 18, 18, SLOT_TEXTURE, 0, 0, .28125f, .28125f, 0xFFFFFFFF);
        if (!stack.isEmpty()) {
            context.renderItem(stack, x + 1, y + 1);
            context.renderItemDecorations(Minecraft.getInstance().font, stack, x + 1, y + 1);
        }
    }

    @Override
    public InputResult onClick(int x, int y, int button) {
        LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);

        if (button == 1) {
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                Minecraft.getInstance().setScreen(
                        new MCRGBClientScreen(new BlockGuiDescription(
                                stack, parentGui.activeColor.toRGB(),
                                parentGui.getTextureNoiseTarget(), parentGui.getScrollPosition())));
            }
            return InputResult.PROCESSED;
        }

        if (button != 0 && button != 2) {
            return InputResult.PROCESSED;
        }

        switch (MCRGBConfig.ITEM_SPAWNING_MODE.get()) {
            case CREATIVE_DRAG -> handleCreativeTransfer(player, button == 2);
            case GIVE_COMMAND -> giveWithCommand(player, button == 2);
            case VIEW_ONLY -> {
                // Intentionally leave item interaction disabled.
            }
        }
        return InputResult.PROCESSED;
    }

    private void giveWithCommand(LocalPlayer player, boolean fullStack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!((player.hasPermissions(2) && player.isCreative()) || MCRGBConfig.BYPASS_OP.get())) {
            return;
        }

        String nbt = "";
        if (stack.hasTag()) {
            nbt = stack.getOrCreateTag().toString();
        }
        String command = MCRGBConfig.GIVE_COMMAND.get()
                .replace("%c", nbt)
                .replace("%p", player.getName().getString())
                .replace("%i", Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(stack.getItem())).toString())
                .replace("%q", fullStack ? Integer.toString(stack.getMaxStackSize()) : "1");
        player.connection.sendCommand(command);
    }

    private void handleCreativeTransfer(LocalPlayer player, boolean fullStack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!player.isCreative() || minecraft.gameMode == null) {
            parentGui.cursorStack = ItemStack.EMPTY;
            return;
        }

        if (hotbarSlot < 0) {
            if (stack.isEmpty()) {
                return;
            }
            if (parentGui.cursorStack.isEmpty()) {
                ItemStack picked = stack.copy();
                picked.setCount(fullStack ? picked.getMaxStackSize() : 1);
                parentGui.cursorStack = picked;
            } else {
                parentGui.cursorStack = ItemStack.EMPTY;
            }
            return;
        }

        if (parentGui.cursorStack.isEmpty()) {
            if (stack.isEmpty()) {
                return;
            }
            ItemStack picked = stack.copy();
            if (fullStack) {
                picked.setCount(picked.getMaxStackSize());
            }
            parentGui.cursorStack = picked;
            setHotbarStack(player, minecraft, ItemStack.EMPTY);
            return;
        }

        ItemStack previous = stack.copy();
        ItemStack replacement = parentGui.cursorStack.copy();
        setHotbarStack(player, minecraft, replacement);
        parentGui.cursorStack = previous;
    }

    private void setHotbarStack(LocalPlayer player, Minecraft minecraft, ItemStack replacement) {
        stack = replacement;
        player.getInventory().setItem(hotbarSlot, replacement);
        minecraft.gameMode.handleCreativeModeItemAdd(replacement, hotbarSlot + 36);
    }

    @Override
    public void addTooltip(TooltipBuilder tooltip) {
        if (stack.isEmpty()) {
            return;
        }
        tooltip.add(Component.translatable(stack.getDescriptionId()));
        Block block = Block.byItem(stack.getItem());
        Map<Block, List<SpriteDetails>> scan = MCRGBClient.getLastScan();
        if (scan == null) {
            return;
        }

        List<SpriteDetails> details = scan.get(block);
        if (details == null || details.isEmpty()) {
            return;
        }

        int colorLines = 0;
        int maxLines = MCRGBConfig.MAX_TOOLTIP_LINES.get();
        for (SpriteDetails sd : details) {
            List<Component> lines = ColorInfoLines.linesFor(sd);

            for (Component line : lines) {
                if (colorLines >= maxLines) {
                    tooltip.add(Component.empty());
                    tooltip.add(Component.translatable(Localisation.TOOLTIP_SHOW_MORE)
                            .withStyle(ChatFormatting.GRAY));
                    return;
                }
                tooltip.add(line);
                colorLines++;
            }
        }
    }

}
