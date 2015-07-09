package com.github.onsdigital.zebedee.util;

import au.com.bytecode.opencsv.CSVReader;
import com.github.onsdigital.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.content.partial.TimeseriesValue;
import com.github.onsdigital.content.util.ContentUtil;
import com.github.onsdigital.zebedee.Zebedee;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Created by thomasridd on 07/07/15.
 *
 * Wrangler contains
 */
public class Wrangler {
    Zebedee zebedee;

    public Wrangler(Zebedee zebedee) {
        this.zebedee = zebedee;
    }

    /**
     * Updates headline numbers of all timeseries
     *
     * @throws IOException
     */
    public void updateTimeSeriesNumbers() throws IOException {
        List<Path> paths = launchpadMatching(timeSeriesMatcher());

        for (Path path: paths) {
            TimeSeries timeseries;
            try (InputStream stream = Files.newInputStream(zebedee.path.resolve(path))) {
                timeseries = ContentUtil.deserialise(stream, TimeSeries.class);
            }

            List<TimeseriesValue> values = new ArrayList<>();

            if (timeseries.years != null) {
                Iterator<TimeseriesValue> iterator = timeseries.years.iterator();
                while (iterator.hasNext()) {
                    values.add(iterator.next());
                }
            }
            if (timeseries.months != null) {
                Iterator<TimeseriesValue> iterator = timeseries.months.iterator();
                while (iterator.hasNext()) {
                    values.add(iterator.next());
                }
            }
            if (timeseries.quarters != null) {
                Iterator<TimeseriesValue> iterator = timeseries.quarters.iterator();
                while (iterator.hasNext()) {
                    values.add(iterator.next());
                }
            }

            class CustomComparator implements Comparator<TimeseriesValue> {
                @Override
                public int compare(TimeseriesValue o1, TimeseriesValue o2) {
                    return o1.toDate().compareTo(o2.toDate());
                }
            }

            if (values.size() > 0) {
                Collections.sort(values, new CustomComparator());
                TimeseriesValue value = values.get(values.size() - 1);
                timeseries.getDescription().setNumber(value.value);
                System.out.println("Setting " + value.value + " (" + value.date + ") for series " + timeseries.getUri().toString());
                try (OutputStream stream = Files.newOutputStream(zebedee.path.resolve(path))) {
                    IOUtils.write(ContentUtil.serialise(timeseries), stream);
                }
            }

        }
    }

    /**
     * Updates details of timeseries files based on a spreadsheet
     *
      * @param timeSeriesDetailsFile Path to a csv file of the form [CDID, Pre Unit, Unit] including a header
     * @throws IOException
     */
    public void updateTimeSeriesDetails(Path timeSeriesDetailsFile) throws IOException {
        int updates = 0;

        // Build the details file into a hashmap
        HashMap<String, HashMap<String,String>> timeSeriesDetails = new HashMap<>();
        try(CSVReader reader = new CSVReader(new InputStreamReader(Files.newInputStream(timeSeriesDetailsFile),"cp1252"))) {
            List<String[]> records = reader.readAll();

            Iterator<String[]> iterator = records.iterator();
            iterator.next();

            while(iterator.hasNext()){
                String[] record = iterator.next();
                HashMap<String, String> seriesDetails = new HashMap<>();

                seriesDetails.put("CDID", record[0].toLowerCase());
                seriesDetails.put("Pre unit", record[1]);
                seriesDetails.put("Units", record[2]);
                timeSeriesDetails.put(record[0].toLowerCase(), seriesDetails);
            }

        }

        // Iterate through time-series
        List<Path> paths = launchpadMatching(timeSeriesMatcher());
        for (Path path: paths) {
            TimeSeries timeseries;

            try (InputStream stream = Files.newInputStream(zebedee.path.resolve(path))) {
                timeseries = ContentUtil.deserialise(stream, TimeSeries.class);
            }

            if (timeseries != null) {
                HashMap<String, String> details = null;
                for (String key: timeSeriesDetails.keySet()) {
                    if (key.equalsIgnoreCase(timeseries.getCdid().toLowerCase())) {
                        details = timeSeriesDetails.get(key);
                        break;
                    }
                }
                if (details != null) {
                    System.out.println(++updates + ") Going to update timeseries with CDID: " + timeseries.getCdid() + " & uri: " + timeseries.getUri().toString());
                    timeseries.getDescription().setPreUnit(details.get("Pre unit"));
                    timeseries.getDescription().setUnit(details.get("Units"));
                    try (OutputStream stream = Files.newOutputStream(zebedee.path.resolve(path))) {
                        IOUtils.write(ContentUtil.serialise(timeseries), stream);
                    }
                }
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * List of Paths from launchpad using a pathmatcher filter
     *
     * @param matcher
     * @return
     * @throws IOException
     */
    private List<Path> launchpadMatching(final PathMatcher matcher) throws IOException {
        Path startPath = zebedee.launchpad.path;
        final List<Path> paths = new ArrayList<>();

        Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException
            {
                if (matcher.matches(file)) {
                    paths.add(zebedee.path.relativize(file));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return paths;
    }

    /**
     * Pathmatcher filter that picks up timeseries
     *
     * @return
     */
    private static PathMatcher timeSeriesMatcher() {
        PathMatcher matcher = new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                if (path.toString().contains("data.json") && path.toString().contains("timeseries")) {
                    return true;
                }
                return false;
            }
        };
        return  matcher;
    }


}
