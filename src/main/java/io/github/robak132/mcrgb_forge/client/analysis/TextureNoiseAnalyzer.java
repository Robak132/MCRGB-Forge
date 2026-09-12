package io.github.robak132.mcrgb_forge.client.analysis;

import io.github.robak132.libgui_forge.widget.data.colors.OkLAB;
import io.github.robak132.libgui_forge.widget.data.colors.RGB;

/**
 * Calculates the global and spatial noise metrics described by the texture-noise methodology.
 */
public final class TextureNoiseAnalyzer {

    private static final int KERNEL_RADIUS = 2;

    private TextureNoiseAnalyzer() {
    }

    /**
     * Calculates noise for a row-major ARGB sprite. Texture coordinates wrap because Minecraft block textures tile at
     * their edges.
     */
    public static TextureNoise analyze(int[] pixels, int width, int height) {
        if (width <= 0 || height <= 0 || pixels.length != width * height) {
            throw new IllegalArgumentException("Pixel array dimensions do not match");
        }

        double totalWeight = 0.0;
        double lightness = 0.0;
        double greenRed = 0.0;
        double blueYellow = 0.0;
        OkLAB[] okLabPixels = new OkLAB[pixels.length];
        double[] weights = new double[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            double weight = ((pixel >>> 24) & 0xFF) / 255.0;
            if (weight == 0.0) {
                continue;
            }
            OkLAB converted = new RGB(pixel).toOkLAB();
            okLabPixels[i] = converted;
            weights[i] = weight;
            totalWeight += weight;
            lightness += converted.lightness() * weight;
            greenRed += converted.greenRedAxis() * weight;
            blueYellow += converted.blueYellowAxis() * weight;
        }
        if (totalWeight == 0.0) {
            return TextureNoise.NONE;
        }

        double meanLightness = lightness / totalWeight;
        double meanGreenRed = greenRed / totalWeight;
        double meanBlueYellow = blueYellow / totalWeight;
        double globalDistance = 0.0;
        for (int i = 0; i < okLabPixels.length; i++) {
            OkLAB pixel = okLabPixels[i];
            if (pixel != null) {
                globalDistance += distance(pixel, meanLightness, meanGreenRed, meanBlueYellow) * weights[i];
            }
        }

        double kernelDistance = 0.0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                OkLAB center = okLabPixels[y * width + x];
                if (center == null) {
                    continue;
                }

                double localDistance = 0.0;
                double localWeight = weights[y * width + x];
                for (int offsetY = -KERNEL_RADIUS; offsetY <= KERNEL_RADIUS; offsetY++) {
                    for (int offsetX = -KERNEL_RADIUS; offsetX <= KERNEL_RADIUS; offsetX++) {
                        if (offsetX == 0 && offsetY == 0) {
                            continue;
                        }
                        int neighbourX = Math.floorMod(x + offsetX, width);
                        int neighbourY = Math.floorMod(y + offsetY, height);
                        int neighbourIndex = neighbourY * width + neighbourX;
                        OkLAB neighbour = okLabPixels[neighbourIndex];
                        if (neighbour != null) {
                            localDistance += distance(center, neighbour.lightness(),
                                    neighbour.greenRedAxis(), neighbour.blueYellowAxis()) * weights[neighbourIndex];
                            localWeight += weights[neighbourIndex];
                        }
                    }
                }
                kernelDistance += localDistance / localWeight * weights[y * width + x];
            }
        }

        return new TextureNoise(
                TextureNoise.normalizeDistance(globalDistance / totalWeight),
                TextureNoise.normalizeDistance(kernelDistance / totalWeight));
    }

    private static double distance(OkLAB pixel, double lightness, double greenRed, double blueYellow) {
        double lightnessDifference = pixel.lightness() - lightness;
        double greenRedDifference = pixel.greenRedAxis() - greenRed;
        double blueYellowDifference = pixel.blueYellowAxis() - blueYellow;
        return Math.sqrt(lightnessDifference * lightnessDifference
                + greenRedDifference * greenRedDifference
                + blueYellowDifference * blueYellowDifference);
    }
}
