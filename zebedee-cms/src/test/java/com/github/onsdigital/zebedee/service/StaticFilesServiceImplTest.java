package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.dp.files.api.Client;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class StaticFilesServiceImplTest extends TestCase {

    @Mock
    Collection mockCollection;
    @Mock
    CollectionDescription mockCollectionDescription;
    @Mock
    Client mockApiClient;

    @Test(expected = IllegalArgumentException.class)
    public void testWhenCollectionIdIsBlankIllegalArgumentIsThrown() throws Exception {

        when(mockCollectionDescription.getId()).thenReturn("");

        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);

        StaticFilesService service = new StaticFilesServiceImpl(mockApiClient);
        service.publishCollection(mockCollection);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWhenCollectionIdIsNullIllegalArgumentIsThrown() throws Exception {

        when(mockCollectionDescription.getId()).thenReturn(null);

        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);

        StaticFilesService service = new StaticFilesServiceImpl(mockApiClient);
        service.publishCollection(mockCollection);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWhenCollectionsDescriptionIsNullIllegalArgumentIsThrown() throws Exception {
        when(mockCollection.getDescription()).thenReturn(null);

        StaticFilesService service = new StaticFilesServiceImpl(mockApiClient);
        service.publishCollection(mockCollection);
    }

    @Test
    public void testPublishCollectionWithCorrectCollectionID() {
        String collectionID = "123456789";
        when(mockCollectionDescription.getId()).thenReturn(collectionID);

        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);

        StaticFilesService service = new StaticFilesServiceImpl(mockApiClient);
        service.publishCollection(mockCollection);

        verify(mockApiClient, times(1)).publishCollection(collectionID);
    }
}