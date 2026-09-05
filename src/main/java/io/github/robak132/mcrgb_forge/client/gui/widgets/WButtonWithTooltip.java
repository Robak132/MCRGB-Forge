package io.github.robak132.mcrgb_forge.client.gui.widgets;

import io.github.robak132.libgui_forge.widget.TooltipBuilder;
import io.github.robak132.libgui_forge.widget.WButton;
import io.github.robak132.libgui_forge.widget.icon.Icon;
import net.minecraft.network.chat.Component;

public class WButtonWithTooltip extends WButton {

    private final Component tooltipComponent;

    public WButtonWithTooltip(Icon icon, Component tooltipComponent) {
        super(icon);
        this.tooltipComponent = tooltipComponent;
    }

    @Override
    public void addTooltip(TooltipBuilder tooltip) {
        tooltip.add(tooltipComponent);
        super.addTooltip(tooltip);
    }

}
