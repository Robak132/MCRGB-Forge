package io.github.robak132.mcrgb_forge.client.analysis;

import io.github.robak132.libgui_forge.widget.data.colors.Color;
import io.github.robak132.libgui_forge.widget.data.colors.OkLAB;
import java.util.List;

public final class ColorScoring {

    private static final double COLOR_SCORE_WEIGHT = 0.90;
    private static final double NOISE_SCORE_WEIGHT = 0.10;

    private ColorScoring() {
    }

    /**
     * Scores against the requested quantized texture-noise values on the same 0..100 scale as {@link TextureNoise}.
     * A null target excludes that metric from scoring.
     */
    public static double score(Color query, List<SpriteDetails> sprites, Integer targetGlobalNoise,
            Integer targetSpatialNoise) {
        OkLAB queryOkLAB = query.toOkLAB();
        Double globalTarget = normalize(targetGlobalNoise);
        Double spatialTarget = normalize(targetSpatialNoise);
        double bestScore = Double.MAX_VALUE;

        for (SpriteDetails sprite : sprites) {
            double colorScore = Double.MAX_VALUE;
            for (SpriteColor spriteColor : sprite.getColors()) {
                if (spriteColor.weight() <= 0) {
                    continue;
                }
                colorScore = Math.min(colorScore, queryOkLAB.distanceSquared(spriteColor.color().toOkLAB()));
            }

            if (colorScore == Double.MAX_VALUE) {
                continue;
            }
            TextureNoise noise = sprite.getNoise();
            if (noise == null || globalTarget == null && spatialTarget == null) {
                bestScore = Math.min(bestScore, colorScore);
                continue;
            }
            double noiseScore = 0.0;
            int metricCount = 0;
            if (globalTarget != null) {
                double difference = noise.global() / 100.0 - globalTarget;
                noiseScore += difference * difference;
                metricCount++;
            }
            if (spatialTarget != null) {
                double difference = noise.kernel() / 100.0 - spatialTarget;
                noiseScore += difference * difference;
                metricCount++;
            }
            noiseScore /= metricCount;
            bestScore = Math.min(bestScore,
                    colorScore * COLOR_SCORE_WEIGHT + noiseScore * NOISE_SCORE_WEIGHT);
        }

        return bestScore;
    }

    private static Double normalize(Integer value) {
        return value == null ? null : TextureNoise.clamp(value) / 100.0;
    }
}
