package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.content.collection.Collection;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.CollectionNotFoundException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.get;

public class FakeCollectionWriter extends CollectionWriter {

    private Path collections;

    public FakeCollectionWriter(String collectionsFolderPath, String collectionId) throws BadRequestException, IOException, UnauthorizedException, NotFoundException {

        if (collectionsFolderPath == null) {
            throw new NullPointerException("Collections folder can not be null");
        }
        this.collections = Paths.get(collectionsFolderPath);
        Path collectionsPath = findCollectionPath(collectionId);
        inProgress = getContentWriter(collectionsPath, get().getInProgressFolderName());
        complete = getContentWriter(collectionsPath, get().getCompleteFolderName());
        reviewed = getContentWriter(collectionsPath, get().getReviewedFolderName());
    }

    private ContentWriter getContentWriter(Path collectionPath, String folderName) {
        return new ContentWriter(collectionPath.resolve(folderName));
    }

    private Path findCollectionPath(String collectionId) throws IOException, NotFoundException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(collections, "*.{json}")) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    continue;
                } else {
                    try (InputStream fileStream = Files.newInputStream(path)) {
                        Collection collection = ContentUtil.deserialise(fileStream, Collection.class);
                        if (StringUtils.equalsIgnoreCase(collection.getId(), collectionId)) {
                            return collections.resolve(FilenameUtils.removeExtension(path.getFileName().toString())); //get directory with same name

                        }
                    }
                }
            }
            throw new CollectionNotFoundException("Collection with given id not found, id:" + collectionId);
        }
    }
}
