package com.github.onsdigital.zebedee.dataset.api;

import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.dataset.api.exception.BadRequestException;
import com.github.onsdigital.zebedee.dataset.api.exception.DatasetNotFoundException;
import com.github.onsdigital.zebedee.dataset.api.exception.UnexpectedResponseException;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.management.InstanceNotFoundException;
import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * HTTP DatasetAPIClient for the dataset API.
 */
public class DatasetAPIClient implements DatasetClient {

    private static DatasetClient instance;
    private static String datasetAPIURL = Configuration.getDatasetAPIURL();
    private static String datasetAPIAuthToken = Configuration.getDatasetAPIAuthToken();

    private static CloseableHttpClient client = HttpClients.createDefault();

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
    public Dataset getDataset(String datasetID) throws IOException, DatasetNotFoundException, UnexpectedResponseException, BadRequestException {

        if (StringUtils.isEmpty(datasetID)) {
            throw new BadRequestException("A dataset ID must be provided.");
        }

        String path = "/datasets/" + datasetID;

        logInfo("Calling dataset API")
                .addParameter("path", path)
                .addParameter("method", "GET")
                .log();

        HttpGet httpget = new HttpGet(datasetAPIURL + path);

        try (CloseableHttpResponse response = client.execute(httpget)) {

            switch (response.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_OK:
                    String responseString = EntityUtils.toString(response.getEntity());
                    return ContentUtil.deserialise(responseString, Dataset.class);
                case HttpStatus.SC_NOT_FOUND:
                    throw new DatasetNotFoundException("The dataset API returned 404 for " + path);
                default:
                    throw new UnexpectedResponseException(
                            String.format("The dataset API returned a %s response for %s",
                                    response.getStatusLine().getStatusCode(),
                                    path));
            }
        }
    }

    /**
     * Get the instance for the given instance ID.
     *
     * @param instanceID
     * @return
     */
    @Override
    public Instance getInstance(String instanceID) throws IOException, InstanceNotFoundException, UnexpectedResponseException, BadRequestException {

        if (StringUtils.isEmpty(instanceID)) {
            throw new BadRequestException("An instance ID must be provided.");
        }

        String path = "/instances/" + instanceID;

        logInfo("Calling dataset API")
                .addParameter("path", path)
                .addParameter("method", "GET")
                .log();

        HttpGet httpget = new HttpGet(datasetAPIURL + path);

        try (CloseableHttpResponse response = client.execute(httpget)) {

            switch (response.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_OK:
                    String responseString = EntityUtils.toString(response.getEntity());
                    return ContentUtil.deserialise(responseString, Instance.class);
                case HttpStatus.SC_NOT_FOUND:
                    throw new InstanceNotFoundException("The dataset API returned 404 for " + path);
                default:
                    throw new UnexpectedResponseException(
                            String.format("The dataset API returned a %s response for %s",
                                    response.getStatusLine().getStatusCode(),
                                    path));
            }
        }
    }

    /**
     * Update the dataset for the given dataset ID with the given dataset instance data.
     * @param datasetID
     * @param dataset
     */
    @Override
    public Dataset updateDataset(String datasetID, Dataset dataset) throws BadRequestException, IOException, DatasetNotFoundException, UnexpectedResponseException {

        if (StringUtils.isEmpty(datasetID)) {
            throw new BadRequestException("A dataset ID must be provided.");
        }

        String path = "/datasets/" + datasetID;

        logInfo("Calling dataset API")
                .addParameter("path", path)
                .addParameter("method", "PUT")
                .log();

        HttpPut httpPut = new HttpPut(datasetAPIURL + path);
        httpPut.setHeader("internal-token", datasetAPIAuthToken);
        httpPut.setHeader("Content-Type", "application/json");

        String datasetJson = ContentUtil.serialise(dataset);
        StringEntity stringEntity = new StringEntity(datasetJson);
        httpPut.setEntity(stringEntity);

        try (CloseableHttpResponse response = client.execute(httpPut)) {

            switch (response.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_OK:
                    String responseString = EntityUtils.toString(response.getEntity());
                    return ContentUtil.deserialise(responseString, Dataset.class);
                case HttpStatus.SC_NOT_FOUND:
                    throw new DatasetNotFoundException("The dataset API returned 404 for " + path);
                default:
                    throw new UnexpectedResponseException(
                            String.format("The dataset API returned a %s response for %s",
                                    response.getStatusLine().getStatusCode(),
                                    path));
            }
        }
    }
}
