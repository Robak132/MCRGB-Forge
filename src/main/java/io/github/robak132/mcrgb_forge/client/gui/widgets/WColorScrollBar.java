package io.github.robak132.mcrgb_forge.client.gui.widgets;

import io.github.robak132.libgui_forge.widget.WScrollBar;
import io.github.robak132.libgui_forge.widget.data.InputResult;
import net.minecraft.core.Direction;

public class WColorScrollBar extends WScrollBar {

    Runnable runnable;

    public WColorScrollBar(Runnable runnable) {
        super(Direction.Plane.VERTICAL);
        this.runnable = runnable;
    }

    @Override
    public InputResult onMouseDrag(int x, int y, int button, double deltaX, double deltaY) {
        this.runnable.run();
        return super.onMouseDrag(x, y, button, deltaX, deltaY);
    }

    @Override
    public InputResult onMouseScroll(int x, int y, double vAmount) {
        this.runnable.run();
        setValue(getValue() + (int) -vAmount);
        return InputResult.PROCESSED;
    }
}
