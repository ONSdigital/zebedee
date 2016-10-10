package com.github.onsdigital.zebedee.persistence.dao.impl;

import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.content.deleted.DeletedContentEvent;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import com.google.common.io.Files;
import org.apache.commons.lang3.time.FastDateFormat;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by carlhembrough on 05/10/2016.
 */
public class DeleteContentFileStoreImplTest {

    private static FastDateFormat format = FastDateFormat.getInstance("yyyy-MM-dd-HH-mm", TimeZone.getTimeZone("Europe/London"));

    @Test
    public void testSaveFilesAddsFilesToTheExpectedDirectory() throws Exception {

        // Given a source file that we want to save as deleted content.
        Path filePath = Paths.get("some/uri");
        ContentReader sourceContentReader = getSourceContentReader(filePath.resolve("data.json"));
        DeletedContentFileStoreImpl deletedContentFileStore = getDeletedContentFileStore();
        DeletedContentEvent deleteContentEvent = getDeletedContentEvent(filePath);

        // When files are stored in an deleted content file store
        deletedContentFileStore.storeFiles(deleteContentEvent, sourceContentReader);

        // Then the file exists
        Path destinationFile = deletedContentFileStore.deletedContentRootPath
                .resolve(format.format(deleteContentEvent.getEventDate()))
                .resolve(filePath);
        Assert.assertTrue(java.nio.file.Files.exists(destinationFile));
    }

    @Test
    public void testRetrieveFiles() throws Exception {

        // Given some content that has already been stored as deleted content.
        Path filePath = Paths.get("some/uri");
        ContentReader sourceContentReader = getSourceContentReader(filePath.resolve("data.json"));
        DeletedContentFileStore deletedContentFileStore = getDeletedContentFileStore();
        DeletedContentEvent deleteContentEvent = getDeletedContentEvent(filePath);
        deletedContentFileStore.storeFiles(deleteContentEvent, sourceContentReader);

        // When we call retrieve on the deleted content store.
        File destinationDirectory = Files.createTempDir();
        ContentWriter contentWriter = new ContentWriter(destinationDirectory.toPath());
        deletedContentFileStore.retrieveFiles(deleteContentEvent, contentWriter);

        // Then the files are written to the given content writer.
        Path destinationFile = destinationDirectory.toPath()
                .resolve(filePath);
        Assert.assertTrue(java.nio.file.Files.exists(destinationFile));
    }

    private DeletedContentFileStoreImpl getDeletedContentFileStore() {
        // create an instance of the deleted content store for the given temp path.
        File destinationDirectory = Files.createTempDir();
        return new DeletedContentFileStoreImpl(destinationDirectory.toPath());
    }

    private DeletedContentEvent getDeletedContentEvent(Path filePath) {
        // create a dummy deleted content event with the example file in it.
        DeletedContentEvent deleteContentEvent = new DeletedContentEvent();
        Date eventDate = new Date();
        deleteContentEvent.setEventDate(eventDate);
        deleteContentEvent.addDeletedFile(filePath.toString());
        return deleteContentEvent;
    }

    private ContentReader getSourceContentReader(Path filePath) throws IOException {
        // create source file and ContentReader.
        File sourceDirectory = Files.createTempDir();
        File sourceFile = sourceDirectory.toPath().resolve(filePath).toFile();
        Files.createParentDirs(sourceFile);
        Files.touch(sourceFile);
        return new FileSystemContentReader(sourceDirectory.toPath());
    }
}
