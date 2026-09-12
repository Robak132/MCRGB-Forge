package io.github.robak132.mcrgb_forge.client.gui.widgets;

import io.github.robak132.libgui_forge.widget.WScrollBar;
import io.github.robak132.libgui_forge.widget.data.InputResult;
import net.minecraft.core.Direction;

public class WColorScrollBar extends WScrollBar {

    private final Runnable runnable;

    public WColorScrollBar(Runnable runnable) {
        super(Direction.Plane.VERTICAL);
        this.runnable = runnable;
    }

    @Override
    public InputResult onMouseDrag(int x, int y, int button, double deltaX, double deltaY) {
        InputResult result = super.onMouseDrag(x, y, button, deltaX, deltaY);
        this.runnable.run();
        return result;
    }

    @Override
    public InputResult onMouseScroll(int x, int y, double vAmount) {
        setValue(getValue() - (int) Math.signum(vAmount));
        this.runnable.run();
        return InputResult.PROCESSED;
    }
}
