package io.github.robak132.mcrgb_forge.client.analysis;

import static io.github.robak132.mcrgb_forge.client.utils.Utils.nanosToMillis;
import static io.github.robak132.mcrgb_forge.client.utils.Utils.perSecond;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "mcrgb_forge")
final class SpriteAnalysisStats {

    private static final int PROGRESS_SPRITE_STEP = 250;
    private static final long SLOW_SPRITE_MILLIS = 25L;

    private final long started = System.nanoTime();
    private long pixelReadNanos;
    private long clusteringNanos;
    private long noiseNanos;
    private long totalPixels;
    private long visiblePixels;
    private long slowestSpriteNanos;
    private String slowestSprite = "none";
    private int processedSprites;
    private int submittedSprites;
    private int nextProgressAt = PROGRESS_SPRITE_STEP;

    void recordSubmitted(int count) {
        submittedSprites += count;
        log.debug("Queued {} sprites for texture analysis (submitted total={})", count, submittedSprites);
    }

    void recordPixelRead(Sprite sprite, long elapsedNanos) {
        pixelReadNanos += elapsedNanos;
        totalPixels += (long) sprite.width() * sprite.height();
        visiblePixels += sprite.visiblePixels().size();
    }

    void recordClustering(long elapsedNanos) {
        clusteringNanos += elapsedNanos;
    }

    void recordNoise(long elapsedNanos) {
        noiseNanos += elapsedNanos;
    }

    void recordSprite(Sprite sprite, long elapsedNanos) {
        if (elapsedNanos > slowestSpriteNanos) {
            slowestSpriteNanos = elapsedNanos;
            slowestSprite = spriteLabel(sprite);
        }
        long elapsedMillis = nanosToMillis(elapsedNanos);
        if (elapsedMillis >= SLOW_SPRITE_MILLIS) {
            log.debug("Slow sprite analysis: {} in {} ms", spriteLabel(sprite), elapsedMillis);
        }
        processedSprites++;
        logProgress();
    }

    void log() {
        long elapsedNanos = System.nanoTime() - started;
        log.info("Texture analysis completed in {} ms: {} sprites, {} sprites/s, {} total pixels, "
                        + "{} visible pixels; reads={} ms, clustering={} ms, noise={} ms",
                nanosToMillis(elapsedNanos), processedSprites, perSecond(processedSprites, elapsedNanos),
                totalPixels, visiblePixels, nanosToMillis(pixelReadNanos), nanosToMillis(clusteringNanos),
                nanosToMillis(noiseNanos));
        log.info("Slowest sprite analysis: {} in {} ms",
                slowestSprite, nanosToMillis(slowestSpriteNanos));
    }

    private void logProgress() {
        if (processedSprites < nextProgressAt) {
            return;
        }
        long elapsedNanos = System.nanoTime() - started;
        log.info("Texture analysis progress: {}/{} sprites, {} ms elapsed, {} sprites/s",
                processedSprites, submittedSprites, nanosToMillis(elapsedNanos),
                perSecond(processedSprites, elapsedNanos));
        nextProgressAt += PROGRESS_SPRITE_STEP;
    }

    private static String spriteLabel(Sprite sprite) {
        return sprite.name() + " (" + sprite.width() + "x" + sprite.height() + ")";
    }
}
