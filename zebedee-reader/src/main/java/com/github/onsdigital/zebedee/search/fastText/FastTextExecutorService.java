package com.github.onsdigital.zebedee.search.fastText;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logError;
import static com.github.onsdigital.zebedee.util.VariableUtils.getEnv;
import static com.github.onsdigital.zebedee.util.VariableUtils.getSystemProperty;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;

public class FastTextExecutorService implements AutoCloseable {

    private static FastTextExecutorService INSTANCE;

    public static FastTextExecutorService getInstance() {
        if (INSTANCE == null) {
            synchronized (FastTextExecutorService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FastTextExecutorService();
                }
            }
        }
        return INSTANCE;
    }

    private final ExecutorService executorService;

    private FastTextExecutorService() {
        this.executorService = Executors.newFixedThreadPool(Configuration.getThreadPoolSize());
    }

    public <T> Future<T> submit(Callable<T> task) {
        return this.executorService.submit(task);
    }

    @Override
    public void close() {
        this.executorService.shutdown();
    }

    /**
     * TODO - Fix me
     */
    private static class Configuration {
        private static final int DEFAULT_POOL_SIZE = 8;
        private static final String THREAD_POOL_SIZE_KEY = "DP_FASTTEXT_THREAD_POOL_SIZE";

        public static final int getThreadPoolSize() {
            try {
                return Integer.valueOf(defaultIfBlank(getEnv(THREAD_POOL_SIZE_KEY),
                        defaultIfBlank(getSystemProperty(THREAD_POOL_SIZE_KEY), String.valueOf(DEFAULT_POOL_SIZE))));
            } catch (NumberFormatException e) {
                logError(e)
                        .addMessage("Caught exception parsing variable, returning default")
                        .addParameter("variable", THREAD_POOL_SIZE_KEY)
                        .addParameter("default", DEFAULT_POOL_SIZE)
                        .log();
                return DEFAULT_POOL_SIZE;
            }
        }
    }
}
