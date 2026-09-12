package io.github.robak132.mcrgb_forge.client.analysis;

import static io.github.robak132.mcrgb_forge.client.utils.Utils.nanosToMillis;
import static io.github.robak132.mcrgb_forge.client.utils.Utils.perSecond;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "mcrgb_forge")
final class ModelScanStats {

    private static final int PROGRESS_INTERVAL = 10;

    private final int totalStates;
    private final long started = System.nanoTime();
    private long modelWorkNanos;
    private int batchCount;
    private int nextLog = PROGRESS_INTERVAL;

    ModelScanStats(int totalStates) {
        this.totalStates = totalStates;
    }

    void recordBatch(
            int processedStates,
            long workNanos,
            int batchStates,
            int discoveredSprites,
            int completedBlocks,
            long submitMillis) {
        batchCount++;
        modelWorkNanos += workNanos;
        log.debug("Model batch #{}: states={}, sprites={}, completedBlocks={}, work={} ms, submit={} ms, "
                        + "progress={}/{}",
                batchCount, batchStates, discoveredSprites, completedBlocks, nanosToMillis(workNanos),
                submitMillis, processedStates, totalStates);

        int percentage = (int) ((long) processedStates * 100 / totalStates);
        logProgress(processedStates, percentage);
    }

    void logCompletion() {
        long elapsedNanos = System.nanoTime() - started;
        log.info("Model scan completed in {} ms: {} states in {} adaptive batches, {} states/s, "
                        + "{} ms client-thread model work, {} ms throttling/coordination",
                nanosToMillis(elapsedNanos), totalStates, batchCount, perSecond(totalStates, elapsedNanos),
                nanosToMillis(modelWorkNanos), nanosToMillis(Math.max(0, elapsedNanos - modelWorkNanos)));
    }

    private void logProgress(int processedStates, int percentage) {
        if (percentage < nextLog) {
            return;
        }
        long elapsedNanos = System.nanoTime() - started;
        long etaMillis = nanosToMillis(elapsedNanos * (totalStates - processedStates) / processedStates);
        log.info("Model scan progress: {}% ({}/{}), {} ms elapsed, {} states/s, ETA {} ms",
                percentage, processedStates, totalStates, nanosToMillis(elapsedNanos),
                perSecond(processedStates, elapsedNanos), etaMillis);
        nextLog = (percentage / PROGRESS_INTERVAL + 1) * PROGRESS_INTERVAL;
    }
}
