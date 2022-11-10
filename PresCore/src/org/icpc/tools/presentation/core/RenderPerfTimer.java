package org.icpc.tools.presentation.core;

import java.util.HashMap;
import java.util.Map;

public class RenderPerfTimer {
    public static final int N_FRAMES = 3 * 60;

    public enum Category {
        GC,
        GC_COUNT,
        SYNC,
        RANK_AND_SCORE,
        LOGO,
        NAME,
        PROBLEM,
        PROBLEM_DRAW,
        INACTIVE_PROBLEM,
        INACTIVE_PROBLEM2,
        INACTIVE_PROBLEM3,
        ACTIVE_PROBLEM,
        FRAME,
    }

    public static class Counter implements AutoCloseable {
        long createNanos;
        int measurements;
        long totalNanos;
        long startNanos;

        long lastFrameNanos;
        long[] frameNanos = new long[N_FRAMES];

        public Counter() {
            createNanos = System.nanoTime();
        }

        public void addMeasurement(double nanos) {
            measurements++;
            totalNanos += nanos;
        }

        public Counter startMeasure() {
            startNanos = System.nanoTime();
            return this;
        }

        public void stopMeasure() {
            long nanos = System.nanoTime();
            measurements++;
            totalNanos += nanos - startNanos;
        }

        public double nanosPerSecond() {
            long nanos = System.nanoTime();
            long elapsed = nanos - createNanos;
            if (elapsed > 1e9) {
                elapsed /= 2;
                createNanos += elapsed;
                measurements /= 2;
                totalNanos /= 2;
                lastFrameNanos = totalNanos;
            }
            return 1e9 * totalNanos / Math.max(elapsed, 1);
        }

        public void frame() {
            for (int i = 0; i < frameNanos.length - 1; i++) {
                frameNanos[i] = frameNanos[i + 1];
            }
            frameNanos[frameNanos.length - 1] = totalNanos - lastFrameNanos;
            lastFrameNanos = totalNanos;
        }

        public long[] getFrameNanos() {
            return frameNanos;
        }

        @Override
        public void close() throws Exception {
            stopMeasure();
        }
    }

    Map<Category, Counter> counters = new HashMap<>();

    public Counter getCounter(Category category) {
        Counter counter = counters.get(category);
        if (counter == null) {
            counter = new Counter();
            counters.put(category, counter);
        }
        return counter;
    }

    private long[] frameStartNanos = new long[N_FRAMES];

    public void frame() {
        for (Counter counter : counters.values()) {
            counter.frame();
        }
        for (int i = 0; i < frameStartNanos.length - 1; i++) {
            frameStartNanos[i] = frameStartNanos[i + 1];
        }
        frameStartNanos[frameStartNanos.length - 1] = System.nanoTime();
    }

    public void frameReset() {
        for (Counter counter : counters.values()) {
            counter.lastFrameNanos = counter.totalNanos;
        }
    }

    public long[] getFrameStartNanos() {
        return frameStartNanos;
    }

    public static RenderPerfTimer DEFAULT_INSTANCE = new RenderPerfTimer();

    public static Counter measure(Category category) {
        return DEFAULT_INSTANCE.getCounter(category).startMeasure();
    }
}
