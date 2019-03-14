package com.github.onsdigital.zebedee.content.page;

import com.github.onsdigital.zebedee.content.page.statistics.dataset.ApiDatasetLandingPage;
import dp.api.dataset.DatasetClient;
import dp.api.dataset.exception.DatasetAPIException;
import dp.api.dataset.exception.UnexpectedResponseException;
import org.apache.http.HttpStatus;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class APIDatasetLandingPageDeletionHookTest {

    private String uri = "some/uri";
    String datasetId = "123";

    @Test
    public void testDatasetLandingPageDeletionHook_onPageUpdated() throws IOException, DatasetAPIException {


        // Given a APIDatasetLandingPageDeletionHook
        DatasetClient mockDatasetClient = mock(DatasetClient.class);

        APIDatasetLandingPageDeletionHook deletionHook = new APIDatasetLandingPageDeletionHook(mockDatasetClient);

        ApiDatasetLandingPage page = new ApiDatasetLandingPage();
        page.setapiDatasetId(datasetId);

        // When onPageUpdated is called
        deletionHook.onPageUpdated(page, uri);

        // Then the dataset client is called to delete the dataset
        verify(mockDatasetClient, times(1)).deleteDataset(datasetId);
        verify(mockDatasetClient, only()).deleteDataset(datasetId);
    }

    @Test(expected = RuntimeException.class)
    public void testDatasetLandingPageDeletionHook_onPageUpdated_exception() throws IOException, DatasetAPIException {

        // Given a APIDatasetLandingPageDeletionHook with a mock client that throws an exception
        DatasetClient mockDatasetClient = mock(DatasetClient.class);

        UnexpectedResponseException broken = new UnexpectedResponseException("broken", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        doThrow(broken)
                .when(mockDatasetClient).deleteDataset(datasetId);

        APIDatasetLandingPageDeletionHook deletionHook = new APIDatasetLandingPageDeletionHook(mockDatasetClient);
        ApiDatasetLandingPage page = new ApiDatasetLandingPage();
        page.setapiDatasetId(datasetId);

        // When onPageUpdated is called
        deletionHook.onPageUpdated(page, "some/uri");

        // Then a runtime exception is thrown
    }
}
