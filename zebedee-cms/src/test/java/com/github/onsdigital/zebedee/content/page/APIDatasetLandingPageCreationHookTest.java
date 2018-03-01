package com.github.onsdigital.zebedee.content.page;

import com.github.onsdigital.zebedee.content.page.statistics.dataset.ApiDatasetLandingPage;
import dp.api.dataset.DatasetClient;
import dp.api.dataset.exception.DatasetAPIException;
import dp.api.dataset.exception.UnexpectedResponseException;
import dp.api.dataset.model.Dataset;
import org.apache.http.HttpStatus;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class APIDatasetLandingPageCreationHookTest {

    @Test
    public void testDatasetLandingPageCreationHook_onPageUpdated() throws IOException, DatasetAPIException {

        String datasetId = "123";
        Dataset dataset = new Dataset();

        // Given a APIDatasetLandingPageCreationHook
        DatasetClient mockDatasetClient = mock(DatasetClient.class);
        when(mockDatasetClient.createDataset(anyString(), any(Dataset.class))).thenReturn(dataset);

        APIDatasetLandingPageCreationHook creationHook = new APIDatasetLandingPageCreationHook(mockDatasetClient);

        ApiDatasetLandingPage page = new ApiDatasetLandingPage();
        page.setapiDatasetId(datasetId);

        // When onPageUpdated is called
        creationHook.onPageUpdated(page, "some/uri");

        // Then the dataset client is called to create the dataset
        verify(mockDatasetClient, times(1)).createDataset(anyString(), any(Dataset.class));
        verify(mockDatasetClient, only()).createDataset(anyString(), any(Dataset.class));
    }


    @Test(expected = RuntimeException.class)
    public void testDatasetLandingPageCreationHook_onPageUpdated_exception() throws IOException, DatasetAPIException {

        // Given a APIDatasetLandingPageCreationHook with a mock client that throws an exception
        DatasetClient mockDatasetClient = mock(DatasetClient.class);
        when(mockDatasetClient.createDataset(anyString(), any(Dataset.class)))
                .thenThrow(new UnexpectedResponseException("broken", HttpStatus.SC_FORBIDDEN));

        APIDatasetLandingPageCreationHook creationHook = new APIDatasetLandingPageCreationHook(mockDatasetClient);
        ApiDatasetLandingPage page = new ApiDatasetLandingPage();

        // When onPageUpdated is called
        creationHook.onPageUpdated(page, "some/uri");

        // Then a runtime exception is thrown
    }
}
