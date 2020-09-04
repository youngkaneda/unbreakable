package org.capsules.the;

import java.util.concurrent.*;
import java.util.function.Supplier;

public class CircuitBreaker<E, T extends Supplier<E>> {

    private final T block;
    private final int threshold;
    private final long timeout;
    private int failureMonitor;
    private ExecutorService service;

    public CircuitBreaker(T block, int threshold, long timeout) {
        this.block = block;
        this.threshold = threshold;
        this.timeout = timeout;
        this.failureMonitor = 0;
        this.service = Executors.newFixedThreadPool(1);
    }

    public E call() throws ExecutionException, InterruptedException, TimeoutException {
        if (!isClosed()) {
            throw new TimeoutException("Circuit Breaker timeout.");
        }
        Callable<E> task = block::get;
        FutureTask<E> future = new FutureTask<>(task);
        this.service.submit(future);
        long now = System.currentTimeMillis();
        while (!future.isDone()) {
            if (System.currentTimeMillis() - now > timeout) {
                failureMonitor++;
                if (failureMonitor == threshold) {
                    // start self healing job
                }
                throw new TimeoutException("Circuit Breaker timeout.");
            }
        }
        return future.get();
    }

    private boolean isClosed() {
        return threshold > failureMonitor;
    }

    private void release() {
        service.shutdown();
    }
}
