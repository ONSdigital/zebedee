package com.github.onsdigital.zebedee.json;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CollectionDescriptionTest {

    @Test
    public void testAddDataset() throws Exception {

        // Given a collection description and a CollectionDataset
        CollectionDescription collection = new CollectionDescription();

        CollectionDataset dataset = new CollectionDataset();
        String datasetID = "123";
        dataset.setId(datasetID);

        // When a dataset is added
        collection.addDataset(dataset);

        // Then the dataset is in the collection description
        Optional<CollectionDataset> optional = collection.getDataset(datasetID);
        assertTrue(optional.isPresent());
        assertTrue(collection.getDatasets().contains(dataset));
    }

    @Test
    public void testRemoveDataset() throws Exception {

        // Given a collection description with a dataset
        CollectionDescription collection = new CollectionDescription();

        CollectionDataset dataset = new CollectionDataset();
        String datasetID = "123";
        dataset.setId(datasetID);

        collection.addDataset(dataset);

        // When a dataset is removed
        collection.removeDataset(dataset);

        // Then the dataset is in the collection description
        Optional<CollectionDataset> option = collection.getDataset(datasetID);
        assertFalse(option.isPresent());
        assertFalse(collection.getDatasets().contains(dataset));
    }

    @Test
    public void testAddDatasetVersion() throws Exception {

        // Given a collection description and a CollectionDatasetVersion
        CollectionDescription collection = new CollectionDescription();
        String datasetID = "123";
        String edition = "2016";
        String version = "1";

        CollectionDatasetVersion datasetVersion = new CollectionDatasetVersion();
        datasetVersion.setId(datasetID);
        datasetVersion.setEdition(edition);
        datasetVersion.setVersion(version);

        // When a dataset version is added
        collection.addDatasetVersion(datasetVersion);

        // Then the dataset version is in the collection description
        Optional<CollectionDatasetVersion> optional = collection.getDatasetVersion(datasetID, edition, version);
        assertTrue(optional.isPresent());
        assertTrue(collection.getDatasetVersions().contains(datasetVersion));
    }

    @Test
    public void testRemoveDatasetVersion() throws Exception {

        // Given a collection description with a dataset version
        CollectionDescription collection = new CollectionDescription();

        CollectionDatasetVersion datasetVersion = new CollectionDatasetVersion();
        String datasetID = "123";
        String edition = "2016";
        String version = "1";

        datasetVersion.setId(datasetID);
        datasetVersion.setEdition(edition);
        datasetVersion.setVersion(version);

        collection.addDatasetVersion(datasetVersion);

        // When a dataset version is removed
        collection.removeDatasetVersion(datasetVersion);

        // Then the dataset is in the collection description
        Optional<CollectionDatasetVersion> option = collection.getDatasetVersion(datasetID, edition, version);
        assertFalse(option.isPresent());
        assertFalse(collection.getDatasetVersions().contains(datasetVersion));
    }
}
