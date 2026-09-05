package io.github.robak132.mcrgb_forge.client.analysis;

import io.github.robak132.libgui_forge.widget.data.colors.RGB;
import io.github.robak132.mcrgb_forge.config.MCRGBConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.registries.ForgeRegistries;

@Slf4j
public class ColorScanner {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final RandomSource random = RandomSource.create();

    private static final int K_VALUE = 5;
    private static final int MAX_ITERS = 8;
    private static final int SAMPLE_LIMIT = 4096;

    /**
     * Extracts all model sprites of a block.
     */
    public static Set<TextureAtlasSprite> getSprites(Block block) {
        Set<TextureAtlasSprite> sprites = new HashSet<>();
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            IForgeBakedModel model = mc.getBlockRenderer().getBlockModelShaper().getBlockModel(state);

            for (Direction dir : getDirections()) {
                List<BakedQuad> quads = model.getQuads(state, dir, random, ModelData.EMPTY, null);
                if (!quads.isEmpty()) {
                    sprites.add(quads.get(0).getSprite());
                }
            }
        }
        return sprites;
    }

    private static List<Direction> getDirections() {
        List<Direction> dirs = new ArrayList<>(Arrays.asList(Direction.values()));
        dirs.add(null);
        return dirs;
    }

    @SuppressWarnings("resource")
    private static String getSpriteName(TextureAtlasSprite sprite) {
        return sprite.contents().name().getPath();
    }

    /**
     * Asynchronously scans blocks.
     */
    public Future<ScanResult> scanAsync(Consumer<ScanResult> onSuccess, Consumer<Throwable> onError) {
        return CompletableFuture.supplyAsync(() -> scan(ForgeRegistries.BLOCKS.getEntries().stream().map(Entry::getValue).toList()))
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        onSuccess.accept(result);
                    } else {
                        onError.accept(ex);
                    }
                });
    }

    /**
     * Performs the scan synchronously.
     */
    public ScanResult scan(List<Block> blocks) {
        log.info("Starting color scan of {} blocks", blocks.size());
        Map<Block, List<SpriteDetails>> result = new HashMap<>();

        for (Block block : blocks) {
            Set<TextureAtlasSprite> sprites = getSprites(block);
            List<SpriteDetails> spriteDetailsList = new ArrayList<>();

            for (TextureAtlasSprite sprite : sprites) {
                List<RGB> pixels = getSpritePixels(sprite);
                if (pixels.isEmpty()) {
                    continue;
                }

                List<SpriteColor> clustered = switch (MCRGBConfig.COLOR_CALCULATION_MODE.get()) {
                    case OKLAB -> ColorClustering.kMeansOkLab(pixels, K_VALUE, MAX_ITERS, SAMPLE_LIMIT);
                    case FABRIC -> ColorClustering.fabric(pixels);
                    case MEAN -> ColorClustering.mean(pixels);
                    case MEDIAN -> ColorClustering.median(pixels);
                };

                spriteDetailsList.add(new SpriteDetails(getSpriteName(sprite), clustered));
            }

            result.put(block, spriteDetailsList);
        }

        return new ScanResult(result);
    }

    /**
     * Extracts visible pixel colors from a sprite.
     */
    @SuppressWarnings("resource")
    private List<RGB> getSpritePixels(TextureAtlasSprite sprite) {
        List<RGB> pixels = new ArrayList<>();
        var contents = sprite.contents();
        int w = contents.width();
        int h = contents.height();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = sprite.getPixelRGBA(0, x, y);
                RGB rgb = new RGB((argb >>> 24) & 0xFF, argb & 0xFF, (argb >>> 8) & 0xFF, (argb >>> 16) & 0xFF);
                if (rgb.alpha() == 0) {
                    continue;
                }
                pixels.add(rgb);
            }
        }

        return pixels;
    }

    public record ScanResult(Map<Block, List<SpriteDetails>> blockSprites) {

    }
}
