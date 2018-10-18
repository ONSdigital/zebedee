package com.github.onsdigital.zebedee;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logInfo;
import static java.lang.System.getProperty;
import static java.lang.System.getenv;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

public class ReaderFeatureFlags {

    private static final String ENABLE_DATASET_IMPORT = "ENABLE_DATASET_IMPORT";

    private static ReaderFeatureFlags instance;

    private final boolean enableDatasetImport;

    /**
     * Construct new ReaderFeatureFlags and load the feature flags configs. ReaderFeatureFlags is a singleton instance
     * use {@link #readerFeatureFlags()} to obtain an instance of it.
     */
    private ReaderFeatureFlags() {
        this.enableDatasetImport = Boolean.valueOf(
                defaultIfBlank(getProperty(ENABLE_DATASET_IMPORT), getenv(ENABLE_DATASET_IMPORT)));

        logInfo("Reader feature flags configuration")
                .addParameter(ENABLE_DATASET_IMPORT, this.enableDatasetImport)
                .log();
    }

    public boolean isEnableDatasetImport() {
        return Boolean.valueOf(defaultIfBlank(
                getProperty(ENABLE_DATASET_IMPORT), getenv(ENABLE_DATASET_IMPORT)));
    }

    public static ReaderFeatureFlags readerFeatureFlags() {
        if (instance == null) {
            synchronized (ReaderFeatureFlags.class) {
                if (instance == null) {
                    instance = new ReaderFeatureFlags();
                }
            }
        }
        return instance;
    }
}
