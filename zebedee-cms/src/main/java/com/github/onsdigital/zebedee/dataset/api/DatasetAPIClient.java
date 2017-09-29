package com.github.onsdigital.zebedee.dataset.api;

import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.verification.http.ClientConfiguration;
import com.github.onsdigital.zebedee.verification.http.PooledHttpClient;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * HTTP DatasetAPIClient for the dataset API.
 */
public class DatasetAPIClient implements DatasetClient {

    private static DatasetClient instance;
    private static String datasetAPIURL = Configuration.getDatasetAPIURL();
    private static PooledHttpClient client;

    private DatasetAPIClient() {
        // private constructor - use getInstance()
    }

    /**
     * Get the singleton instance of the DatasetAPIClient.
     *
     * @return
     */
    public static DatasetClient getInstance() {

        if (DatasetAPIClient.instance == null) {
            synchronized (DatasetAPIClient.class) {
                if (DatasetAPIClient.instance == null) {
                    DatasetAPIClient.instance = new DatasetAPIClient();
                    DatasetAPIClient.client = new PooledHttpClient(DatasetAPIClient.datasetAPIURL, new ClientConfiguration());
                }
            }
        }
        return DatasetAPIClient.instance;
    }

    /**
     * Get the dataset for the given dataset ID.
     *
     * @param datasetID
     * @return
     */
    @Override
    public Dataset getDataset(String datasetID) throws IOException, BadRequestException {

        if (StringUtils.isEmpty(datasetID)) {
            throw new BadRequestException("A dataset ID must be provided.");
        }

        String path = "/datasets/" + datasetID;

        logInfo("Calling dataset API").addParameter("path", path).log();

        try (CloseableHttpResponse response = client.sendGet(path, null, null)) {
            String responseString = EntityUtils.toString(response.getEntity());
            return ContentUtil.deserialise(responseString, Dataset.class);
        }
    }

    /**
     * Get the instance for the given instance ID.
     *
     * @param instanceID
     * @return
     */
    @Override
    public Instance getInstance(String instanceID) throws IOException, BadRequestException {

        if (StringUtils.isEmpty(instanceID)) {
            throw new BadRequestException("An instance ID must be provided.");
        }

        String path = "/instances/" + instanceID;

        logInfo("Calling dataset API").addParameter("path", path).log();

        try (CloseableHttpResponse response = client.sendGet(path, null, null)) {
            String responseString = EntityUtils.toString(response.getEntity());
            return ContentUtil.deserialise(responseString, Instance.class);
        }
    }
}
