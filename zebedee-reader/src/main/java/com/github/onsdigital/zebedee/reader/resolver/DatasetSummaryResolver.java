package com.github.onsdigital.zebedee.reader.resolver;

import com.github.onsdigital.logging.v2.event.SimpleEvent;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.api.ReadRequestHandler;
import com.github.onsdigital.zebedee.reader.api.bean.DatasetSummary;
import com.github.onsdigital.zebedee.reader.util.DatasetAPIClientSupplier;
import dp.api.dataset.DatasetAPIClient;
import dp.api.dataset.exception.DatasetAPIException;
import dp.api.dataset.model.Dataset;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;

import static com.github.onsdigital.zebedee.logging.ReaderLogger.error;
import static com.github.onsdigital.zebedee.logging.ReaderLogger.info;
import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.get;

public class DatasetSummaryResolver {

    private static final String CMD_DATASET_LINK_PREFIX = "/datasets/";
    private static final String PAGE_URI = "page_uri";
    private static final String DATASET_URI = "dataset_uri";

    private DatasetAPIClient datasetAPIClient;
    private boolean isDatasetImportEnabled;

    private static DatasetSummaryResolver INSTANCE = null;

    public static DatasetSummaryResolver getInstance() throws ZebedeeException {
        if (INSTANCE == null) {
            synchronized (DatasetSummaryResolver.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DatasetSummaryResolver();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * @throws ZebedeeException
     */
    DatasetSummaryResolver() throws ZebedeeException {
        this.isDatasetImportEnabled = get().isDatasetImportEnabled();
        if (isDatasetImportEnabled) {
            this.datasetAPIClient = DatasetAPIClientSupplier.get();
        }
    }

    DatasetSummaryResolver(DatasetAPIClient datasetAPIClient, boolean isDatasetImportEnabled) {
        this.datasetAPIClient = datasetAPIClient;
        this.isDatasetImportEnabled = isDatasetImportEnabled;
    }

    public DatasetSummary resolve(String pageURI, Link datasetLink, HttpServletRequest request,
                                  ReadRequestHandler handler) {
        SimpleEvent event = info().data(PAGE_URI, pageURI).data(DATASET_URI, datasetLink.getUri().toString());

        if (datasetLink != null && datasetLink.getUri().toString().startsWith(CMD_DATASET_LINK_PREFIX)) {
            if (isDatasetImportEnabled) {
                event.log("uri identified as cmd dataset cmd feature flag is enabled resolving summary details");
                return getCMDDatasetSummary(pageURI, datasetLink);
            }
            event.log("uri identified as cmd dataset cmd feature flag is disabled summary details will not " +
                    "be resolved or include in results");
            return null;
        }

        event.log("uri identified as legacy dataset resolving summary details");
        return getLegacyDatasetSummary(pageURI, datasetLink, request, handler);
    }

    /**
     * Get a dataset summary for a old world legacy dataset.
     */
    DatasetSummary getLegacyDatasetSummary(String pageURI, Link datasetLink, HttpServletRequest request,
                                           ReadRequestHandler handler) {
        String datasetURI = datasetLink.getUri().toString();
        try {
            DatasetLandingPage dlp = (DatasetLandingPage) handler.getContent(datasetURI, request);
            return new DatasetSummary(dlp);
        } catch (ZebedeeException | IOException e) {
            error().exception(e)
                    .data(PAGE_URI, pageURI)
                    .data(DATASET_URI, datasetURI)
                    .log("error resolving legacy dataset summary, dataset will be ommitted from the results");
            return null;
        }
    }

    /**
     * Get a dataset summary for a new world CMD dataset.
     */
    DatasetSummary getCMDDatasetSummary(String pageURI, Link dataset) {
        String uri = dataset.getUri().toString();
        try {
            String datasetID = getCMDDatasetID(pageURI, dataset);
            Dataset d = datasetAPIClient.getDataset(datasetID);
            return new DatasetSummary(d);
        } catch (IllegalArgumentException e) {
            // error already logged
            return null;
        } catch (IOException | URISyntaxException | DatasetAPIException e) {
            error().exception(e)
                    .data(PAGE_URI, pageURI)
                    .data(DATASET_URI, uri)
                    .log("error getting cmd dataset details dataset API client returned an error, summary details " +
                            "will be ommitted from the results");
            return null;
        }
    }

    String getCMDDatasetID(String pageURI, Link dataset) throws IllegalArgumentException {
        String uri = dataset.getUri().toString();
        String[] sections = uri.split("/");
        if (sections.length < 3) {
            error().data(PAGE_URI, pageURI)
                    .data(DATASET_URI, uri)
                    .log("error parsing cmd dataset uri could not determined dataset ID from uri, summary details " +
                            "will be ommitted from result");
            throw new IllegalArgumentException("unable to  determined dataset ID from uri");
        }
        return sections[2];
    }
}
