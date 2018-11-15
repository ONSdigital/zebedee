package com.github.onsdigital.zebedee.search.fastText;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FastTextExecutorService implements AutoCloseable {

    private final ExecutorService executorService;

    public FastTextExecutorService() {
        this.executorService = Executors.newFixedThreadPool(8);
    }

    public <T> Future<T> submit(Callable<T> task) {
        return this.executorService.submit(task);
    }

    @Override
    public void close() {
        this.executorService.shutdown();
    }
}
