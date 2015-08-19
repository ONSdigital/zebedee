package com.github.onsdigital.zebedee.data;

import com.github.onsdigital.content.DirectoryListing;
import com.github.onsdigital.content.service.ContentNotFoundException;
import com.github.onsdigital.content.service.ContentService;
import com.github.onsdigital.content.util.ContentUtil;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.model.Content;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by tom on 13/06/15.
 *
 * Used when resolving content references in simple instances where we want to update
 */
public class SimpleReader implements ContentService {
    Content content = null;

    public SimpleReader(Content content) {
        this.content = content;
    }
    public static SimpleReader launchpadReader(Zebedee zebedee) {
        return new SimpleReader(zebedee.launchpad);
    }
    public static SimpleReader masterReader(Zebedee zebedee) {
        return new SimpleReader(zebedee.published);
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
    public DirectoryListing readDirectory(String uri) {
        try {
            return ContentUtil.listDirectory(this.content.path);
        } catch (IOException e) {
            throw new RuntimeException("Failed reading data at " + uri);
        }
    }

    private InputStream getDataStream(String uri)
            throws IOException, ContentNotFoundException {

        uri =  StringUtils.removeStart(uri, "/") + "/data.json";
        Path dataPath = this.content.toPath(uri);

        // Look for a data.json file, or
        // fall back to adding a .json file extension
        if (Files.exists(dataPath)) {
            return Files.newInputStream(dataPath);
        } else {
            throw new ContentNotFoundException("No data found under  " + uri);
        }

    }

}
