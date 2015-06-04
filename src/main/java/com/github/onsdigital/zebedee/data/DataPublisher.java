package com.github.onsdigital.zebedee.data;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.model.PathUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by thomasridd on 04/06/15.
 */
public class DataPublisher {
    public static void preprocessCollection(Collection collection, Session session) throws IOException, BadRequestException, UnauthorizedException {
        preprocessCSDB(collection, session);
    }
    private static void preprocessCSDB(Collection collection, Session session) throws IOException, BadRequestException, UnauthorizedException {
        List<HashMap<String, Path>> datasets = csdbDatasetsInCollection(collection);
        for(HashMap<String, Path> dataset: datasets) {

            List<Object> serieses = callBrianToProcessCSDB(dataset.get("file"));
            Object datasetJson = Serialiser.deserialise(FileUtils.openInputStream(dataset.get("json").toFile()), Object.class);

            for(Object series: serieses) {
                // add metadata from datasetDetails to the timeseries object
                // series.releasedate = datasetJson.releasedate;
                // series.nextrelease = datasetJson.nextrelease;

                String uri = null; // = datasetJson.uri + "/" + series.id;

                // We want to copy our new series file to the reviewed section for the uri
                Path savePath = collection.autocreatePath(uri).resolve("data.json");

                // Write the json
                IOUtils.write(Serialiser.serialise(series), FileUtils.openOutputStream(savePath.toFile()));

                // Write csv and other files:
                // ...
            }
        }
    }
    private static List<HashMap<String, Path>> csdbDatasetsInCollection(Collection collection) {
        return null;
    }
    private static List<Object> callBrianToProcessCSDB(Path path) {
        return null;
    }


}
