package io.github.robak132.mcrgb_forge.client.analysis;

import io.github.robak132.libgui_forge.widget.data.colors.OkLAB;
import io.github.robak132.libgui_forge.widget.data.colors.RGB;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Simple K-means using OKLab distance. Returns list of Sprite (mean + weight%).
 */
public final class ColorClustering {

    private ColorClustering() {
    }

    /**
     * Cluster pixels into up to k clusters.
     *
     * @param pixels      list of ColorVector (RGB 0..255)
     * @param k           desired number of clusters (e.g., 3)
     * @param maxIters    max iterations (e.g., 8)
     * @param sampleLimit maximum pixels to sample for performance (e.g., 4096)
     */
    public static List<SpriteColor> kMeansOkLab(List<RGB> pixels, int k, int maxIters, int sampleLimit) {
        if (pixels.isEmpty()) {
            return List.of();
        }

        int boundedSampleLimit = Mth.clamp(sampleLimit, 1, pixels.size());
        List<RGB> sample = pixels;
        if (pixels.size() > boundedSampleLimit) {
            sample = new ArrayList<>(boundedSampleLimit);
            int step = Math.max(1, pixels.size() / boundedSampleLimit);
            for (int i = 0; i < pixels.size() && sample.size() < boundedSampleLimit; i += step) {
                sample.add(pixels.get(i));
            }
        }

        final int n = sample.size();
        final int clusters = Math.min(k, n);

        OkLAB[] okLab = new OkLAB[n];
        for (int i = 0; i < n; i++) {
            RGB cv = sample.get(i);
            okLab[i] = cv.toOkLAB();
        }

        RandomSource rnd = RandomSource.create();
        List<OkLAB> centers = new ArrayList<>(clusters);
        boolean[] used = new boolean[n];
        for (int i = 0; i < clusters; i++) {
            int idx;
            do {
                idx = rnd.nextInt(n);
            } while (used[idx]);
            used[idx] = true;
            centers.add(new OkLAB(okLab[idx]));
        }

        int[] assignments = new int[n];
        boolean changed = true;

        for (int iter = 0; iter < maxIters && changed; iter++) {
            changed = false;

            // assignment step
            for (int i = 0; i < n; i++) {
                double bestDist = Double.MAX_VALUE;
                int best = 0;
                OkLAB p = okLab[i];
                for (int c = 0; c < centers.size(); c++) {
                    double d = p.distanceWeighted(centers.get(c));
                    if (d < bestDist) {
                        bestDist = d;
                        best = c;
                    }
                }
                if (assignments[i] != best) {
                    changed = true;
                    assignments[i] = best;
                }
            }

            // update step: compute new centers as mean of assigned OKLab coords
            int[] counts = new int[clusters];
            float[][] sums = new float[clusters][3];
            for (int i = 0; i < n; i++) {
                int c = assignments[i];
                OkLAB p = okLab[i];
                counts[c]++;

                sums[c][0] += p.lightness();
                sums[c][1] += p.greenRedAxis();
                sums[c][2] += p.blueYellowAxis();
            }
            for (int c = 0; c < clusters; c++) {
                if (counts[c] > 0) {
                    centers.set(c, new OkLAB(255, sums[c][0] / counts[c], sums[c][1] / counts[c], sums[c][2] / counts[c]));
                }
            }
        }

        List<SpriteColor> result = new ArrayList<>();
        float[] fullCounts = new float[clusters];
        for (RGB cv : pixels) {
            OkLAB p = cv.toOkLAB();
            int best = 0;
            double bestD = Float.MAX_VALUE;

            for (int c = 0; c < clusters; c++) {
                double d = p.distanceWeighted(centers.get(c));
                if (d < bestD) {
                    bestD = d;
                    best = c;
                }
            }

            float alphaWeight = 1f + (cv.alpha() / 255f) * 4f;
            fullCounts[best] += alphaWeight;
        }

        float totalWeight = 0f;
        for (float v : fullCounts) {
            totalWeight += v;
        }

        for (int c = 0; c < clusters; c++) {
            if (fullCounts[c] == 0) {
                continue;
            }
            OkLAB center = centers.get(c);
            RGB mean = center.toRGB();
            int weight = Mth.clamp(Math.round((fullCounts[c] / totalWeight) * 100f), 0, 100);
            result.add(new SpriteColor(mean, weight));
        }

        result.sort((a, b) -> Integer.compare(b.weight(), a.weight()));
        return result;
    }

