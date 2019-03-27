package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import dp.api.dataset.DatasetAPIClient;

import static com.github.onsdigital.zebedee.ReaderFeatureFlags.readerFeatureFlags;
import static com.github.onsdigital.zebedee.logging.ReaderLogger.error;
import static com.github.onsdigital.zebedee.logging.ReaderLogger.info;
import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.getDatasetAPIAuthToken;
import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.getDatasetAPIHost;
import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.getServiceAuthToken;

public class DatasetAPIClientSupplier {

    private static DatasetAPIClient INSTANCE = null;

    public static DatasetAPIClient get() throws ZebedeeException {
        if (readerFeatureFlags().isEnableDatasetImport()) {
            info().log("cmd feature flag is enabled returning datasetAPIClient instance");
            return getInstance();
        }
        info().log("cmd feature flag is not enabled returning null datasetAPIClient instance");
        return null;
    }

    private static DatasetAPIClient getInstance() throws ZebedeeException {
        if (INSTANCE == null) {
            synchronized (DatasetAPIClientSupplier.class) {
                if (INSTANCE == null) {
                    info().log("creating new instance of datasetAPIClient");
                    try {
                        INSTANCE = new DatasetAPIClient(
                                getDatasetAPIHost(),
                                getDatasetAPIAuthToken(),
                                getServiceAuthToken());
                    } catch (Exception e) {
                        ZebedeeException ex = new InternalServerError("error initalising dataset api client", e);
                        throw error().logException(ex, "error instantiating datasetAPIClient instabce");
                    }
                }
            }
        }
        return INSTANCE;
    }


}
