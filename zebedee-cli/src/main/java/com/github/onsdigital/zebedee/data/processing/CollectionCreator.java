package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CollectionCreator {

    public static void createCollection(String[] args) throws Exception {

        Path source = Paths.get(args[1]);
        String collectionName = args[2];

        CreateCollection(source, collectionName);
    }

    public static void CreateCollection(Path source, String collectionName) throws IOException, InterruptedException {

        // build the data index so we know where to find timeseries files given the CDID
        ContentReader contentReader = new ContentReader(source);


    }
}
