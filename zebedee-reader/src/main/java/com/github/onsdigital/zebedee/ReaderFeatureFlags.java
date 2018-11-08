package com.github.onsdigital.zebedee;

import com.github.onsdigital.zebedee.content.page.base.PageType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.github.onsdigital.zebedee.content.page.base.PageType.api_dataset;
import static com.github.onsdigital.zebedee.content.page.base.PageType.api_dataset_landing_page;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logInfo;

public class ReaderFeatureFlags {

    private static final String ENABLE_DATASET_IMPORT = "ENABLE_DATASET_IMPORT";

    private static ReaderFeatureFlags instance;

    private final boolean enableDatasetImport;
    private final Set<PageType> datasetPageTypes;

    /**
     * Construct new ReaderFeatureFlags and load the feature flags configs. ReaderFeatureFlags is a singleton instance
     * use {@link #readerFeatureFlags()} to obtain an instance of it.
     */
    private ReaderFeatureFlags() {
/*        this.enableDatasetImport = Boolean.valueOf(
                defaultIfBlank(getProperty(ENABLE_DATASET_IMPORT), getenv(ENABLE_DATASET_IMPORT)));*/

        this.enableDatasetImport = true;

        if (enableDatasetImport) {
            this.datasetPageTypes = new HashSet<>(Arrays.asList(api_dataset, api_dataset_landing_page));
        } else {
            this.datasetPageTypes = new HashSet<>();
        }

        logInfo("Reader feature flags configuration")
                .addParameter(ENABLE_DATASET_IMPORT, this.enableDatasetImport)
                .log();
    }

    public boolean isEnableDatasetImport() {
        return enableDatasetImport;
    }

    public Set<PageType> datasetImportPageTypes() {
        return datasetPageTypes;
    }

    public boolean isDatasetImportPageType(PageType pageType) {
        if (pageType == null) {
            return false;
        }
        return datasetImportPageTypes().contains(pageType);
    }

    public static ReaderFeatureFlags readerFeatureFlags() {
        logInfo("attempting to load reader feature flags").log();
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
