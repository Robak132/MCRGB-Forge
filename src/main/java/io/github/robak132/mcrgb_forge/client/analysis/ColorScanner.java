package io.github.robak132.mcrgb_forge.client.analysis;

import static io.github.robak132.mcrgb_forge.client.utils.Utils.elapsedMillis;
import static io.github.robak132.mcrgb_forge.client.utils.Utils.maximumMemoryMib;
import static io.github.robak132.mcrgb_forge.client.utils.Utils.usedMemoryMib;
import static io.github.robak132.mcrgb_forge.config.MCRGBConfig.EMI_LOADED;

import io.github.robak132.mcrgb_forge.client.integration.EmiIntegration;
import io.github.robak132.mcrgb_forge.config.MCRGBConfig;
import io.github.robak132.mcrgb_forge.config.MCRGBConfig.ColorCalculationMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

@Slf4j(topic = "mcrgb_forge")
public class ColorScanner {

    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static final ExecutorService SCAN_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "MCRGB Color Scanner");
        thread.setDaemon(true);
        return thread;
    });
    private static final int MAX_STATES_PER_BATCH = 4096;
    private static final long MODEL_BATCH_BUDGET_NANOS = 1_000_000L;
    private static final long SLOW_BATCH_WAIT_MILLIS = 50L;
    private final Semaphore scanBatches = new Semaphore(0);

    public List<Block> resolveScanBlocks() {
        long filterStarted = System.nanoTime();
        List<Block> registeredBlocks = ForgeRegistries.BLOCKS.getEntries().stream().map(Entry::getValue).toList();
        List<Block> blocks = EMI_LOADED
                ? EmiIntegration.retainIndexedBlocks(registeredBlocks)
                : registeredBlocks;
        log.debug("Resolved scan block list in {} ms: registered={}, selected={}, emiLoaded={}",
                elapsedMillis(filterStarted), registeredBlocks.size(), blocks.size(), EMI_LOADED);
        return blocks;
    }

    public Future<Map<Block, List<SpriteDetails>>> scanAsync(
            Map<Block, List<SpriteDetails>> completed,
            Consumer<Map<Block, List<SpriteDetails>>> onPartial,
            Consumer<Map<Block, List<SpriteDetails>>> onSuccess,
            Consumer<Throwable> onError,
            IntConsumer onBlockCount) {
        Map<Block, List<SpriteDetails>> seed = completed == null ? Map.of() : Map.copyOf(completed);
        List<Block> allBlocks = resolveScanBlocks();
        List<Block> blocks = seed.isEmpty()
                ? allBlocks
                : allBlocks.stream().filter(block -> !seed.containsKey(block)).toList();
        onBlockCount.accept(allBlocks.size());
        ColorCalculationMode mode = MCRGBConfig.COLOR_CALCULATION_MODE.get();
        if (!seed.isEmpty()) {
            log.info("Resuming color scan: {} cached, {} remaining, {} total; mode={}",
                    seed.size(), blocks.size(), allBlocks.size(), mode);
        }
        return SCAN_EXECUTOR.submit(() -> {
            // A previous logout cancel may leave the worker thread interrupted.
            Thread.interrupted();
            log.debug("Color scanner thread started; mode={}, blocks={}, seeded={}",
                    mode, blocks.size(), seed.size());
            try {
                Map<Block, List<SpriteDetails>> scanned = blocks.isEmpty()
                        ? Map.of()
                        : scan(blocks, mode, onPartial);
                Map<Block, List<SpriteDetails>> result = new HashMap<>(seed.size() + scanned.size());
                result.putAll(seed);
                result.putAll(scanned);
                onSuccess.accept(result);
                return result;
            } catch (Exception error) {
                onError.accept(error);
                throw error;
            }
        });
    }

    public void replenishScanBatch() {
        if (scanBatches.availablePermits() == 0) {
            scanBatches.release();
        }
    }

    private Map<Block, List<SpriteDetails>> scan(
            List<Block> blocks,
            ColorCalculationMode mode,
            Consumer<Map<Block, List<SpriteDetails>>> onPartial) {
        long started = System.nanoTime();
        ScanContext context = prepareScan(blocks, mode, onPartial);
        try {
            scanModels(context);
            finishTextureAnalysis(context);
            Map<Block, List<SpriteDetails>> result = assembleResult(context);
            log.info("Color scan finished in {} ms; {} sprites retained, memory={} MiB/{} MiB",
                    elapsedMillis(started), context.spriteAnalyzer.retainedSpriteCount(),
                    usedMemoryMib(), maximumMemoryMib());
            return result;
        } finally {
            context.spriteAnalyzer.close();
        }
    }

    private ScanContext prepareScan(
            List<Block> blocks,
            ColorCalculationMode mode,
            Consumer<Map<Block, List<SpriteDetails>>> onPartial) {
        long started = System.nanoTime();
        ScanContext context = new ScanContext(blocks.size(), mode, onPartial);
        for (Block block : blocks) {
            BlockScan blockScan = new BlockScan(block);
            context.blocks.add(blockScan);
            context.totalStates += blockScan.stateCount();
        }
        context.blocks.sort(Comparator.comparingInt(BlockScan::stateCount));
        context.pendingBlocks.addAll(context.blocks);
        log.info("Prepared color scan of {} blocks and {} block states in {} ms; mode={}, memory={} MiB/{} MiB",
                blocks.size(), context.totalStates, elapsedMillis(started), mode,
                usedMemoryMib(), maximumMemoryMib());
        return context;
    }

    private void scanModels(ScanContext context) {
        int processedStates = 0;
        ModelScanStats stats = new ModelScanStats(context.totalStates);
        log.debug("Beginning model scan of {} states across {} blocks",
                context.totalStates, context.blocks.size());

        scanBatches.drainPermits();
        while (processedStates < context.totalStates) {
            checkCancellation();
            context.spriteAnalyzer.checkFailure();
            awaitScanBatch();

            long submitStarted = System.nanoTime();
            ModelBatch batch;
            try {
                batch = MINECRAFT.submit(() -> scanModelBatch(context)).get();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new CancellationException("Color scan cancelled");
            } catch (ExecutionException error) {
                throw new IllegalStateException("Failed to scan block models", error.getCause());
            }
            long submitMillis = elapsedMillis(submitStarted);
            processedStates += batch.processedStates();
            context.spriteAnalyzer.submit(batch.discoveredSprites(),
                    () -> publishUpdatedBlocks(context, batch.updatedBlocks()));
            stats.recordBatch(processedStates, batch.workNanos(), batch.processedStates(),
                    batch.discoveredSprites().size(), batch.updatedBlocks().size(), submitMillis);
        }
        stats.logCompletion();
    }

    private ModelBatch scanModelBatch(ScanContext context) {
        long started = System.nanoTime();
        int processedStates = 0;
        List<Sprite> discoveredSprites = new ArrayList<>();
        List<BlockSprites> updatedBlocks = new ArrayList<>();
        while (!context.pendingBlocks.isEmpty() && processedStates < MAX_STATES_PER_BATCH) {
            // Finish the current block before rotating so completed-count UI progress stays live.
            BlockScan block = context.pendingBlocks.removeFirst();
            boolean budgetExhausted = false;
            do {
                block.collectNextState(context.uniqueSprites, discoveredSprites);
                processedStates++;
                budgetExhausted = System.nanoTime() - started >= MODEL_BATCH_BUDGET_NANOS
                        || processedStates >= MAX_STATES_PER_BATCH;
            } while (block.hasPendingState() && !budgetExhausted);

            if (block.hasPendingState()) {
                context.pendingBlocks.addFirst(block);
                break;
            }
            updatedBlocks.add(snapshot(block));
        }
        return new ModelBatch(processedStates, discoveredSprites, updatedBlocks, System.nanoTime() - started);
    }

    private void publishUpdatedBlocks(ScanContext context, List<BlockSprites> updatedBlocks) {
        if (updatedBlocks.isEmpty()) {
            return;
        }
        Map<Block, List<SpriteDetails>> result = new HashMap<>(updatedBlocks.size());
        for (BlockSprites block : updatedBlocks) {
            result.put(block.block(), context.spriteAnalyzer.detailsFor(block.sprites()));
        }
        context.onPartial.accept(Map.copyOf(result));
    }

    private static BlockSprites snapshot(BlockScan block) {
        return new BlockSprites(block.block(), Set.copyOf(block.sprites()));
    }

    private void finishTextureAnalysis(ScanContext context) {
        log.info("Collected {} unique sprites; waiting for pipelined texture analysis, memory={} MiB/{} MiB",
                context.uniqueSprites.size(), usedMemoryMib(), maximumMemoryMib());
        context.spriteAnalyzer.awaitCompletion();
    }

    private Map<Block, List<SpriteDetails>> assembleResult(ScanContext context) {
        long started = System.nanoTime();
        Map<Block, List<SpriteDetails>> result = new HashMap<>(context.blocks.size());
        for (BlockScan block : context.blocks) {
            result.put(block.block(), context.spriteAnalyzer.detailsFor(block.sprites()));
        }
        log.info("Assembled color data for {} blocks in {} ms", result.size(), elapsedMillis(started));
        return result;
    }

    private void awaitScanBatch() {
        long waitStarted = System.nanoTime();
        try {
            scanBatches.acquire();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new CancellationException("Color scan cancelled");
        }
        long waitMillis = elapsedMillis(waitStarted);
        if (waitMillis >= SLOW_BATCH_WAIT_MILLIS) {
            log.debug("Waited {} ms for next model-scan render tick permit", waitMillis);
        }
    }

    private static void checkCancellation() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Color scan cancelled");
        }
    }

    private record ModelBatch(
            int processedStates,
            List<Sprite> discoveredSprites,
            List<BlockSprites> updatedBlocks,
            long workNanos) {
    }

    private record BlockSprites(Block block, Set<Sprite> sprites) {
    }
}
