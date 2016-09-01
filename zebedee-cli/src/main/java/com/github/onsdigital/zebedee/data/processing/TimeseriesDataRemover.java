package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;
import com.github.onsdigital.zebedee.data.processing.setup.DataIndexBuilder;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.content.CompoundContentReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logDebug;

public class TimeseriesDataRemover {


    public static void removeTimeseriesEntries(String[] args) throws InterruptedException, BadRequestException, NotFoundException, IOException {
        // args[1] - source data directory
        // args[2] - destination directory to save the updated timeseries (can be a collection or master)
        // args[3] - the CDID of the timeseries to remove entries from.
        // args[4]... the dates to remove entries for.

        Path source = Paths.get(args[1]);
        Path destination = Paths.get(args[2]);

        Set<String> cdids = new TreeSet<>(Arrays.asList(args[3].split(",")));
        Set<String> datasets = new TreeSet<>(Arrays.asList(args[4].split(",")));
        Set<String> dates = new HashSet<>();

        logDebug("TimeSeries entries to remove").addParameter("targets", cdids).log();

        for (int i = 4; i < args.length; i++) {
            dates.add(args[i]);
        }

        logDebug("Dates to remove").addParameter("dates", dates).log();
        removeTimeseriesEntries(source, destination, cdids, datasets, dates);
    }

    public static void removeTimeseriesData(String[] args) throws Exception {

        // args[1] - source data directory
        // args[2] - destination directory to save the updated timeseries (can be a collection or master)
        // args[3] - the type of data to remove (monthly | quarterly | yearly)
        // args[4]... the timeseries CDID's to update

        Path source = Paths.get(args[1]);
        Path destination = Paths.get(args[2]);

        DataResoltion dataResoltion = DataResoltion.valueOf(args[3]);
        Set<String> cdids = new HashSet<>();

        for (int i = 4; i < args.length; i++) {
            cdids.add(args[i]);
        }

        logDebug("TimesSeries CDID's to be removed").addParameter("targets", cdids).log();
        removeTimeseriesData(source, destination, dataResoltion, cdids);
    }

    private static void removeTimeseriesEntries(Path source, Path destination, Set<String> cdids, Set<String> datasets, Set<String> dates)
            throws InterruptedException, NotFoundException, IOException, BadRequestException {
// build the data index so we know where to find timeseries files given the CDID
        ContentReader contentReader = new FileSystemContentReader(source);

        // create a compound reader to check if the file already exists in the destination before reading it from the source.
        CompoundContentReader compoundContentReader = new CompoundContentReader(contentReader);
        compoundContentReader.add(new FileSystemContentReader(destination));

        ContentWriter contentWriter = new ContentWriter(destination);

        DataIndex dataIndex = DataIndexBuilder.buildDataIndex(contentReader);

        for (String cdid : cdids) {
            if (cdid == null || cdid.length() == 0)
                continue;

            String uri = dataIndex.getUriForCdid(cdid.toLowerCase());

            if (uri == null) {
                logDebug("TimeSeries data not found in data index").cdid(cdid).log();
                return;
            }

            for (String dataset : datasets) {

                uri += "/" + dataset;

                TimeSeries page = (TimeSeries) compoundContentReader.getContent(uri);
                logDebug("Removing TimeSeries entries").cdid(cdid).addParameter("uri", uri).log();

                for (String date : dates) {
                    removeLabel(date, page.years);
                    removeLabel(date, page.quarters);
                    removeLabel(date, page.months);
                }

                contentWriter.writeObject(page, uri + "/data.json");
            }
        }

    }

    private static void removeLabel(String date, TreeSet<TimeSeriesValue> values) {
        List<TimeSeriesValue> toRemove = values.stream().filter(item -> item.date.equalsIgnoreCase(date)).collect(Collectors.toList());
        toRemove.forEach(item -> {
            values.remove(item);
            logDebug("Removed item").addParameter("itemDate", item.date).log();
        });
    }

    private static void removeTimeseriesData(Path source, Path destination, DataResoltion dataResoltion, Set<String> cdids) throws InterruptedException, ZebedeeException, IOException {

        // build the data index so we know where to find timeseries files given the CDID
        ContentReader contentReader = new FileSystemContentReader(source);

        // create a compound reader to check if the file already exists in the destination before reading it from the source.
        CompoundContentReader compoundContentReader = new CompoundContentReader(contentReader);
        compoundContentReader.add(new FileSystemContentReader(destination));

        ContentWriter contentWriter = new ContentWriter(destination);

        DataIndex dataIndex = DataIndexBuilder.buildDataIndex(contentReader);

        for (String cdid : cdids) {
            String uri = dataIndex.getUriForCdid(cdid.toLowerCase());

            if (uri == null) {
                logDebug("Timeseries file not found in data index").cdid(cdid).log();
                continue;
            }

            TimeSeries page = (TimeSeries) compoundContentReader.getContent(uri);

            switch (dataResoltion) {
                case months:
                    logDebug("Removing monthly Timeseries data").cdid(cdid).addParameter("uri", uri).log();
                    page.months = new TreeSet<>();
                    break;
                case quarters:
                    logDebug("Removing quarterly Timeseries data").cdid(cdid).addParameter("uri", uri).log();
                    page.quarters = new TreeSet<>();
                    break;
                case years:
                    logDebug("Removing yearly Timeseries data").cdid(cdid).addParameter("uri", uri).log();
                    page.years = new TreeSet<>();
                    break;
            }

            contentWriter.writeObject(page, uri + "/data.json");
        }
    }

    public enum DataResoltion {
        months,
        quarters,
        years
    }
}
