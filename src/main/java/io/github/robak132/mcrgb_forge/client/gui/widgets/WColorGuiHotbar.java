package io.github.robak132.mcrgb_forge.client.gui.widgets;

import io.github.robak132.libgui_forge.widget.WPlainPanel;
import io.github.robak132.mcrgb_forge.client.gui.ColorsGuiDescription;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

public class WColorGuiHotbar extends WPlainPanel {

    private final List<WColorGuiSlot> slots = new ArrayList<>();

    public WColorGuiHotbar(ColorsGuiDescription gui) {
        LocalPlayer player = Minecraft.getInstance().player;
        for (int i = 0; i < 9; i++) {
            WColorGuiSlot slot = new WColorGuiSlot(player == null ? ItemStack.EMPTY
                    : player.getInventory().getItem(i), gui, i);
            slots.add(slot);
            add(slot, i * 18, 0, 18, 18);
        }
    }

    @Override
    public void tick() {
        super.tick();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        for (int i = 0; i < slots.size(); i++) {
            slots.get(i).setStack(player.getInventory().getItem(i));
        }
    }
}
