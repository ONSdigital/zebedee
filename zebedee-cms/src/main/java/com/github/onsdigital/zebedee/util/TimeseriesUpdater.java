package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.data.importing.CsvTimeseriesUpdateImporter;
import com.github.onsdigital.zebedee.data.importing.TimeseriesUpdateCommand;
import com.github.onsdigital.zebedee.data.importing.TimeseriesUpdateImporter;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.content.CompoundContentReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;

/**
 * Given as CSV indexed with the timeseries CDID, update each timeseries with the given data.
 */
public class TimeseriesUpdater {

    public static void updateTimeseries(CompoundContentReader contentReader, ContentWriter contentWriter, InputStream csvInput, DataIndex dataIndex) throws IOException {

        // read the CSV and update the timeseries titles.
        TimeseriesUpdateImporter importer = new CsvTimeseriesUpdateImporter(csvInput);

        info().log("Importing CSV file");
        ArrayList<TimeseriesUpdateCommand> updateCommandsImported = importer.importData();

        ArrayList<TimeseriesUpdateCommand> updateCommands = filterTimeseriesThatDoNotExist(dataIndex, updateCommandsImported);

        info().log("Updating timeseries with new metadata");
        updateTimeseriesMetadata(contentReader, contentWriter, updateCommands);
    }

    public static ArrayList<TimeseriesUpdateCommand> filterTimeseriesThatDoNotExist(DataIndex dataIndex, ArrayList<TimeseriesUpdateCommand> updateCommandsImported) {
        ArrayList<TimeseriesUpdateCommand> updateCommands = new ArrayList<>();

        for (TimeseriesUpdateCommand command : updateCommandsImported) {
            String uri = dataIndex.getUriForCdid(command.cdid.toLowerCase());

            if (uri == null) {
                info().data("CDID", command.cdid).log("CDID not found in data index");
                continue;
            } else {
                command.uri = uri + "/" + command.dataset.toLowerCase();
                updateCommands.add(command);
            }
        }
        return updateCommands;
    }

    public static void updateTimeseriesMetadata(CompoundContentReader contentReader, ContentWriter contentWriter, ArrayList<TimeseriesUpdateCommand> updateCommands) throws IOException {
        for (TimeseriesUpdateCommand command : updateCommands) {

            try {
                boolean updated = false;
                TimeSeries page = (TimeSeries) contentReader.getContent(command.uri);

                if (command.title != null && command.title.length() > 0) {
                    page.getDescription().setTitle(command.title);
                    updated = true;
                }

                if (command.preunit != null && command.preunit.length() > 0) {
                    page.getDescription().setPreUnit(command.preunit);
                    updated = true;
                }

                if (command.unit != null && command.unit.length() > 0) {
                    page.getDescription().setUnit(command.unit);
                    updated = true;
                }

                if (command.releaseDate != null) {
                    page.getDescription().setReleaseDate(command.releaseDate);
                    updated = true;
                }

                if (updated) {
                    contentWriter.writeObject(page, command.uri + "/data.json");
                }

            } catch (Exception e) {
                error().data("uri", command.uri).logException(e, "Failed to read timeseries page");
            }
        }
    }
}
