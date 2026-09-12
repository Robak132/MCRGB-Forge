package io.github.robak132.mcrgb_forge.client.analysis;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpriteDetails {

    private String name;
    private List<SpriteColor> colors;
    private TextureNoise noise;

    public SpriteDetails(String name, List<SpriteColor> colors, TextureNoise noise) {
        this.name = name;
        this.colors = colors;
        this.noise = noise;
    }

    public void add(SpriteColor color) {
        colors.add(color);
    }
}
