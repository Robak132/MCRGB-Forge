package io.github.robak132.mcrgb_forge.client.integration;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.robak132.libgui_forge.widget.data.colors.RGB;
import io.github.robak132.mcrgb_forge.client.MCRGBClient;
import io.github.robak132.mcrgb_forge.client.analysis.ColorScoring;
import io.github.robak132.mcrgb_forge.client.analysis.SpriteDetails;
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
    private static final Pattern COLOR_PREFIX = Pattern.compile("^\\^#?([0-9a-fA-F]{6})(?:\\s+|$)");
    private static volatile int targetRgb = NO_COLOR;

    private EmiColorSearch() {
    }

    public static String parseAndStripColorPrefix(String query) {
        Matcher matcher = COLOR_PREFIX.matcher(query);
        if (!matcher.find()) {
            targetRgb = NO_COLOR;
            return query;
        }

        targetRgb = Integer.parseInt(matcher.group(1), 16);
        return query.substring(matcher.end()).stripLeading();
    }

    public static List<EmiIngredient> sortByColor(List<EmiIngredient> stacks) {
        int rgb = targetRgb;
        Map<Block, List<SpriteDetails>> scan = MCRGBClient.getLastScan();
        if (rgb == NO_COLOR || scan == null || stacks.size() < 2) {
            return stacks;
        }

        RGB color = new RGB((rgb >>> 16) & 0xFF, (rgb >>> 8) & 0xFF, rgb & 0xFF);
        Map<Block, Double> scores = new HashMap<>();
        List<EmiIngredient> sorted = new ArrayList<>(stacks);
        sorted.sort(Comparator.comparingDouble(stack -> score(stack, color, scan, scores)));
        return List.copyOf(sorted);
    }

    private static double score(
            EmiIngredient ingredient,
            RGB color,
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

        return scores.computeIfAbsent(block, ignored -> ColorScoring.score(color, sprites));
    }
}
