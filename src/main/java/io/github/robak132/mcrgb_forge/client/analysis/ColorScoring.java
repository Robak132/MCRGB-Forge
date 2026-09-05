package io.github.robak132.mcrgb_forge.client.analysis;

import io.github.robak132.libgui_forge.widget.data.colors.Color;
import io.github.robak132.libgui_forge.widget.data.colors.OkLAB;
import java.util.List;

public final class ColorScoring {

    private ColorScoring() {
    }

    public static double score(Color query, List<SpriteDetails> sprites) {
        OkLAB queryOkLAB = query.toOkLAB();
        double score = 0.0;
        double totalWeight = 0.0;

        for (SpriteDetails sprite : sprites) {
            for (SpriteColor spriteColor : sprite.getColors()) {
                float weight = spriteColor.weight() / 100f;
                if (weight <= 0.0001f) {
                    continue;
                }

                score += queryOkLAB.distanceWeighted(spriteColor.color().toOkLAB()) * weight;
                totalWeight += weight;
            }
        }

        return totalWeight == 0.0 ? Double.MAX_VALUE : score / totalWeight;
    }
}
