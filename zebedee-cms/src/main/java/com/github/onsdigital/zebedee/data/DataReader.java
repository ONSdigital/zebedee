package com.github.onsdigital.zebedee.data;

import com.github.onsdigital.content.DirectoryListing;
import com.github.onsdigital.content.service.ContentNotFoundException;
import com.github.onsdigital.content.service.ContentService;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by bren on 13/06/15.
 *
 * Used when resolving content references in fetched content
 */
public class DataReader implements ContentService {

    private Collection collection;
    private Session session;

    public DataReader(Session session, Collection collection) {
        this.collection = collection;
        this.session = session;
    }

    @Override
    public InputStream readData(String uri) throws ContentNotFoundException {
        try {
            return getDataStream(uri);
        }  catch (IOException e) {
            throw new RuntimeException("Failed reading data at " + uri);
        }
    }

    @Override
    public DirectoryListing readDirectory(String uri) throws ContentNotFoundException {
        try {
            return Root.zebedee.collections.listDirectory(collection, uri, session);
        } catch (IOException e) {
            throw new RuntimeException("Failed reading data at " + uri);
        } catch (UnauthorizedException | NotFoundException | BadRequestException e) {
            throw new ContentNotFoundException(e);
        }
    }

    private InputStream getDataStream(String uri)
            throws IOException, ContentNotFoundException {
        uri =  StringUtils.removeStart(uri, "/") + "/data.json";
        System.out.println("Reading data under uri:" + uri);
        Path dataPath = collection.find(session.email, uri);
        // Look for a data.json file, or
        // fall back to adding a .json file extension
        if (dataPath == null || !Files.exists(dataPath)) {
            throw new ContentNotFoundException("No data found under  " + uri);
        } else {
            return Files.newInputStream(dataPath);
        }

    }

}
