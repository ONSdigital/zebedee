package com.github.onsdigital.zebedee;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.github.onsdigital.zebedee.content.page.base.PageType.api_dataset;
import static com.github.onsdigital.zebedee.content.page.base.PageType.api_dataset_landing_page;
import static com.github.onsdigital.zebedee.logging.ReaderLogger.info;
import static com.github.onsdigital.zebedee.logging.ReaderLogger.warn;

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
        this.enableDatasetImport = Boolean.valueOf(getConfigValue(ENABLE_DATASET_IMPORT));

        if (enableDatasetImport) {
            this.datasetPageTypes = new HashSet<>(Arrays.asList(api_dataset, api_dataset_landing_page));
        } else {
            this.datasetPageTypes = new HashSet<>();
        }

        info().data(ENABLE_DATASET_IMPORT, this.enableDatasetImport).log("Reader feature flags configuration");
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


    public static String getConfigValue(String name) {
        String value = System.getProperty(name);
        if (StringUtils.isNoneEmpty(value)) {
            info().data(name, value).log("applying Reader feature flag config value from system.properties");
            return value;
        }

        value = System.getenv(name);
        if (StringUtils.isNoneEmpty(value)) {
            info().data(name, value).log("applying Reader feature flag config value from system.env");
            return value;
        }

        warn().data("name", name).log("Reader config value not found in system.properties or system.env default will be applied");
        return "";
    }

    public static ReaderFeatureFlags readerFeatureFlags() {
        if (instance == null) {
            info().log("attempting to load reader feature flags");
            synchronized (ReaderFeatureFlags.class) {
                if (instance == null) {
                    instance = new ReaderFeatureFlags();
                }
            }
        }
        return instance;
    }
}
