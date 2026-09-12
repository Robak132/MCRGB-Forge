package io.github.robak132.mcrgb_forge.client.analysis;

/**
 * Quantized perceptual texture-variation metrics on a 0..100 scale.
 *
 * @param global average distance of visible pixels from the texture mean
 * @param kernel average distance within a 5x5 neighborhood
 */
public record TextureNoise(int global, int kernel) {

    public static final TextureNoise NONE = new TextureNoise(0, 0);

    public TextureNoise {
        global = clamp(global);
        kernel = clamp(kernel);
    }

    /**
     * Maps a raw OKLab distance to an integer from 0 to 100 using a concave gamma curve. This gives small changes near
     * zero more influence while progressively compressing changes near one.
     */
    public static int normalizeDistance(double distance) {
        if (!Double.isFinite(distance) || distance <= 0.0) {
            return 0;
        }
        return (int) Math.round(Math.sqrt(Math.min(distance, 1.0)) * 100.0);
    }

    public static int clamp(int value) {
        return Math.max(0, Math.min(value, 100));
    }
}
