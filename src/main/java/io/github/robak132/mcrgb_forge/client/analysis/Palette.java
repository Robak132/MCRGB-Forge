package io.github.robak132.mcrgb_forge.client.analysis;

import io.github.robak132.libgui_forge.widget.data.colors.RGB;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Palette {

    List<RGB> colorList = new ArrayList<>();

    public void setColor(int i, RGB color) {
        colorList.set(i, color);
    }

    public RGB getColor(int i) {
        return colorList.get(i);
    }

    public void addColor(RGB color) {
        colorList.add(color);
    }
}
