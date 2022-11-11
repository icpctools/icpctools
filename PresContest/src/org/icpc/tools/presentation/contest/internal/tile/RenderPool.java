package org.icpc.tools.presentation.contest.internal.tile;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RenderPool {
    ExecutorService executor;

    public RenderPool() {
        executor = Executors.newWorkStealingPool();
    }

    public ExecutorService getExecutor() {
        return executor;
    }
}
