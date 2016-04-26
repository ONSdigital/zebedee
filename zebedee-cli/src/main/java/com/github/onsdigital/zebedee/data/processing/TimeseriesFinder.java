package com.github.onsdigital.zebedee.data.processing;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class TimeseriesFinder extends SimpleFileVisitor<Path> {

    List<Path> timeseriesFiles = new ArrayList<>();
    Path root;

    public List<Path> findTimeseries(Path root) {
        this.root = root;

        try {
            Files.walkFileTree(root, this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this.timeseriesFiles;
    }


    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attr) throws IOException {
        // Get the uri
        String uri = "/" + root.relativize(path).toString();

        // Check json files in timeseries directories (excluding versions)
        if (uri.endsWith("data.json") && uri.toString().contains("/timeseries/")) {
            uri = uri.substring(0, uri.length() - "/data.json".length());

            //Log.print("Adding file with uri: %s and path %s", uri, path.toString());
            this.timeseriesFiles.add(path);
        }
        return FileVisitResult.CONTINUE;
    }
}
