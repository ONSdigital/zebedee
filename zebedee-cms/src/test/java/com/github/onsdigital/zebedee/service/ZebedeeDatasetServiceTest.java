package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.dataset.api.Dataset;
import com.github.onsdigital.zebedee.dataset.api.DatasetClient;
import com.github.onsdigital.zebedee.json.CollectionDataset;
import com.github.onsdigital.zebedee.json.CollectionDatasetVersion;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZebedeeDatasetServiceTest {

    String datasetID = "321";
    String edition = "2015";
    String version = "1";
    String collectionID = "345";
    String initialState = "in-progress";

    Dataset dataset = new Dataset();

    DatasetClient mockDatasetAPI = mock(DatasetClient.class);
    ZebedeeCmsService mockZebedee = mock(ZebedeeCmsService.class);
    Collection mockCollection = mock(Collection.class);
    CollectionDescription mockCollectionDescription = mock(CollectionDescription.class);

    CollectionDataset collectionDataset = new CollectionDataset();
    CollectionDatasetVersion collectionDatasetVersion = new CollectionDatasetVersion();

    @Before
    public void setUp() throws Exception {

        dataset.setId(datasetID);
        dataset.setTitle("Dataset title");
        dataset.setUri("/the/dataset/uri");

        when(mockDatasetAPI.getDataset(datasetID)).thenReturn(dataset);
        when(mockZebedee.getCollection(collectionID)).thenReturn(mockCollection);
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);

        collectionDataset.setState(initialState);
        collectionDatasetVersion.setState(initialState);
    }

    @Test
    public void TestDatasetService_addDatasetToCollection_UpdatesStateWhenAlreadyInCollection() throws Exception {

        // Given a mockCollection that already contains the dataset
        when(mockCollectionDescription.getDataset(datasetID)).thenReturn(Optional.of(new CollectionDataset()));
        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI, mockZebedee);

        // When updateDatasetInCollection is called with an updated state
        String state = "reviewed";
        collectionDataset.setState(state);
        CollectionDataset updated = service.updateDatasetInCollection(collectionID, datasetID, collectionDataset);

        // Then the updated dataset contains the updated state
        ArgumentCaptor<CollectionDataset> datasetArgumentCaptor = ArgumentCaptor.forClass(CollectionDataset.class);
        verify(mockCollectionDescription, times(1)).addDataset(datasetArgumentCaptor.capture());
        Assert.assertEquals(datasetArgumentCaptor.getAllValues().get(0).getState(), state);

        Assert.assertEquals(updated.getState(), state);
    }

    @Test
    public void TestDatasetService_updateDatasetVersionToCollection_UpdatesStateWhenAlreadyInCollection() throws Exception {

        // Given a mockCollection that already contains a dataset version
        when(mockCollectionDescription.getDatasetVersion(datasetID, edition, version))
                .thenReturn(Optional.of(new CollectionDatasetVersion()));

        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI, mockZebedee);

        // When updateDatasetInCollection is called
        String state = "reviewed";
        collectionDatasetVersion.setState(state);
        CollectionDatasetVersion updated = service.updateDatasetVersionInCollection(collectionID, datasetID, edition, version, collectionDatasetVersion);

        // Then the updated dataset contains the updated state
        ArgumentCaptor<CollectionDatasetVersion> versionArgumentCaptor = ArgumentCaptor.forClass(CollectionDatasetVersion.class);
        verify(mockCollectionDescription, times(1)).addDatasetVersion(versionArgumentCaptor.capture());
        Assert.assertEquals(versionArgumentCaptor.getAllValues().get(0).getState(), state);

        Assert.assertEquals(updated.getState(), state);
    }

    @Test
    public void TestDatasetService_updateDatasetInCollection() throws Exception {

        // Given a mockCollection that contains no datasets
        when(mockCollectionDescription.getDataset(datasetID)).thenReturn(Optional.empty());
        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI, mockZebedee);

        // When updateDatasetInCollection is called
        CollectionDataset actualDataset = service.updateDatasetInCollection(collectionID, datasetID, collectionDataset);

        // Then the dataset API is called to populate the dataset data, and its added to the collection.
        verify(mockDatasetAPI, times(1)).getDataset(datasetID);
        verify(mockCollectionDescription, times(1)).addDataset(actualDataset);
        verify(mockCollection, times(1)).save();

        // The returned dataset has the expected values.
        Assert.assertEquals(dataset.getId(), actualDataset.getId());
        Assert.assertEquals(dataset.getTitle(), actualDataset.getTitle());
        Assert.assertEquals(initialState, actualDataset.getState());
    }

    @Test
    public void TestDatasetService_addDatasetVersionToCollection() throws Exception {

        // Given a mockCollection that contains no dataset versions
        when(mockCollectionDescription.getDatasetVersion(datasetID,edition,version)).thenReturn(Optional.empty());
        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI, mockZebedee);

        // When updateDatasetInCollection is called
        CollectionDatasetVersion actualVersion = service.updateDatasetVersionInCollection(collectionID, datasetID,edition,version, collectionDatasetVersion);

        // Then the dataset API is called to populate the version data, and its added to the collection.
        verify(mockDatasetAPI, times(1)).getDataset(datasetID);
        verify(mockCollectionDescription, times(1)).addDatasetVersion(actualVersion);
        verify(mockCollection, times(1)).save();

        // The returned dataset has the expected values.
        Assert.assertEquals(dataset.getId(), actualVersion.getId());
        Assert.assertEquals(dataset.getTitle(), actualVersion.getTitle());
        Assert.assertEquals(edition, actualVersion.getEdition());
        Assert.assertEquals(version, actualVersion.getVersion());
        Assert.assertEquals(initialState, actualVersion.getState());
    }

    @Test
    public void TestDatasetService_deleteDatasetFromCollection() throws Exception {

        CollectionDataset collectionDataset = new CollectionDataset();

        // Given a mockCollection that does not contain the dataset we try to delete
        when(mockCollectionDescription.getDataset(datasetID)).thenReturn(Optional.of(collectionDataset));

        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI, mockZebedee);

        // When removeDatasetFromCollection is called
        service.removeDatasetFromCollection(collectionID, datasetID);

        // Then the collection is prompted to delete the dataset and save.
        verify(mockCollectionDescription, times(1)).removeDataset(collectionDataset);
        verify(mockCollection, times(1)).save();
    }

    @Test
    public void TestDatasetService_deleteDatasetVersionFromCollection() throws Exception {

        CollectionDatasetVersion datasetVersion = new CollectionDatasetVersion();

        // Given a mockCollection that does not contain the dataset we try to delete
        when(mockCollectionDescription.getDatasetVersion(datasetID,edition,version))
                .thenReturn(Optional.of(datasetVersion));

        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI, mockZebedee);

        // When removeDatasetFromCollection is called
        service.removeDatasetVersionFromCollection(collectionID, datasetID, edition, version);

        // Then the collection is prompted to delete the dataset and save.
        verify(mockCollectionDescription, times(1)).removeDatasetVersion(datasetVersion);
        verify(mockCollection, times(1)).save();
    }

    @Test
    public void TestDatasetService_deleteDatasetFromCollection_ReturnsIfNotInCollection() throws Exception {

        CollectionDataset collectionDataset = new CollectionDataset();

        // Given a mockCollection that does not contain the dataset we try to delete
        when(mockCollectionDescription.getDataset(datasetID)).thenReturn(Optional.empty());

        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI, mockZebedee);

        // When deleteDatasetFromCollection is called
        service.removeDatasetFromCollection(collectionID, datasetID);

        // Then the delete function is not called, as the dataset is not in the collection.
        verify(mockCollectionDescription, times(0)).removeDataset(collectionDataset);
        verify(mockCollection, times(0)).save();
    }

    @Test
    public void TestDatasetService_deleteDatasetVersionFromCollection_ReturnsIfNotInCollection() throws Exception {

        CollectionDatasetVersion collectionDatasetVersion = new CollectionDatasetVersion();

        // Given a mockCollection that does not contain the dataset we try to delete
        when(mockCollectionDescription.getDatasetVersion(datasetID, edition, version)).thenReturn(Optional.empty());

        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI, mockZebedee);

        // When deleteDatasetFromCollection is called
        service.removeDatasetVersionFromCollection(collectionID, datasetID, edition, version);

        // Then the delete function is not called, as the dataset is not in the collection.
        verify(mockCollectionDescription, times(0)).removeDatasetVersion(collectionDatasetVersion);
        verify(mockCollection, times(0)).save();
    }
}
