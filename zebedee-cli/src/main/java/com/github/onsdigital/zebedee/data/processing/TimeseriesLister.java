package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import com.github.onsdigital.zebedee.reader.Resource;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TimeseriesLister {

    public static void listTimeseries(String[] args) throws InterruptedException, ZebedeeException, IOException {
        // args[1] - source data directory
        // args[2] - destination directory to save the updated timeseries (can be a collection or master)

        Path source = Paths.get(args[1]);
        Path destination = Paths.get(args[2]);


        listTimeseries(source, destination);
    }


    private static void listTimeseries(Path source, Path destination) throws ZebedeeException, IOException {

        ContentReader contentReader = new FileSystemContentReader(source);

        // find the paths of all timeseries
        List<Path> timeseries = getPaths(source);
        List<TimeseriesCsvEntry> entries = getTimeseriesCsvEntries(source, contentReader, timeseries);
        writeCsvFile(destination, entries);
    }

    private static void writeCsvFile(Path destination, List<TimeseriesCsvEntry> entries) {
        try {
            FileWriter writer = new FileWriter(destination.toFile());

            for (TimeseriesCsvEntry entry : entries) {
                writer.append(entry.cdid);
                writer.append(',');
                writer.append("\"" + entry.title + "\"");
                writer.append(',');
                writer.append("\"" + entry.path + "\"");
                writer.append('\n');
            }

            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<TimeseriesCsvEntry> getTimeseriesCsvEntries(Path source, ContentReader contentReader, List<Path> timeseries) throws IOException, ZebedeeException {
        List<TimeseriesCsvEntry> entries = new ArrayList<>();

        // read each one and capture the fields we want to list.
        for (Path path : timeseries) {
            String uri = source.relativize(path).toString();
            try (Resource resource = contentReader.getResource(uri)) {
                TimeSeries page = (TimeSeries) ContentUtil.deserialiseContent(resource.getData());
                if (page == null) { //Contents without type is null when deserialised. There should not be no such data
                    continue;
                }

                TimeseriesCsvEntry entry = new TimeseriesCsvEntry();
                entry.cdid = page.getCdid();
                entry.title = page.getDescription().getTitle();
                entry.path = uri;
                entries.add(entry);
                System.out.println("CDID:" + page.getCdid() + " Title: " + page.getDescription().getTitle());
            }
        }
        return entries;
    }

    private static List<Path> getPaths(Path source) {
        TimeseriesFinder timeseriesFinder = new TimeseriesFinder();
        return timeseriesFinder.findTimeseries(source);
    }

    public static void main(String[] args) {
        Path root = Paths.get("/some/dir");
        Path path = Paths.get("/some/dir/and/the/file");

        System.out.println(root.relativize(path).toString());
        System.out.println(path.relativize(root).toString());
    }

    static class TimeseriesCsvEntry {
        public String cdid;
        public String title;
        public String path;
    }
}
