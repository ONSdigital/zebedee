package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.data.framework.DataPagesGenerator;
import com.github.onsdigital.zebedee.data.framework.DataPagesSet;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ContentReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class DataPublicationFinderTest {

    @Mock
    private ContentReader reviewedContentReader;

    @Mock
    private DataIndex dataIndex;

    private MockedConstruction<DataPublication> dataPublicationMock;
    private DataPagesGenerator generator;
    private ContentReader publishedReader;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        generator = new DataPagesGenerator();
    }

    @After
    public void tearDown() throws Exception {
        if (dataPublicationMock != null) {
            dataPublicationMock.close();
        }
    }

    @Test
    public void findPublications_givenCollectionWithNoData_returnsEmptyList() throws IOException, ZebedeeException {
        // Given
        // An empty collection
        when(reviewedContentReader.listUris()).thenReturn(new ArrayList<>());

        // When
        // we search for publications
        List<DataPublication> publications = new DataPublicationFinder().findPublications(publishedReader, reviewedContentReader);

        // Then
        // no data turns up
        assertEquals(0, publications.size());
    }

    @Test
    public void findPublications_givenCollectionWithData_returnsPublication() throws IOException, ZebedeeException, ParseException, URISyntaxException {
        // Given
        // A time series dataset in a collection
        DataPagesSet dataPagesSet = generator.generateDataPagesSet("node", "test", 2015, 2, "data.csdb");
        DataPublicationDetails expected = new DataPublicationDetails(dataPagesSet.timeSeriesDataset, dataPagesSet.datasetLandingPage, dataPagesSet.fileUri);
        when(reviewedContentReader.getContent(dataPagesSet.timeSeriesDataset.getUri().toString()))
                .thenReturn(dataPagesSet.timeSeriesDataset);

        ArrayList<String> uris = new ArrayList<>();
        uris.add(dataPagesSet.datasetLandingPage.getUri().toString() + "/data.json");
        uris.add(dataPagesSet.timeSeriesDataset.getUri().toString() + "/data.json");
        uris.add(dataPagesSet.fileUri);
        when(reviewedContentReader.listUris()).thenReturn(uris);

        dataPublicationMock = Mockito.mockConstruction(DataPublication.class, (mock, context) -> {
            String fileUri = (String) context.arguments().get(2);
            DataPublicationDetails details = null;
            if (fileUri.equals(dataPagesSet.timeSeriesDataset.getUri().toString())) {
                details = expected;
            }
            when(mock.getDetails()).thenReturn(details);
        });

        // When
        // we search for publications
        List<DataPublication> publications = new DataPublicationFinder().findPublications(publishedReader, reviewedContentReader);

        // Then
        // the correct dataset publication is identified
        assertEquals(1, publications.size());
        assertEquals(expected, publications.get(0).getDetails());
    }

    @Test
    public void findPublications_givenCollectionWithTwoDataPublications_returnsBoth() throws IOException, ZebedeeException, ParseException, URISyntaxException {
        // Given
        // Two time series dataset in the same collection
        DataPagesSet dataPagesSet1 = generator.generateDataPagesSet("node", "test", 2015, 2, "data.csdb");
        DataPublicationDetails expected1 = new DataPublicationDetails(dataPagesSet1.timeSeriesDataset, dataPagesSet1.datasetLandingPage, dataPagesSet1.fileUri);
        when(reviewedContentReader.getContent(dataPagesSet1.datasetLandingPage.getUri().toString()))
                .thenReturn(dataPagesSet1.datasetLandingPage);
        when(reviewedContentReader.getContent(dataPagesSet1.timeSeriesDataset.getUri().toString()))
                .thenReturn(dataPagesSet1.timeSeriesDataset);
        when(dataIndex.getUriForCdid(eq(dataPagesSet1.getTimeSerieses().get(0).getCdid())))
                .thenReturn(dataPagesSet1.getTimeSerieses().get(0).getUri().resolve(".").toString());

        DataPagesSet dataPagesSet2 = generator.generateDataPagesSet("node2", "test", 2015, 2, "data.csdb");
        DataPublicationDetails expected2 = new DataPublicationDetails(dataPagesSet2.timeSeriesDataset, dataPagesSet2.datasetLandingPage, dataPagesSet2.fileUri);
        when(reviewedContentReader.getContent(dataPagesSet2.datasetLandingPage.getUri().toString()))
                .thenReturn(dataPagesSet2.datasetLandingPage);
        when(reviewedContentReader.getContent(dataPagesSet2.timeSeriesDataset.getUri().toString()))
                .thenReturn(dataPagesSet2.timeSeriesDataset);
        when(dataIndex.getUriForCdid(eq(dataPagesSet2.getTimeSerieses().get(0).getCdid())))
                .thenReturn(dataPagesSet2.getTimeSerieses().get(0).getUri().resolve(".").toString());

        ArrayList<String> uris = new ArrayList<>();
        uris.add(dataPagesSet1.datasetLandingPage.getUri().toString() + "/data.json");
        uris.add(dataPagesSet1.timeSeriesDataset.getUri().toString() + "/data.json");
        uris.add(dataPagesSet1.fileUri);
        uris.add(dataPagesSet2.datasetLandingPage.getUri().toString() + "/data.json");
        uris.add(dataPagesSet2.timeSeriesDataset.getUri().toString() + "/data.json");
        uris.add(dataPagesSet2.fileUri);
        when(reviewedContentReader.listUris()).thenReturn(uris);

        dataPublicationMock = Mockito.mockConstruction(DataPublication.class, (mock, context) -> {
            String fileUri = (String) context.arguments().get(2);
            DataPublicationDetails details = null;
            if (fileUri.equals(dataPagesSet1.timeSeriesDataset.getUri().toString())) {
                details = expected1;
            } else if (fileUri.equals(dataPagesSet2.timeSeriesDataset.getUri().toString())) {
                details = expected2;
            }
            when(mock.getDetails()).thenReturn(details);
        });

        // When
        // we search for publications
        List<DataPublication> publications = new DataPublicationFinder().findPublications(publishedReader, reviewedContentReader);

        // Then
        // the correct dataset publications are identified
        assertEquals(2, publications.size());
        assertEquals(expected1, publications.get(0).getDetails());
        assertEquals(expected2, publications.get(1).getDetails());
    }
}