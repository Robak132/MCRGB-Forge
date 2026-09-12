package io.github.robak132.mcrgb_forge.client.analysis;

import static io.github.robak132.mcrgb_forge.client.utils.Utils.elapsedMillis;

import io.github.robak132.libgui_forge.widget.data.colors.RGB;
import io.github.robak132.mcrgb_forge.config.MCRGBConfig.ColorCalculationMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "mcrgb_forge")
final class SpriteAnalyzer implements AutoCloseable {

    private static final int K_VALUE = 5;
    private static final int MAX_ITERATIONS = 8;
    private static final int SAMPLE_LIMIT = 4096;

    private final ColorCalculationMode mode;
    private final ExecutorService executor = createExecutor();
    private final AtomicReference<RuntimeException> failure = new AtomicReference<>();
    private final Map<Sprite, SpriteDetails> detailsBySprite = new HashMap<>();
    private final SpriteAnalysisStats stats = new SpriteAnalysisStats();

    SpriteAnalyzer(ColorCalculationMode mode) {
        this.mode = mode;
        log.debug("Created texture analyzer; mode={}", mode);
    }

    void submit(List<Sprite> sprites, Runnable onComplete) {
        if (!sprites.isEmpty()) {
            stats.recordSubmitted(sprites.size());
        }
        executor.submit(() -> {
            analyze(sprites);
            if (failure.get() == null) {
                onComplete.run();
            }
        });
    }

    void awaitCompletion() {
        long started = System.nanoTime();
        log.debug("Waiting for pipelined texture analysis to drain");
        await(executor.submit(() -> { }));
        log.info("Pipelined texture analysis drain completed in {} ms", elapsedMillis(started));
        stats.log();
    }

    void checkFailure() {
        RuntimeException error = failure.get();
        if (error != null) {
            throw error;
        }
    }

    List<SpriteDetails> detailsFor(Collection<Sprite> sprites) {
        return sprites.stream()
                .map(detailsBySprite::get)
                .filter(Objects::nonNull)
                .toList();
    }

    int retainedSpriteCount() {
        return detailsBySprite.size();
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private void analyze(List<Sprite> sprites) {
        if (failure.get() != null) {
            return;
        }
        try {
            for (Sprite sprite : sprites) {
                analyze(sprite);
            }
        } catch (RuntimeException error) {
            failure.compareAndSet(null, error);
        }
    }

    private void analyze(Sprite sprite) {
        checkCancellation();
        long started = System.nanoTime();
        sprite.loadPixels();
        stats.recordPixelRead(sprite, System.nanoTime() - started);

        try {
            List<RGB> visiblePixels = sprite.visiblePixels();
            if (visiblePixels.isEmpty()) {
                return;
            }

            long clusteringStarted = System.nanoTime();
            List<SpriteColor> colors = clusterColors(visiblePixels);
            stats.recordClustering(System.nanoTime() - clusteringStarted);

            long noiseStarted = System.nanoTime();
            TextureNoise noise = TextureNoiseAnalyzer.analyze(
                    sprite.spatialPixels(), sprite.width(), sprite.height());
            stats.recordNoise(System.nanoTime() - noiseStarted);
            detailsBySprite.put(sprite, new SpriteDetails(sprite.name().toString(), colors, noise));
        } finally {
            sprite.releasePixels();
            stats.recordSprite(sprite, System.nanoTime() - started);
        }
    }

    private List<SpriteColor> clusterColors(List<RGB> pixels) {
        return switch (mode) {
            case OKLAB -> ColorClustering.kMeansOkLab(pixels, K_VALUE, MAX_ITERATIONS, SAMPLE_LIMIT);
            case FABRIC -> ColorClustering.fabric(pixels);
            case MEAN -> ColorClustering.mean(pixels);
            case MEDIAN -> ColorClustering.median(pixels);
        };
    }

    private void await(Future<?> completion) {
        try {
            completion.get();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new CancellationException("Color scan cancelled");
        } catch (ExecutionException error) {
            throw new IllegalStateException("Texture analysis failed", error.getCause());
        }
        checkFailure();
    }

    private static void checkCancellation() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Color scan cancelled");
        }
    }

    private static ExecutorService createExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "MCRGB Texture Analyzer");
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        });
    }

}