    /**
     * Original greedy RGB grouping algorithm from the Fabric version.
     */
    public static List<SpriteColor> fabric(List<RGB> pixels) {
        if (pixels.isEmpty()) {
            return List.of();
        }

        List<List<RGB>> groups = new ArrayList<>();
        for (int i = 0; i < pixels.size(); i++) {
            RGB seed = pixels.get(i);
            if (containsColor(groups, seed)) {
                continue;
            }

            List<RGB> group = new ArrayList<>();
            group.add(seed);
            for (int j = i + 1; j < pixels.size(); j++) {
                RGB candidate = pixels.get(j);
                if (rgbDistanceSquared(candidate, seed) < 100 * 100 && !containsColor(groups, candidate)) {
                    group.add(candidate);
                }
            }
            groups.add(group);
        }

        List<SpriteColor> result = new ArrayList<>(groups.size());
        for (List<RGB> group : groups) {
            long red = 0;
            long green = 0;
            long blue = 0;
            for (RGB pixel : group) {
                red += pixel.red();
                green += pixel.green();
                blue += pixel.blue();
            }

            int count = group.size();
            RGB mean = new RGB((int) (red / count), (int) (green / count), (int) (blue / count));
            int weight = (int) ((float) count / pixels.size() * 100f);
            result.add(new SpriteColor(mean, weight));
        }
        return result;
    }

    private static boolean containsColor(List<List<RGB>> groups, RGB color) {
        for (List<RGB> group : groups) {
            for (RGB grouped : group) {
                if (grouped.red() == color.red() && grouped.green() == color.green() && grouped.blue() == color.blue()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int rgbDistanceSquared(RGB first, RGB second) {
        int red = first.red() - second.red();
        int green = first.green() - second.green();
        int blue = first.blue() - second.blue();
        return red * red + green * green + blue * blue;
    }

    public static List<SpriteColor> mean(List<RGB> pixels) {
        if (pixels.isEmpty()) {
            return List.of();
        }

        long red = 0;
        long green = 0;
        long blue = 0;
        for (RGB pixel : pixels) {
            red += pixel.red();
            green += pixel.green();
            blue += pixel.blue();
        }

        int size = pixels.size();
        return List.of(new SpriteColor(new RGB(
                meanChannel(red, size),
                meanChannel(green, size),
                meanChannel(blue, size)), 100));
    }

    public static List<SpriteColor> median(List<RGB> pixels) {
        if (pixels.isEmpty()) {
            return List.of();
        }

        List<Integer> reds = pixels.stream().map(RGB::red).sorted(Comparator.naturalOrder()).toList();
        List<Integer> greens = pixels.stream().map(RGB::green).sorted(Comparator.naturalOrder()).toList();
        List<Integer> blues = pixels.stream().map(RGB::blue).sorted(Comparator.naturalOrder()).toList();
        int middle = pixels.size() / 2;

        return List.of(new SpriteColor(new RGB(
                medianChannel(reds, middle),
                medianChannel(greens, middle),
                medianChannel(blues, middle)), 100));
    }

    private static int medianChannel(List<Integer> values, int middle) {
        if (values.size() % 2 == 1) {
            return values.get(middle);
        }
        return Mth.clamp(Math.round((values.get(middle - 1) + values.get(middle)) / 2f), 0, 255);
    }

    private static int meanChannel(long total, int count) {
        return Mth.clamp((int) Math.round((double) total / count), 0, 255);
    }
}
