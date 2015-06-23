package com.github.onsdigital.zebedee.data;

import com.github.onsdigital.content.service.ContentNotFoundException;
import com.github.onsdigital.content.service.ContentService;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by bren on 13/06/15.
 */
public class DataReader implements ContentService {
    @Override
    public InputStream readData(String uri) throws ContentNotFoundException {
        try {
            return getDataStream(uri);
        }  catch (IOException e) {
            throw new RuntimeException("Failed reading data at " + uri);
        }
    }

    private InputStream getDataStream(String uriString)
            throws IOException, ContentNotFoundException {
        System.out.println("Reading data under uri:" + uriString);
        Path dataPath = Root.zebedee.published.toPath(uriString);

        // Look for a data.json file, or
        // fall back to adding a .json file extension
        Path data = dataPath.resolve("data.json");
        if (Files.exists(data)) {
            return Files.newInputStream(data);
        } else {
            throw new ContentNotFoundException("No data found under  " + uriString);
        }

    }

}
