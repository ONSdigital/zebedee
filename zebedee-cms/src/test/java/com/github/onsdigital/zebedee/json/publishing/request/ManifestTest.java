package com.github.onsdigital.zebedee.json.publishing.request;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.PendingDelete;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Content;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class ManifestTest {

    @Mock
    private Collection collection;

    @Mock
    private Content reviewed;

    private Path collectionPath;
    private CollectionDescription collectionDescription;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        collectionPath = Files.createTempDirectory(Random.id());
        when(collection.getPath()).thenReturn(collectionPath);
        when(collection.getReviewed()).thenReturn(reviewed);

        collectionDescription = new CollectionDescription();
        when(collection.getDescription()).thenReturn(collectionDescription);
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(collectionPath.toFile());
    }

    @Test
    public void getManifestPath_givenValidCollection_shouldGetManifestPathForCollection() {

        // Given a collection

        // When the getManifestPath method is called
        Path path = Manifest.getManifestPath(collection);

        // Then the expected path is returned
        assertEquals(String.format("%s/%s", collectionPath, Manifest.FILENAME), path.toString());
    }

    @Test
    public void get_givenCollectionWithoutManifest_shouldCreateManifest() throws IOException {

        // Given a collection with some content but without a manifest file
        List<String> uris = new ArrayList<>();
        uris.add("/content/uri/a/data.json");
        uris.add("/content/uri/b/data.json");
        uris.add("/content/uri/b/previous/v1/data.json");
        when(reviewed.uris()).thenReturn(uris);

        PendingDelete delete = new PendingDelete("username", new ContentDetail("/page/to/delete", PageType.STATIC_PAGE));
        collectionDescription.getPendingDeletes().add(delete);

        // When get is called
        Manifest manifest = Manifest.get(collection);

        // Then a new manifest is created with correct deletes and moves
        assertEquals(collectionDescription.getPendingDeletes().size(), manifest.urisToDelete.size());
        assertTrue(manifest.urisToDelete.contains(collectionDescription.getPendingDeletes().get(0).getRoot().getUri()));

        assertEquals(1, manifest.filesToCopy.size());
        for (FileCopy fileToCopy : manifest.filesToCopy) {
            String expectedTarget = uris.get(2);
            assertEquals(expectedTarget, fileToCopy.target);

            // Check the source by reversing to target
            assertEquals(FilenameUtils.getName(expectedTarget), FilenameUtils.getName(fileToCopy.source));
            assertEquals(FilenameUtils.getPath(expectedTarget), FilenameUtils.getPath(fileToCopy.source) + "previous/v1/");
        }
    }

    @Test
    public void save_get_givenValidManifestAndCollection_shouldSaveAndGetManifest() throws IOException {

        // Given a new manifest
        Manifest manifest = new Manifest();
        manifest.addFileCopy("from/here", "to/there");
        manifest.addDelete("from/here");

        // When the save method is called
        boolean saved = Manifest.save(manifest, collection);

        // Then we can load the manifest and it has the same content as the saved
        // manifest.
        Manifest loadedManifest = Manifest.load(collection);

        assertTrue(saved);
        assertEquals(manifest.filesToCopy.size(), loadedManifest.filesToCopy.size());

        // check the file copy is persisted
        assertEquals(manifest.filesToCopy.iterator().next().source,
                loadedManifest.filesToCopy.iterator().next().source);
        assertEquals(manifest.filesToCopy.iterator().next().target,
                loadedManifest.filesToCopy.iterator().next().target);

        // check the delete is persisted
        assertEquals(manifest.urisToDelete.iterator().next(), loadedManifest.urisToDelete.iterator().next());
    }

    @Test
    public void shouldAddFileCopiesForNonAutomatedCollection() throws IOException {
        // Given a manual collection
        collectionDescription.setType(CollectionType.manual);

        // And a versioned URI in the reviewed content
        List<String> uris = new ArrayList<>();
        uris.add("/some/content/previous/v1/data.json");
        when(reviewed.uris()).thenReturn(uris);

        // When the manifest is created
        Manifest manifest = Manifest.get(collection);

        // Then file copies are added
        assertEquals(1, manifest.filesToCopy.size());
    }

    @Test
    public void shouldNotAddFileCopiesForAutomatedCollection() throws IOException {
        // Given an automated collection
        collectionDescription.setType(CollectionType.automated);

        // And a versioned URI in the reviewed content
        List<String> uris = new ArrayList<>();
        uris.add("/some/content/previous/v1/data.json");
        when(reviewed.uris()).thenReturn(uris);

        // When the manifest is created
        Manifest manifest = Manifest.get(collection);

        // Then no file copies are added
        assertEquals(0, manifest.filesToCopy.size());
    }
}
