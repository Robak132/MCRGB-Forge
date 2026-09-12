package io.github.robak132.mcrgb_forge.client.analysis;

import io.github.robak132.mcrgb_forge.config.MCRGBConfig.ColorCalculationMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.world.level.block.Block;

final class ScanContext {

    final List<BlockScan> blocks;
    final Consumer<Map<Block, List<SpriteDetails>>> onPartial;
    final Deque<BlockScan> pendingBlocks = new ArrayDeque<>();
    final Set<Sprite> uniqueSprites = new HashSet<>();
    final SpriteAnalyzer spriteAnalyzer;
    int totalStates;

    ScanContext(
            int blockCount,
            ColorCalculationMode mode,
            Consumer<Map<Block, List<SpriteDetails>>> onPartial) {
        this.onPartial = onPartial;
        this.blocks = new ArrayList<>(blockCount);
        spriteAnalyzer = new SpriteAnalyzer(mode);
    }
}
