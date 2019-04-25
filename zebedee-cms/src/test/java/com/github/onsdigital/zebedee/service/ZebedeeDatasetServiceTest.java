package com.github.onsdigital.zebedee.service;


import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.json.CollectionDataset;
import com.github.onsdigital.zebedee.json.CollectionDatasetVersion;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.ContentStatus;
import com.github.onsdigital.zebedee.model.Collection;
import dp.api.dataset.DatasetClient;
import dp.api.dataset.model.Dataset;
import dp.api.dataset.model.DatasetLinks;
import dp.api.dataset.model.DatasetVersion;
import dp.api.dataset.model.Link;
import dp.api.dataset.model.State;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZebedeeDatasetServiceTest {

    String datasetID = "321";
    String edition = "2015";
    String version = "1";
    String collectionID = "345";
    String datasetUrl = "/datasets/" + datasetID;
    String datasetVersionUrl = "/datasets/" + datasetID + "/" + edition + "/" + version;
    private String user = "test@email.com";

    ContentStatus initialState = ContentStatus.Complete;

    Dataset dataset = createDataset();
    DatasetVersion datasetVersion = createDatasetVersion();

    DatasetClient mockDatasetAPI = mock(DatasetClient.class);
    Collection mockCollection = mock(Collection.class);
    CollectionDescription mockCollectionDescription = mock(CollectionDescription.class);

    CollectionDataset collectionDataset = new CollectionDataset();
    CollectionDatasetVersion collectionDatasetVersion = new CollectionDatasetVersion();

    @Before
    public void setUp() throws Exception {

        when(mockDatasetAPI.getDataset(datasetID)).thenReturn(dataset);
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);
        when(mockCollection.getId()).thenReturn(collectionID);
        when(mockCollectionDescription.getDataset(datasetID)).thenReturn(Optional.empty());
        when(mockCollectionDescription.getDatasetVersion(datasetID, edition, version)).thenReturn(Optional.empty());

        when(mockDatasetAPI.getDatasetVersion(datasetID, edition, version))
                .thenReturn(datasetVersion);

        collectionDataset.setId(datasetID);
        collectionDataset.setState(initialState);
        collectionDataset.setLastEditedBy(user);

        collectionDatasetVersion.setId(datasetID);
        collectionDatasetVersion.setEdition(edition);
        collectionDatasetVersion.setVersion(version);
        collectionDatasetVersion.setState(initialState);
        collectionDatasetVersion.setLastEditedBy(user);
    }

    @Test
    public void TestDatasetService_updateDatasetInCollection_UpdatesStateWhenAlreadyInCollection() throws Exception {

        // Given a mockCollection that already contains the dataset
        when(mockCollectionDescription.getDataset(datasetID)).thenReturn(Optional.of(new CollectionDataset()));
        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI);

        // When updateDatasetInCollection is called with an updated state
        ContentStatus state = ContentStatus.Complete;
        collectionDataset.setState(state);
        CollectionDataset updated = service.updateDatasetInCollection(mockCollection, datasetID, collectionDataset, user);

        // Then the updated dataset contains the updated state
        ArgumentCaptor<CollectionDataset> datasetArgumentCaptor = ArgumentCaptor.forClass(CollectionDataset.class);
        verify(mockCollectionDescription, times(1)).addDataset(datasetArgumentCaptor.capture());
        Assert.assertEquals(datasetArgumentCaptor.getAllValues().get(0).getState(), state);

        Assert.assertEquals(updated.getState(), state);
    }

    @Test
    public void TestDatasetService_updateDatasetInCollection_SendsCollectionIDToDatasetAPI() throws Exception {

        // Given a dataset in the dataset API that is not associated with a collection
        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI);

        // When updateDatasetInCollection is called
        service.updateDatasetInCollection(mockCollection, datasetID, collectionDataset, user);

        // Then the dataset API is called to set the collection ID
        ArgumentCaptor<Dataset> datasetArgumentCaptor = ArgumentCaptor.forClass(Dataset.class);
        verify(mockDatasetAPI, times(1)).updateDataset(anyString(), datasetArgumentCaptor.capture());
        Assert.assertEquals(datasetArgumentCaptor.getAllValues().get(0).getCollection_id(), collectionID);
        Assert.assertEquals(datasetArgumentCaptor.getAllValues().get(0).getState(), State.ASSOCIATED);
    }

    @Test
    public void TestDatasetService_updateDatasetInCollection_DoesNotSendCollectionIDToDatasetAPIIfAlreadySet() throws Exception {

        // Given a dataset in the dataset API that is already associated with the collection
        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI);

        Dataset dataset = createDataset();
        dataset.setCollection_id(collectionID);
        dataset.setState(State.ASSOCIATED);
        when(mockDatasetAPI.getDataset(datasetID)).thenReturn(dataset);

        // When updateDatasetInCollection is called
        service.updateDatasetInCollection(mockCollection, datasetID, collectionDataset, user);

        // Then the dataset API is not called to set the collection ID
        verify(mockDatasetAPI, times(0)).updateDataset(anyString(), anyObject());
    }

    @Test(expected = ConflictException.class)
    public void TestDatasetService_updateDatasetInCollection_ThrowsConflictExceptionIfAlreadyInAnotherCollection() throws Exception {

        // Given a dataset in the dataset API that is already associated with a different collection
        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI);

        Dataset dataset = createDataset();
        dataset.setState(State.ASSOCIATED);
        dataset.setCollection_id("someOtherCollectionID");
        when(mockDatasetAPI.getDataset(datasetID)).thenReturn(dataset);

        // When updateDatasetInCollection is called
        service.updateDatasetInCollection(mockCollection, datasetID, collectionDataset, user);

        // Then a conflict exception is thrown
    }

    @Test
    public void TestDatasetService_updateDatasetVersionInCollection_SendsCollectionIDToDatasetAPI() throws Exception {

        // Given a dataset version in the dataset API that is not associated with a collection
        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI);

        // When updateDatasetInCollection is called
        service.updateDatasetVersionInCollection(mockCollection, datasetID, edition, version, collectionDatasetVersion, user);

        // Then the dataset API is called to set the collection ID
        ArgumentCaptor<DatasetVersion> argumentCaptor = ArgumentCaptor.forClass(DatasetVersion.class);
        verify(mockDatasetAPI, times(1)).updateDatasetVersion(anyString(), anyString(), anyString(), argumentCaptor.capture());
        Assert.assertEquals(collectionID, argumentCaptor.getValue().getCollection_id());
        Assert.assertEquals(State.ASSOCIATED, argumentCaptor.getValue().getState());
    }

    @Test
    public void TestDatasetService_updateDatasetVersionInCollection_DoesNotSendCollectionIDToDatasetAPIIfAlreadySet() throws Exception {

        // Given a dataset version in the dataset API that is already associated with the collection
        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI);

        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setCollection_id(collectionID);
        datasetVersion.setState(State.ASSOCIATED);
        when(mockDatasetAPI.getDatasetVersion(datasetID, edition, version)).thenReturn(datasetVersion);

        // When updateDatasetInCollection is called
        service.updateDatasetInCollection(mockCollection, datasetID, collectionDataset, user);

        // Then the dataset API is not called to set the collection ID
        verify(mockDatasetAPI, times(0)).updateDatasetVersion(anyString(), anyString(), anyString(), anyObject());
    }

    @Test(expected = ConflictException.class)
    public void TestDatasetService_updateDatasetVersionInCollection_ThrowsConflictExceptionIfAlreadyInAnotherCollection() throws Exception {

        // Given a dataset in the dataset API that is already associated with a collection
        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI);

        when(mockCollectionDescription.getDatasetVersion(datasetID, edition, version))
                .thenReturn(Optional.of(new CollectionDatasetVersion()));

        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setState(State.ASSOCIATED);
        datasetVersion.setCollection_id("someOtherCollectionID");
        when(mockDatasetAPI.getDatasetVersion(datasetID, edition, version)).thenReturn(datasetVersion);

        // When updateDatasetInCollection is called
        service.updateDatasetVersionInCollection(mockCollection, datasetID, edition, version, collectionDatasetVersion, user);

        // Then a conflict exception is thrown
    }

    @Test
    public void TestDatasetService_updateDatasetVersionInCollection_UpdatesStateWhenAlreadyInCollection() throws Exception {

        // Given a mockCollection that already contains a dataset version
        when(mockCollectionDescription.getDatasetVersion(datasetID, edition, version))
                .thenReturn(Optional.of(new CollectionDatasetVersion()));

        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI);

        // When updateDatasetInCollection is called
        ContentStatus state = ContentStatus.Complete;
        collectionDatasetVersion.setState(state);
        CollectionDatasetVersion updated = service.updateDatasetVersionInCollection(mockCollection, datasetID, edition, version, collectionDatasetVersion, user);

        // Then the updated dataset contains the updated state
        ArgumentCaptor<CollectionDatasetVersion> versionArgumentCaptor = ArgumentCaptor.forClass(CollectionDatasetVersion.class);
        verify(mockCollectionDescription, times(1)).addDatasetVersion(versionArgumentCaptor.capture());
        Assert.assertEquals(versionArgumentCaptor.getAllValues().get(0).getState(), state);

        Assert.assertEquals(updated.getState(), state);
    }

    @Test
    public void TestDatasetService_updateDatasetInCollection() throws Exception {

        // Given a mockCollection that contains no datasets
        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI);

        // When updateDatasetInCollection is called
        CollectionDataset actualDataset = service.updateDatasetInCollection(mockCollection, datasetID, collectionDataset, user);

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
        when(mockCollectionDescription.getDatasetVersion(datasetID, edition, version)).thenReturn(Optional.empty());
        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI);

        // When updateDatasetInCollection is called
        CollectionDatasetVersion actualVersion = service.updateDatasetVersionInCollection(mockCollection, datasetID, edition, version, collectionDatasetVersion, user);

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

        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI);

        // When removeDatasetFromCollection is called
        service.removeDatasetFromCollection(mockCollection, datasetID);

        ArgumentCaptor<Dataset> argumentCaptor = ArgumentCaptor.forClass(Dataset.class);
        verify(mockDatasetAPI, times(1)).deleteDataset(anyString());

        // Then the collection is prompted to delete the dataset and save.
        verify(mockCollectionDescription, times(1)).removeDataset(collectionDataset);
        verify(mockCollection, times(1)).save();
    }

    @Test
    public void TestDatasetService_deleteDatasetVersionFromCollection() throws Exception {

        CollectionDatasetVersion datasetVersion = new CollectionDatasetVersion();

        // Given a mockCollection that does not contain the dataset we try to delete
        when(mockCollectionDescription.getDatasetVersion(datasetID, edition, version))
                .thenReturn(Optional.of(datasetVersion));

        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI);

        // When removeDatasetFromCollection is called
        service.removeDatasetVersionFromCollection(mockCollection, datasetID, edition, version);

        // Then the collection is cleared in the version on the dataset API
        verify(mockDatasetAPI, times(1)).detachVersion(anyString(), anyString(), anyString());

        // Then the collection is prompted to remove the reference from that dataset-version from itself and save.
        verify(mockCollectionDescription, times(1)).removeDatasetVersion(datasetVersion);
        verify(mockCollection, times(1)).save();
    }

    @Test
    public void TestDatasetService_deleteDatasetFromCollection_ReturnsIfNotInCollection() throws Exception {

        CollectionDataset collectionDataset = new CollectionDataset();

        // Given a mockCollection that does not contain the dataset we try to delete

        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI);

        // When deleteDatasetFromCollection is called
        service.removeDatasetFromCollection(mockCollection, datasetID);

        // Then the delete function is not called, as the dataset is not in the collection.
        verify(mockCollectionDescription, times(0)).removeDataset(collectionDataset);
        verify(mockCollection, times(0)).save();
    }

    @Test
    public void TestDatasetService_deleteDatasetVersionFromCollection_ReturnsIfNotInCollection() throws Exception {

        CollectionDatasetVersion collectionDatasetVersion = new CollectionDatasetVersion();

        // Given a mockCollection that does not contain the dataset we try to delete
        when(mockCollectionDescription.getDatasetVersion(datasetID, edition, version)).thenReturn(Optional.empty());

        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI);

        // When removeDatasetVersionFromCollection is called
        service.removeDatasetVersionFromCollection(mockCollection, datasetID, edition, version);


        // Then the delete function is not called, as the dataset is not in the collection.
        verify(mockCollectionDescription, times(0)).removeDatasetVersion(collectionDatasetVersion);
        verify(mockCollection, times(0)).save();
    }

    @Test
    public void TestDatasetService_publishDatasetsInCollection_publishesDatasets() throws Exception {

        // Given a mockCollection that contains datasets
        Set<CollectionDataset> collectionDatasets = new HashSet<>();
        collectionDatasets.add(collectionDataset);

        when(mockCollectionDescription.getDatasets()).thenReturn(collectionDatasets);
        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI);

        // When publishDatasetsInCollection is called
        service.publishDatasetsInCollection(mockCollection);

        // Then the dataset API is called to update the dataset state to published
        ArgumentCaptor<String> datasetIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Dataset> datasetCaptor = ArgumentCaptor.forClass(Dataset.class);

        verify(mockDatasetAPI, times(1)).updateDataset(datasetIdCaptor.capture(), datasetCaptor.capture());

        Assert.assertEquals(datasetID, datasetIdCaptor.getValue());
        Assert.assertEquals(State.PUBLISHED, datasetCaptor.getValue().getState());
    }

    @Test
    public void TestDatasetService_publishDatasetsInCollection_publishesVersions() throws Exception {

        // Given a mockCollection that contains datasets
        Set<CollectionDatasetVersion> collectionVersions = new HashSet<>();
        collectionVersions.add(collectionDatasetVersion);

        when(mockCollectionDescription.getDatasetVersions()).thenReturn(collectionVersions);
        DatasetService service = new ZebedeeDatasetService(mockDatasetAPI);

        // When publishDatasetsInCollection is called
        service.publishDatasetsInCollection(mockCollection);

        // Then the dataset API is called to update the dataset state to published
        ArgumentCaptor<String> datasetIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> datasetEditionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> datasetVersionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<DatasetVersion> versionCaptor = ArgumentCaptor.forClass(DatasetVersion.class);

        verify(mockDatasetAPI, times(1)).updateDatasetVersion(datasetIdCaptor.capture(), datasetEditionCaptor.capture(), datasetVersionCaptor.capture(), versionCaptor.capture());

        Assert.assertEquals(datasetID, datasetIdCaptor.getValue());
        Assert.assertEquals(edition, datasetEditionCaptor.getValue());
        Assert.assertEquals(version, datasetVersionCaptor.getValue());
        Assert.assertEquals(State.PUBLISHED, versionCaptor.getValue().getState());
    }

    private Dataset createDataset() {

        Dataset dataset = new Dataset();
        dataset.setId(datasetID);
        dataset.setTitle("Dataset title");
        dataset.setState(State.CREATED);

        DatasetLinks links = new DatasetLinks();
        Link self = new Link();
        self.setHref(datasetUrl);
        links.setSelf(self);
        dataset.setLinks(links);
        return dataset;
    }


    private DatasetVersion createDatasetVersion() {

        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setCollection_id(collectionID);
        datasetVersion.setState(State.EDITION_CONFIRMED);
        DatasetLinks versionLinks = new DatasetLinks();
        Link self = new Link();
        self.setHref(datasetVersionUrl);
        versionLinks.setSelf(self);

        datasetVersion.setLinks(versionLinks);

        return datasetVersion;
    }

}
