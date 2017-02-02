package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.data.processing.setup.DataIndexBuilder;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.content.CompoundContentReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;

/**
 * Created by carlhembrough on 29/09/2016.
 */
public class TimeseriesUpdater {

    public static void updateTimeseries(String[] args) throws InterruptedException, BadRequestException, NotFoundException, IOException {
        // args[1] - source data directory
        // args[2] - destination directory to save the updated timeseries (can be a collection or master)
        // args[3] - A comma seperated list of CDID's to apply the changes to.
        // args[4] - A comma seperated list of dataset ids to apply the changes to.

        Path source = Paths.get(args[1]);
        Path destination = Paths.get(args[2]);

        Set<String> cdids = new TreeSet<>(Arrays.asList(args[3].split(",")));
        Set<String> datasets = new TreeSet<>(Arrays.asList(args[4].split(",")));

        logDebug("CDID's to update").addParameter("cdids", cdids).log();
        logDebug("Datasets to update").addParameter("datasets", datasets).log();

        addTimeseriesNote(source, destination, cdids, datasets);
    }

    private static void addTimeseriesNote(Path source, Path destination, Set<String> cdids, Set<String> datasets) throws InterruptedException, NotFoundException, IOException, BadRequestException {

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

                TimeSeries timeSeries = (TimeSeries) compoundContentReader.getContent(uri);
                logDebug("Updating timeseries.").cdid(cdid).addParameter("uri", uri).log();

                timeSeries.setNotes(new ArrayList<>());
                timeSeries.getNotes().add("Following a quality review it has been identified that the methodology used to estimate elements of purchased software within gross fixed capital formation (GFCF) has led to some double counting from 1997 onwards. When this issue is amended in The Blue Book 2017 it will reduce the level of GFCF across the period by around 1.1% per year. The average impact on quarter-on-quarter GFCF growth is negative 0.02% and the average impact on quarter-on-quarter GDP growth is 0.00%.");
                contentWriter.writeObject(timeSeries, uri + "/data.json");
            }
        }
    }
}
