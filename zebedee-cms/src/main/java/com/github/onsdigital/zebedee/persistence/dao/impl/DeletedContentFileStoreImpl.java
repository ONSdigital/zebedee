package com.github.onsdigital.zebedee.persistence.dao.impl;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.content.deleted.DeletedContentEvent;
import com.github.onsdigital.zebedee.model.content.deleted.DeletedFile;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TimeZone;

/**
 * A local disk based implementation of the deleted content store.
 */
public class DeletedContentFileStoreImpl implements DeletedContentFileStore {

    public static final String DELETED_CONTENT_DIRECTORY = "deleted-content"; // the directory name used to store deleted content under.
    final Path deletedContentRootPath; // the path containing the deleted content directory.

    // The format used when writing deleted content.
    private static FastDateFormat format = FastDateFormat.getInstance("yyyy-MM-dd-HH-mm", TimeZone.getTimeZone("Europe/London"));

    /**
     * Create a new instance using the given root path to store deleted content.
     * @param rootPath
     */
    public DeletedContentFileStoreImpl(Path rootPath) {
        this.deletedContentRootPath = rootPath.resolve(DELETED_CONTENT_DIRECTORY);
    }

    /**
     * Store files defined in the given DeletedContentEvent, reading them from the given ContentReader.
     * @param deletedContentEvent
     * @param contentReader
     * @throws ZebedeeException
     * @throws IOException
     */
    @Override
    public void storeFiles(DeletedContentEvent deletedContentEvent, ContentReader contentReader) throws ZebedeeException, IOException {
        String formattedEventDate = DeletedContentFileStoreImpl.format.format(deletedContentEvent.getEventDate());
        ContentWriter contentWriter = new ContentWriter(deletedContentRootPath.resolve(formattedEventDate));

        for (DeletedFile file : deletedContentEvent.getDeletedFiles()) {
            this.copyFiles(file.getUri(), contentReader, contentWriter);
        }
    }

    /**
     * Retrieves the files deleted as part of the given DeletedContentEvent. Writes the retrieved files to the given
     * ContentWriter.
     * @param deletedContentEvent
     * @param contentWriter
     */
    @Override
    public void retrieveFiles(DeletedContentEvent deletedContentEvent, ContentWriter contentWriter) throws ZebedeeException, IOException {
        String formattedEventDate = DeletedContentFileStoreImpl.format.format(deletedContentEvent.getEventDate());
        ContentReader contentReader = new FileSystemContentReader(deletedContentRootPath.resolve(formattedEventDate));

        for (DeletedFile file : deletedContentEvent.getDeletedFiles()) {
            this.copyFiles(file.getUri(), contentReader, contentWriter);
        }
    }

    /**
     * Copy an individual file at the given URI, reading the file from the given ContentReader and storing it
     * using the given ContentWriter
     * @param uri
     * @param contentReader
     * @param contentWriter
     * @throws ZebedeeException
     * @throws IOException
     */
    private void copyFiles(String uri, ContentReader contentReader, ContentWriter contentWriter) throws ZebedeeException, IOException {

        try (DirectoryStream<Path> stream = contentReader.getDirectoryStream(uri)) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    String relativePath = "/" + contentReader.getRootFolder().relativize(path).toString();
                    contentWriter.write(contentReader.getResource(relativePath).getData(), relativePath);
                }
            }
        }
    }
}
