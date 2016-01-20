package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.configuration.Configuration.getUnauthorizedMessage;
import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.getConfiguration;

public class ZebedeeCollectionReader extends CollectionReader {

    public ZebedeeCollectionReader(Zebedee zebedee, Collection collection, Session session) throws BadRequestException, IOException, UnauthorizedException, NotFoundException {

        if (collection == null) {
            throw new NotFoundException("Please specify a collection");
        }

        // Authorisation
        if (session == null
                || !zebedee.permissions.canView(session, collection.description)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        Keyring keyring = zebedee.keyringCache.get(session);
        if (keyring == null) throw new UnauthorizedException("No keyring is available for " + session.email);

        SecretKey key = keyring.get(collection.description.id);
        init(collection, key);
    }

    public ZebedeeCollectionReader(Collection collection, SecretKey key) throws BadRequestException, IOException, UnauthorizedException, NotFoundException {
        init(collection, key);
    }

    private void init(Collection collection, SecretKey key) throws NotFoundException, UnauthorizedException, IOException {

        if (collection == null) {
            throw new NotFoundException("Collection not found");
        }

        inProgress = getContentReader(collection, key, collection.path, getConfiguration().getInProgressFolderName());
        complete = getContentReader(collection, key, collection.path, getConfiguration().getCompleteFolderName());
        reviewed = getContentReader(collection, key, collection.path, getConfiguration().getReviewedFolderName());
    }

    private ContentReader getContentReader(Collection collection, SecretKey key, Path collectionPath, String folderName) throws UnauthorizedException, IOException {
        return new CollectionContentReader(collection, key, collectionPath.resolve(folderName));
    }
}
