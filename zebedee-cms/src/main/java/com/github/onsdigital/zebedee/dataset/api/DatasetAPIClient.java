package com.github.onsdigital.zebedee.dataset.api;

import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.verification.http.ClientConfiguration;
import com.github.onsdigital.zebedee.verification.http.PooledHttpClient;
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
    public Dataset getDataset(String datasetID) throws IOException {

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
    public Instance getInstance(String instanceID) throws IOException {

        String path = "/instances/" + instanceID;

        logInfo("Calling dataset API").addParameter("path", path).log();

        try (CloseableHttpResponse response = client.sendGet(path, null, null)) {
            String responseString = EntityUtils.toString(response.getEntity());
            return ContentUtil.deserialise(responseString, Instance.class);
        }
    }

    public static void main(String[] args) throws IOException {
        Instance instance = DatasetAPIClient.getInstance().getInstance("763e0c3f-02e9-4261-bf50-1727e5a6bd25");
        System.out.println("Instance! " + instance.getId());
    }
}
