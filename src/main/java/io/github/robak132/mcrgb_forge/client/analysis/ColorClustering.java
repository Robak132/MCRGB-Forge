package io.github.robak132.mcrgb_forge.client.analysis;

import io.github.robak132.libgui_forge.widget.data.colors.OkLAB;
import io.github.robak132.libgui_forge.widget.data.colors.RGB;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;
import net.minecraft.util.Mth;

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
        if (k <= 0 || maxIters <= 0 || sampleLimit <= 0) {
            throw new IllegalArgumentException("k, maxIters and sampleLimit must be positive");
        }

        int boundedSampleLimit = Mth.clamp(sampleLimit, 1, pixels.size());
        List<RGB> sample = pixels;
        if (pixels.size() > boundedSampleLimit) {
            sample = new ArrayList<>(boundedSampleLimit);
            for (int i = 0; i < boundedSampleLimit; i++) {
                int index = (int) ((long) i * pixels.size() / boundedSampleLimit);
                sample.add(pixels.get(index));
            }
        }

        final int n = sample.size();

        OkLAB[] okLab = new OkLAB[n];
        float[] sampleWeights = new float[n];
        for (int i = 0; i < n; i++) {
            RGB cv = sample.get(i);
            okLab[i] = cv.toOkLAB();
            sampleWeights[i] = cv.alpha() / 255f;
        }

        List<OkLAB> centers = initializeCenters(okLab, sampleWeights, Math.min(k, n));
        final int clusters = centers.size();

        int[] assignments = new int[n];
        boolean changed = true;

        for (int iter = 0; iter < maxIters && changed; iter++) {
            changed = false;

            for (int i = 0; i < n; i++) {
                double bestDist = Double.MAX_VALUE;
                int best = 0;
                OkLAB p = okLab[i];
                for (int c = 0; c < centers.size(); c++) {
                    double d = p.distanceSquared(centers.get(c));
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

            float[] clusterWeights = new float[clusters];
            float[][] sums = new float[clusters][3];
            for (int i = 0; i < n; i++) {
                int c = assignments[i];
                OkLAB p = okLab[i];
                float weight = sampleWeights[i];
                clusterWeights[c] += weight;

                sums[c][0] += p.lightness() * weight;
                sums[c][1] += p.greenRedAxis() * weight;
                sums[c][2] += p.blueYellowAxis() * weight;
            }
            for (int c = 0; c < clusters; c++) {
                if (clusterWeights[c] > 0) {
                    centers.set(c, new OkLAB(255, sums[c][0] / clusterWeights[c],
                            sums[c][1] / clusterWeights[c], sums[c][2] / clusterWeights[c]));
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
                double d = p.distanceSquared(centers.get(c));
                if (d < bestD) {
                    bestD = d;
                    best = c;
                }
            }

            float alphaWeight = cv.alpha() / 255f;
            fullCounts[best] += alphaWeight;
        }

        float totalWeight = 0f;
        for (float v : fullCounts) {
            totalWeight += v;
        }
        if (totalWeight <= 0f) {
            return List.of();
        }

        int[] percentages = percentages(fullCounts, totalWeight);
        for (int c = 0; c < clusters; c++) {
            if (fullCounts[c] == 0) {
                continue;
            }
            OkLAB center = centers.get(c);
            RGB mean = center.toRGB();
            result.add(new SpriteColor(mean, percentages[c]));
        }

        result.sort((a, b) -> Integer.compare(b.weight(), a.weight()));
        return result;
    }

    private static List<OkLAB> initializeCenters(OkLAB[] pixels, float[] weights, int requestedClusters) {
        List<OkLAB> centers = new ArrayList<>(requestedClusters);
        int first = closestToMean(pixels, weights);
        centers.add(new OkLAB(pixels[first]));

        while (centers.size() < requestedClusters) {
            int farthest = -1;
            double farthestDistance = 0.0;
            for (int i = 0; i < pixels.length; i++) {
                double nearestDistance = Double.MAX_VALUE;
                for (OkLAB center : centers) {
                    nearestDistance = Math.min(nearestDistance, pixels[i].distanceSquared(center));
                }
                double weightedDistance = nearestDistance * weights[i];
                if (weightedDistance > farthestDistance) {
                    farthestDistance = weightedDistance;
                    farthest = i;
                }
            }
            if (farthest < 0) {
                break;
            }
            centers.add(new OkLAB(pixels[farthest]));
        }
        return centers;
    }

    private static int closestToMean(OkLAB[] pixels, float[] weights) {
        double lightness = 0.0;
        double greenRed = 0.0;
        double blueYellow = 0.0;
        double totalWeight = 0.0;
        for (int i = 0; i < pixels.length; i++) {
            lightness += pixels[i].lightness() * weights[i];
            greenRed += pixels[i].greenRedAxis() * weights[i];
            blueYellow += pixels[i].blueYellowAxis() * weights[i];
            totalWeight += weights[i];
        }
        if (totalWeight == 0.0) {
            return 0;
        }
        OkLAB mean = new OkLAB(255, (float) (lightness / totalWeight),
                (float) (greenRed / totalWeight), (float) (blueYellow / totalWeight));
        int closest = 0;
        double closestDistance = Double.MAX_VALUE;
        for (int i = 0; i < pixels.length; i++) {
            if (weights[i] <= 0f) {
                continue;
            }
            double distance = pixels[i].distanceSquared(mean);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = i;
            }
        }
        return closest;
    }

    /**
     * Original greedy RGB grouping algorithm from the Fabric version.
     */
    public static List<SpriteColor> fabric(List<RGB> pixels) {
        if (pixels.isEmpty()) {
            return List.of();
        }

        List<List<RGB>> groups = new ArrayList<>();
        Set<Integer> groupedColors = new HashSet<>();
        for (int i = 0; i < pixels.size(); i++) {
            RGB seed = pixels.get(i);
            if (groupedColors.contains(seed.rgb())) {
                continue;
            }

            List<RGB> group = new ArrayList<>();
            group.add(seed);
            for (int j = i + 1; j < pixels.size(); j++) {
                RGB candidate = pixels.get(j);
                if (candidate.distanceSquared(seed) < 100 * 100 && !groupedColors.contains(candidate.rgb())) {
                    group.add(candidate);
                }
            }
            group.forEach(color -> groupedColors.add(color.rgb()));
            groups.add(group);
        }

        List<SpriteColor> result = new ArrayList<>(groups.size());
        float[] groupWeights = new float[groups.size()];
        RGB[] means = new RGB[groups.size()];
        float totalWeight = 0f;
        for (int i = 0; i < groups.size(); i++) {
            List<RGB> group = groups.get(i);
            double red = 0;
            double green = 0;
            double blue = 0;
            float groupWeight = 0f;
            for (RGB pixel : group) {
                float alphaWeight = pixel.alpha() / 255f;
                red += pixel.red() * alphaWeight;
                green += pixel.green() * alphaWeight;
                blue += pixel.blue() * alphaWeight;
                groupWeight += alphaWeight;
            }
            groupWeights[i] = groupWeight;
            totalWeight += groupWeight;
            if (groupWeight > 0f) {
                means[i] = new RGB((int) Math.round(red / groupWeight),
                        (int) Math.round(green / groupWeight), (int) Math.round(blue / groupWeight));
            }
        }
        if (totalWeight <= 0f) {
            return List.of();
        }
        int[] percentages = percentages(groupWeights, totalWeight);
        for (int i = 0; i < groups.size(); i++) {
            if (means[i] != null) {
                result.add(new SpriteColor(means[i], percentages[i]));
            }
        }
        result.sort((a, b) -> Integer.compare(b.weight(), a.weight()));
        return result;
    }

    public static List<SpriteColor> mean(List<RGB> pixels) {
        if (pixels.isEmpty()) {
            return List.of();
        }

        double red = 0;
        double green = 0;
        double blue = 0;
        double totalWeight = 0;
        for (RGB pixel : pixels) {
            double alphaWeight = pixel.alpha() / 255.0;
            red += pixel.red() * alphaWeight;
            green += pixel.green() * alphaWeight;
            blue += pixel.blue() * alphaWeight;
            totalWeight += alphaWeight;
        }
        if (totalWeight == 0) {
            return List.of();
        }

        return List.of(new SpriteColor(new RGB(
                meanChannel(red, totalWeight),
                meanChannel(green, totalWeight),
                meanChannel(blue, totalWeight)), 100));
    }

    public static List<SpriteColor> median(List<RGB> pixels) {
        if (pixels.isEmpty()) {
            return List.of();
        }

        double totalWeight = pixels.stream().mapToDouble(pixel -> pixel.alpha() / 255.0).sum();
        if (totalWeight == 0.0) {
            return List.of();
        }

        return List.of(new SpriteColor(new RGB(
                medianChannel(pixels, RGB::red, totalWeight),
                medianChannel(pixels, RGB::green, totalWeight),
                medianChannel(pixels, RGB::blue, totalWeight)), 100));
    }

    private static int medianChannel(List<RGB> pixels, ToIntFunction<RGB> channel, double totalWeight) {
        List<RGB> sorted = pixels.stream().sorted(Comparator.comparingInt(channel)).toList();
        double midpoint = totalWeight / 2.0;
        double cumulativeWeight = 0.0;
        for (int i = 0; i < sorted.size(); i++) {
            RGB pixel = sorted.get(i);
            cumulativeWeight += pixel.alpha() / 255.0;
            if (cumulativeWeight > midpoint) {
                return channel.applyAsInt(pixel);
            }
            if (Math.abs(cumulativeWeight - midpoint) < 1.0e-9) {
                for (int j = i + 1; j < sorted.size(); j++) {
                    if (sorted.get(j).alpha() > 0) {
                        return Math.round((channel.applyAsInt(pixel) + channel.applyAsInt(sorted.get(j))) / 2f);
                    }
                }
                return channel.applyAsInt(pixel);
            }
        }
        return channel.applyAsInt(sorted.get(sorted.size() - 1));
    }

    private static int meanChannel(double total, double weight) {
        return Mth.clamp((int) Math.round(total / weight), 0, 255);
    }

    private static int[] percentages(float[] weights, float totalWeight) {
        int[] result = new int[weights.length];
        double[] remainders = new double[weights.length];
        int assigned = 0;
        for (int i = 0; i < weights.length; i++) {
            double exact = weights[i] / totalWeight * 100.0;
            result[i] = (int) Math.floor(exact);
            remainders[i] = exact - result[i];
            assigned += result[i];
        }
        while (assigned < 100) {
            int largestRemainder = 0;
            for (int i = 1; i < remainders.length; i++) {
                if (remainders[i] > remainders[largestRemainder]) {
                    largestRemainder = i;
                }
            }
            result[largestRemainder]++;
            remainders[largestRemainder] = -1.0;
            assigned++;
        }
        return result;
    }
}
