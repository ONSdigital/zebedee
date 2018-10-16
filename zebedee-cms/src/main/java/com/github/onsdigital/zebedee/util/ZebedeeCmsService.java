package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Collections;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.UnexpectedErrorException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * Class adds a wrapper around common Zebedee operations with which are implemented as static methods.
 * Using this wrapper to access the static methods instead of accessing them directly means these calls can be easily
 * replaced with mock implementations making unit tests easier and cleaner to write.
 * Also adds additional error logging and rethrows all exceptions as a {@link ZebedeeException} removing unnecessary
 * try/catch/throws code from calling classes.
 */
public class ZebedeeCmsService {

    private static final String COLLECTION_WRI_ERROR_MSG = "Could not obtain collection writer for requested collection";
    private static final String COLLECTION_READ_ERROR_MSG = "Could not obtain collection reader for requested collection";
    private static final String COLLECTION_NOT_FOUND_MSG = "Could not find requested collection.";
    private static final String SESSION_NOT_FOUND_MSG = "Could not get session from request";

    private static final ZebedeeCmsService instance = new ZebedeeCmsService();

    private ZebedeeCmsService() {
        // use getInstance() method.
    }

    public static ZebedeeCmsService getInstance() {
        return instance;
    }

    public Session getSession(HttpServletRequest request) throws ZebedeeException {
        try {
            return Root.zebedee.getSessionsService().get(request);
        } catch (IOException e) {
            logError(e, SESSION_NOT_FOUND_MSG).logAndThrow(UnauthorizedException.class);
        }
        return null;
    }

    public ContentReader getPublishedContentReader() {
        return new FileSystemContentReader(Root.zebedee.getPublished().path);
    }

    public CollectionWriter getZebedeeCollectionWriter(Collection collection, Session session) throws ZebedeeException {
        try {
            return new ZebedeeCollectionWriter(Root.zebedee, collection, session);
        } catch (IOException e) {
            logError(e, COLLECTION_WRI_ERROR_MSG).collectionId(collection).user(session.getEmail())
                    .logAndThrow(BadRequestException.class);
        }
        return null;
    }

    public CollectionReader getZebedeeCollectionReader(Collection collection, Session session) throws ZebedeeException {
        try {
            return new ZebedeeCollectionReader(Root.zebedee, collection, session);
        } catch (IOException e) {
            logError(e, COLLECTION_READ_ERROR_MSG).collectionId(collection).user(session.getEmail())
                    .logAndThrow(BadRequestException.class);
        }
        return null;
    }

    public Collection getCollection(HttpServletRequest request) throws ZebedeeException {
        try {
            return Collections.getCollection(request);
        } catch (IOException e) {
            logError(e, COLLECTION_NOT_FOUND_MSG).logAndThrow(NotFoundException.class);
        }
        return null;
    }

    public Collection getCollection(String collectionId) throws ZebedeeException {
        try {
            return Root.zebedee.getCollections().getCollection(collectionId);
        } catch (IOException e) {
            logError(e, COLLECTION_NOT_FOUND_MSG).logAndThrow(NotFoundException.class);
        }
        return null;
    }

    public PermissionsService getPermissions() {
        return Root.zebedee.getPermissionsService();
    }

    public InputStream objectAsInputStream(Object obj) {
        return new ByteArrayInputStream(ContentUtil.serialise(obj).getBytes());
    }

    public Zebedee getZebedee() {
        return Root.zebedee;
    }
}
