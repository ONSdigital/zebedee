package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.getConfiguration;

public class ZebedeeCollectionReader extends CollectionReader {

    public ZebedeeCollectionReader(Zebedee zebedee, Collection collection, Session session) throws BadRequestException, IOException, UnauthorizedException {
        inProgress = getContentReader(zebedee, collection, session, collection.path, getConfiguration().getInProgressFolderName());
        complete = getContentReader(zebedee, collection, session, collection.path, getConfiguration().getCompleteFolderName());
        reviewed = getContentReader(zebedee, collection, session, collection.path, getConfiguration().getReviewedFolderName());
    }

    private ContentReader getContentReader(Zebedee zebedee, Collection collection, Session session, Path collectionPath, String folderName) {
        return new CollectionContentReader(zebedee, collection, session, collectionPath.resolve(folderName));
    }
}
