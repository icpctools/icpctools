package org.icpc.tools.presentation.core;

import java.util.HashMap;
import java.util.Map;

public class RenderPerfTimer {
    public enum Category {
        RANK_AND_SCORE,
        LOGO,
        NAME,
        PROBLEM,
        ACTIVE_PROBLEM,
    }

    public static class Counter implements AutoCloseable {
        long createNanos;
        int measurements;
        long totalNanos;
        long startNanos;

        public Counter() {
            createNanos = System.nanoTime();
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
            }
            return 1e9 * totalNanos / Math.max(elapsed, 1);
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

    public static RenderPerfTimer DEFAULT_INSTANCE = new RenderPerfTimer();

    public static Counter measure(Category category) {
        return DEFAULT_INSTANCE.getCounter(category).startMeasure();
    }
}
