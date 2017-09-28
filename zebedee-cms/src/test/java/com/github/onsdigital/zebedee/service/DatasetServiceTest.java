package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.dataset.api.Dataset;
import com.github.onsdigital.zebedee.dataset.api.DatasetClient;
import com.github.onsdigital.zebedee.dataset.api.Instance;
import com.github.onsdigital.zebedee.dataset.api.Link;
import com.github.onsdigital.zebedee.json.CollectionInstance;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

public class DatasetServiceTest {

    String instanceID = "123";
    String datasetID = "321";
    String collectionID = "345";

    Instance instance = new Instance();
    Dataset dataset = new Dataset();

    DatasetClient mockDatasetAPI = mock(DatasetClient.class);
    ZebedeeCmsService mockZebedee = mock(ZebedeeCmsService.class);
    Collection mockCollection = mock(Collection.class);

    @Before
    public void setUp() throws Exception {

        instance.setId(instanceID);
        instance.setLinks(new Instance.Links());
        instance.getLinks().dataset = new Link();
        instance.getLinks().dataset.setId(datasetID);

        dataset.setId(datasetID);
        dataset.setTitle("Dataset title");
        dataset.setUri("/the/dataset/uri");

        when(mockDatasetAPI.getInstance(instanceID)).thenReturn(instance);
        when(mockDatasetAPI.getDataset(datasetID)).thenReturn(dataset);
        when(mockZebedee.getCollection(collectionID)).thenReturn(mockCollection);
    }

    @Test
    public void TestDatasetService_addInstanceToCollection_ReturnsIfAlreadyInCollection() throws Exception {

        // Given a mockCollection that already contains the instance
        when(mockCollection.getInstance(instanceID)).thenReturn(Optional.of(new CollectionInstance()));

        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI, mockZebedee);

        // When addInstanceToCollection is called
        service.addInstanceToCollection(collectionID, instanceID);

        // The function does not call the dataset API as it sees that its already been added.
        verify(mockDatasetAPI, times(0)).getInstance(instanceID);
        verify(mockDatasetAPI, times(0)).getDataset(datasetID);
    }

    @Test
    public void TestDatasetService_addInstanceToCollection() throws Exception {

        // Given a mockCollection that contains no instances
        when(mockCollection.getInstance(instanceID)).thenReturn(Optional.empty());
        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI, mockZebedee);

        // When addInstanceToCollection is called
        CollectionInstance actualInstance = service.addInstanceToCollection(collectionID, instanceID);

        // Then the dataset API is called to populate the instance data, and its added to the mockCollection.
        verify(mockDatasetAPI, times(1)).getInstance(instanceID);
        verify(mockDatasetAPI, times(1)).getDataset(datasetID);
        verify(mockCollection, times(1)).addInstance(actualInstance);
        verify(mockCollection, times(1)).save();

        // The returned instance has the expected values.
        Assert.assertEquals(instance.getId(), actualInstance.getId());
        Assert.assertEquals(instance.getEdition(), actualInstance.getEdition());
        Assert.assertEquals(instance.getVersion(), actualInstance.getVersion());
        Assert.assertEquals(dataset.getId(), actualInstance.getDataset().id);
        Assert.assertEquals(dataset.getTitle(), actualInstance.getDataset().title);
        Assert.assertEquals(dataset.getUri(), actualInstance.getDataset().href);
    }

    @Test
    public void TestDatasetService_deleteInstanceFromCollection() throws Exception {

        CollectionInstance collectionInstance = new CollectionInstance();

        // Given a mockCollection that does not contain the instance we try to delete
        when(mockCollection.getInstance(instanceID)).thenReturn(Optional.of(collectionInstance));

        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI, mockZebedee);

        // When deleteInstanceFromCollection is called
        service.deleteInstanceFromCollection(collectionID, instanceID);

        // Then the collection is prompted to delete the instance and save.
        verify(mockCollection, times(1)).deleteInstance(collectionInstance);
        verify(mockCollection, times(1)).save();
    }

    @Test
    public void TestDatasetService_deleteInstanceFromCollection_ReturnsIfNotInCollection() throws Exception {

        CollectionInstance collectionInstance = new CollectionInstance();

        // Given a mockCollection that does not contain the instance we try to delete
        when(mockCollection.getInstance(instanceID)).thenReturn(Optional.empty());

        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI, mockZebedee);

        // When deleteInstanceFromCollection is called
        service.deleteInstanceFromCollection(collectionID, instanceID);

        // Then the delete function is not called, as the instance is not in the collection.
        verify(mockCollection, times(0)).deleteInstance(collectionInstance);
        verify(mockCollection, times(0)).save();
    }
}
