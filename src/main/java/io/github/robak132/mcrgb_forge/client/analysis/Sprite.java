package io.github.robak132.mcrgb_forge.client.analysis;

import io.github.robak132.libgui_forge.widget.data.colors.RGB;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.registries.ForgeRegistries;

@Slf4j(topic = "mcrgb_forge")
public final class Sprite {

    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static final int UNTINTED = 0xFFFFFF;
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Set<Block> reportedTintFailures = new HashSet<>();
    private static final Set<Block> reportedModelFailures = new HashSet<>();
    private static final Direction[] DIRECTIONS = {
            Direction.DOWN,
            Direction.UP,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST,
            null
    };

    private final TextureAtlasSprite atlasSprite;
    private final SpriteContents spriteContents;
    private final int tint;
    private List<RGB> visiblePixels;
    private int[] spatialPixels;

    private Sprite(TextureAtlasSprite sprite, int tint) {
        atlasSprite = sprite;
        spriteContents = sprite.contents();
        this.tint = tint & UNTINTED;
    }

    public float v1() {
        return atlasSprite.getV1();
    }

    public float u1() {
        return atlasSprite.getU1();
    }

    public float v0() {
        return atlasSprite.getV0();
    }

    public float u0() {
        return atlasSprite.getU0();
    }

    public ResourceLocation atlasLocation() {
        return atlasSprite.atlasLocation();
    }

    public int tint() {
        return tint;
    }

    public static Set<Sprite> getSprites(Block block) {
        Set<Sprite> sprites = new HashSet<>();
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            visitSprites(state, sprites::add);
        }
        return sprites;
    }

    static void visitSprites(BlockState state, Consumer<Sprite> collector) {
        IForgeBakedModel model;
        try {
            model = MINECRAFT.getBlockRenderer().getBlockModelShaper().getBlockModel(state);
        } catch (RuntimeException error) {
            reportModelFailure(state.getBlock(), error);
            return;
        }

        Map<Integer, Integer> tints = new HashMap<>();
        for (Direction direction : DIRECTIONS) {
            List<BakedQuad> quads;
            try {
                quads = model.getQuads(state, direction, RANDOM, ModelData.EMPTY, null);
            } catch (RuntimeException error) {
                reportModelFailure(state.getBlock(), error);
                return;
            }
            for (BakedQuad quad : quads) {
                int tint = UNTINTED;
                if (quad.isTinted()) {
                    tint = tints.computeIfAbsent(quad.getTintIndex(), index -> resolveTint(state, index));
                    if (tint == -1) {
                        continue;
                    }
                }
                collector.accept(new Sprite(quad.getSprite(), tint));
            }
        }
    }

    private static void reportModelFailure(Block block, RuntimeException error) {
        if (reportedModelFailures.add(block)) {
            log.warn("Skipping unreadable block model for {}: {}",
                    ForgeRegistries.BLOCKS.getKey(block), error.toString());
        }
    }

    private static int resolveTint(BlockState state, int tintIndex) {
        try {
            return MINECRAFT.getBlockColors().getColor(state, null, null, tintIndex);
        } catch (RuntimeException error) {
            if (reportedTintFailures.add(state.getBlock())) {
                log.warn("Skipping unsupported tinted textures for {}: {}",
                        ForgeRegistries.BLOCKS.getKey(state.getBlock()), error.toString());
            }
            return -1;
        }
    }

    public ResourceLocation name() {
        return spriteContents.name();
    }

    void loadPixels() {
        int width = spriteContents.width();
        int height = spriteContents.height();
        var image = spriteContents.getOriginalImage();
        List<RGB> visible = new ArrayList<>();
        spatialPixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getPixelRGBA(x, y);
                int red = multiplyTint(argb & 0xFF, tint >>> 16);
                int green = multiplyTint((argb >>> 8) & 0xFF, tint >>> 8);
                int blue = multiplyTint((argb >>> 16) & 0xFF, tint);
                RGB rgb = new RGB((argb >>> 24) & 0xFF, red, green, blue);
                if (rgb.alpha() != 0) {
                    visible.add(rgb);
                    spatialPixels[y * width + x] = rgb.argb();
                }
            }
        }
        visiblePixels = List.copyOf(visible);
    }

    List<RGB> visiblePixels() {
        return visiblePixels;
    }

    int[] spatialPixels() {
        return spatialPixels;
    }

    int width() {
        return spriteContents.width();
    }

    int height() {
        return spriteContents.height();
    }

    void releasePixels() {
        visiblePixels = null;
        spatialPixels = null;
    }

    private static int multiplyTint(int channel, int tint) {
        return channel * (tint & 0xFF) / 0xFF;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Sprite sprite && atlasSprite == sprite.atlasSprite && tint == sprite.tint;
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(atlasSprite) + tint;
    }
}
