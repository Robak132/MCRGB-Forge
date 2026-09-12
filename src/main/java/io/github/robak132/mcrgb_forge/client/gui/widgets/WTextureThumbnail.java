package io.github.robak132.mcrgb_forge.client.gui.widgets;

import io.github.robak132.libgui_forge.widget.WSprite;
import io.github.robak132.libgui_forge.widget.data.InputResult;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;

public class WTextureThumbnail extends WSprite {

    Integer index;
    Consumer<Integer> onClick;

    public WTextureThumbnail(ResourceLocation image, float u1, float v1, float u2, float v2, int i,
            Consumer<Integer> onClick) {
        super(image, u1, v1, u2, v2);
        this.index = i;
        this.onClick = onClick;
    }

    @Override
    public InputResult onClick(int x, int y, int button) {
        onClick.accept(index);
        return super.onClick(x, y, button);
    }
}
