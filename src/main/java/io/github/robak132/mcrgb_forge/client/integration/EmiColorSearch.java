package io.github.robak132.mcrgb_forge.client.integration;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.robak132.libgui_forge.widget.data.colors.RGB;
import io.github.robak132.mcrgb_forge.client.MCRGBClient;
import io.github.robak132.mcrgb_forge.client.analysis.ColorScoring;
import io.github.robak132.mcrgb_forge.client.analysis.SpriteDetails;
import io.github.robak132.mcrgb_forge.client.analysis.TextureNoise;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public final class EmiColorSearch {

    private static final int NO_COLOR = -1;
    private static final int NO_METRIC = -1;
    private static final Pattern COLOR_PREFIX = Pattern.compile(
            "^\\^([0-9a-fA-F]{6})(?::(\\d{1,3})?)?(?::(\\d{1,3})?)?(?:\\s+|$)");
    private static volatile int targetRgb = NO_COLOR;
    private static volatile int targetNoise = NO_METRIC;
    private static volatile int targetSpatial = NO_METRIC;

    private EmiColorSearch() {
    }

    public static boolean hasColorPrefix(String query) {
        return COLOR_PREFIX.matcher(query).find();
    }

    public static String parseAndStripColorPrefix(String query) {
        Matcher matcher = COLOR_PREFIX.matcher(query);
        if (!matcher.find()) {
            targetRgb = NO_COLOR;
            targetNoise = NO_METRIC;
            targetSpatial = NO_METRIC;
            return query;
        }

        targetRgb = Integer.parseInt(matcher.group(1), 16);
        targetNoise = parseMetric(matcher.group(2));
        targetSpatial = parseMetric(matcher.group(3));
        return query.substring(matcher.end()).stripLeading();
    }

    public static List<EmiIngredient> sortByColor(List<EmiIngredient> stacks) {
        int rgb = targetRgb;
        Integer noise = optionalMetric(targetNoise);
        Integer spatial = optionalMetric(targetSpatial);
        Map<Block, List<SpriteDetails>> scan = MCRGBClient.getLastScan();
        if (rgb == NO_COLOR || scan == null || stacks.size() < 2) {
            return stacks;
        }

        RGB color = new RGB((rgb >>> 16) & 0xFF, (rgb >>> 8) & 0xFF, rgb & 0xFF);
        Map<Block, Double> scores = new HashMap<>();
        List<EmiIngredient> sorted = new ArrayList<>(stacks);
        sorted.sort(Comparator.comparingDouble(stack -> score(stack, color, noise, spatial, scan, scores)));
        return List.copyOf(sorted);
    }

    private static double score(
            EmiIngredient ingredient,
            RGB color,
            Integer noise,
            Integer spatial,
            Map<Block, List<SpriteDetails>> scan,
            Map<Block, Double> scores) {
        List<EmiStack> emiStacks = ingredient.getEmiStacks();
        if (emiStacks.size() != 1 || !(emiStacks.get(0).getItemStack().getItem() instanceof BlockItem blockItem)) {
            return Double.MAX_VALUE;
        }

        Block block = blockItem.getBlock();
        List<SpriteDetails> sprites = scan.get(block);
        if (sprites == null || sprites.isEmpty()) {
            return Double.MAX_VALUE;
        }

        return scores.computeIfAbsent(block,
                ignored -> ColorScoring.score(color, sprites, noise, spatial));
    }

    private static int parseMetric(String value) {
        return value == null ? NO_METRIC : TextureNoise.clamp(Integer.parseInt(value));
    }

    private static Integer optionalMetric(int value) {
        return value == NO_METRIC ? null : value;
    }
}
