package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class TimeseriesFinder extends SimpleFileVisitor<Path> {

    List<Path> timeseriesFiles = new ArrayList<>();
    Path root;

    public static void findTimeseriesForSourceDataset(String[] args) {
        // args[1] - source data directory.
        // args[2] - the id of the source dataset.

        Path source = Paths.get(args[1]);
        String sourceDataset = args[2];

        findTimeseriesForSourceDataset(source, sourceDataset)
                .forEach(System.out::println);
    }

    /**
     * find all timeseries files that contain only the given source dataset.
     *
     * @param root
     * @param sourceDataset
     * @return
     */
    public static List<Path> findTimeseriesForSourceDataset(Path root, String sourceDataset) {

        List<Path> timeseriesFiles = new TimeseriesFinder().findTimeseries(root);
        List<Path> result = new ArrayList<>();

        timeseriesFiles.forEach(path -> {
            try {

                TimeSeries timeSeries = (TimeSeries) ContentUtil.deserialiseContent(new FileInputStream(path.toFile()));

                if (timeSeries.sourceDatasets != null
                        && timeSeries.sourceDatasets.size() == 1
                        && timeSeries.sourceDatasets.get(0).equalsIgnoreCase(sourceDataset)) {
                    result.add(path);
                }
            } catch (FileNotFoundException e) {
                System.out.println("exception: " + e);
            }
        });

        return result;
    }

    public List<Path> findTimeseries(Path root) {
        this.root = root;

        try {
            Files.walkFileTree(root, this);
        } catch (NoSuchFileException e) {
            return new ArrayList<>();
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
        if (uri.endsWith("data.json") && uri.toString().contains("/timeseries/") && !VersionedContentItem.isVersionedUri(uri)) {
            uri = uri.substring(0, uri.length() - "/data.json".length());

            //Log.print("Adding file with uri: %s and path %s", uri, path.toString());
            this.timeseriesFiles.add(path);
        }
        return FileVisitResult.CONTINUE;
    }
}
