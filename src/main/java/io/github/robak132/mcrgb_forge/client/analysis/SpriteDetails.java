package io.github.robak132.mcrgb_forge.client.analysis;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpriteDetails {

    private String name;
    private List<SpriteColor> colors;

    public SpriteDetails(String name, List<SpriteColor> colors) {
        this.name = name;
        this.colors = colors;
    }

    public void add(SpriteColor color) {
        colors.add(color);
    }

    /**
     * Builds user-friendly tooltip strings.
     */
    public List<String> getStrings() {
        List<String> strings = new ArrayList<>();
        strings.add(name + ":");

        for (SpriteColor sc : colors) {
            strings.add(sc.color().toHexString() + "  " + sc.weight() + "%");
        }

        return strings;
    }

    /**
     * Returns ARGB ints for color preview squares.
     */
    public List<Integer> getTextColors() {
        List<Integer> list = new ArrayList<>();
        list.add(0xFFFFFF); // Title color

        for (SpriteColor sc : colors) {
            String hex = sc.color().toHexString().substring(1);
            list.add(Integer.parseInt(hex, 16));
        }

        return list;
    }
}
