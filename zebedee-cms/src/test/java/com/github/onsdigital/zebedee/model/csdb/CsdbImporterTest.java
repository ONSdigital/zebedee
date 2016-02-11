package com.github.onsdigital.zebedee.model.csdb;

import com.github.davidcarboni.ResourceUtils;
import com.github.davidcarboni.cryptolite.Keys;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.FakeCollectionReader;
import com.github.onsdigital.zebedee.model.FakeCollectionWriter;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CsdbImporterTest {

    Zebedee zebedee;
    Builder bob;
    Session publisher;
    Collection collection;
    CollectionReader collectionReader;
    CollectionWriter collectionWriter;

    String datasetPath = "/themea/landinga/producta/datasets/a4fk_dataset/current/";
    Dataset unpublishedDataset = null;

    /**
     * The bootstrap resource contains master data and one collection "collection"
     * <p>
     * "collection" contains two reviewed datasets
     * <p>
     * i) /themea/landinga/producta/datasets/a4fk_dataset - this has existing data (the a4fk timeseries)
     * ii) /themea/landinga/producta/datasets/another_dataset - this has no existing data in master
     * <p>
     * i) is a cut down version of Blue Book. ii) is a cut down version of PPI
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        // Loads a zebedee with two collections, each of which contain a dataset
        //
        bob = new Builder(this.getClass(), ResourceUtils.getPath("/bootstraps/data_publisher"));
        zebedee = new Zebedee(bob.zebedee, false);
        publisher = bob.createSession(bob.publisher1);

        collection = zebedee.collections.list().getCollection("collection");

        collectionReader = new FakeCollectionReader(zebedee.collections.path.toString(), collection.description.id);
        collectionWriter = new FakeCollectionWriter(zebedee.collections.path.toString(), collection.description.id);

        try (InputStream inputStream = Files.newInputStream(collection.reviewed.get(datasetPath).resolve("data.json"))) {
            unpublishedDataset = ContentUtil.deserialise(inputStream, Dataset.class);
        }

        assertNotNull(unpublishedDataset);
    }

    @Test
    public void shouldGetCsdbPathFromCollectionWithMatchingDataset() throws IOException {

        Path csdbPathFromCollection = CsdbImporter.getCsdbPathFromCollection("A4FK", collectionReader);

        assertNotNull(csdbPathFromCollection);
        Assert.assertEquals(Paths.get(datasetPath, "A4FK.csdb"), csdbPathFromCollection);
    }

    @Test
    public void shouldGetCsdbDataFromDylan() throws IOException {

        // Given a dummy Dylan client and a new key pair
        KeyPair keyPair = Keys.newKeyPair();
        DummyDylanClient dylanClient = new DummyDylanClient(keyPair.getPublic());

        // When we call the getDylanData method.
        InputStream inputStream = CsdbImporter.getDylanData(keyPair.getPrivate(), "csdbId", dylanClient);

        String result = IOUtils.toString(inputStream);
        inputStream.close();

        assertEquals(dylanClient.testData, result);
    }
}
