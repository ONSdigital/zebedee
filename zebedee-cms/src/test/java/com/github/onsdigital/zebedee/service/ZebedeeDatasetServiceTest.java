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

    Dataset dataset = new Dataset();

    DatasetClient mockDatasetAPI = mock(DatasetClient.class);
    ZebedeeCmsService mockZebedee = mock(ZebedeeCmsService.class);
    Collection mockCollection = mock(Collection.class);
    CollectionDescription mockCollectionDescription = mock(CollectionDescription.class);

    @Before
    public void setUp() throws Exception {

        dataset.setId(datasetID);
        dataset.setTitle("Dataset title");
        dataset.setUri("/the/dataset/uri");

        when(mockDatasetAPI.getDataset(datasetID)).thenReturn(dataset);
        when(mockZebedee.getCollection(collectionID)).thenReturn(mockCollection);
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);
    }

    @Test
    public void TestDatasetService_addDatasetToCollection_ReturnsIfAlreadyInCollection() throws Exception {

        // Given a mockCollection that already contains the dataset
        when(mockCollectionDescription.getDataset(datasetID)).thenReturn(Optional.of(new CollectionDataset()));

        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI, mockZebedee);

        // When addDatasetToCollection is called
        service.addDatasetToCollection(collectionID, datasetID);

        // The function does not call the dataset API as it sees that its already been added.
        verify(mockDatasetAPI, times(0)).getDataset(datasetID);
    }

    @Test
    public void TestDatasetService_addDatasetVersionToCollection_ReturnsIfAlreadyInCollection() throws Exception {

        // Given a mockCollection that already contains a dataset version
        when(mockCollectionDescription.getDatasetVersion(datasetID, edition, version))
                .thenReturn(Optional.of(new CollectionDatasetVersion()));

        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI, mockZebedee);

        // When addDatasetToCollection is called
        service.addDatasetVersionToCollection(collectionID, datasetID, edition, version);

        // The function does not call the dataset API as it sees that its already been added.
        verify(mockDatasetAPI, times(0)).getDataset(datasetID);
    }

    @Test
    public void TestDatasetService_addDatasetToCollection() throws Exception {

        // Given a mockCollection that contains no datasets
        when(mockCollectionDescription.getDataset(datasetID)).thenReturn(Optional.empty());
        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI, mockZebedee);

        // When addDatasetToCollection is called
        CollectionDataset actualDataset = service.addDatasetToCollection(collectionID, datasetID);

        // Then the dataset API is called to populate the dataset data, and its added to the collection.
        verify(mockDatasetAPI, times(1)).getDataset(datasetID);
        verify(mockCollectionDescription, times(1)).addDataset(actualDataset);
        verify(mockCollection, times(1)).save();

        // The returned instance has the expected values.
        Assert.assertEquals(dataset.getId(), actualDataset.getId());
        Assert.assertEquals(dataset.getTitle(), actualDataset.getTitle());
    }

    @Test
    public void TestDatasetService_addDatasetVersionToCollection() throws Exception {

        // Given a mockCollection that contains no dataset versions
        when(mockCollectionDescription.getDatasetVersion(datasetID,edition,version)).thenReturn(Optional.empty());
        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI, mockZebedee);

        // When addDatasetToCollection is called
        CollectionDatasetVersion actualVersion = service.addDatasetVersionToCollection(collectionID, datasetID,edition,version);

        // Then the dataset API is called to populate the version data, and its added to the collection.
        verify(mockDatasetAPI, times(1)).getDataset(datasetID);
        verify(mockCollectionDescription, times(1)).addDatasetVersion(actualVersion);
        verify(mockCollection, times(1)).save();

        // The returned instance has the expected values.
        Assert.assertEquals(dataset.getId(), actualVersion.getId());
        Assert.assertEquals(dataset.getTitle(), actualVersion.getTitle());
        Assert.assertEquals(edition, actualVersion.getEdition());
        Assert.assertEquals(version, actualVersion.getVersion());
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

        // Then the delete function is not called, as the instance is not in the collection.
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

        // Then the delete function is not called, as the instance is not in the collection.
        verify(mockCollectionDescription, times(0)).removeDatasetVersion(collectionDatasetVersion);
        verify(mockCollection, times(0)).save();
    }
}
