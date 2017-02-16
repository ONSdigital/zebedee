package com.github.onsdigital.zebedee.data.importing;

import au.com.bytecode.opencsv.CSVReader;
import com.github.onsdigital.zebedee.content.util.ContentConstants;
import com.github.onsdigital.zebedee.content.util.IsoDateSerializer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * Import data from a CSV to update timeseries data.
 */
public class CsvTimeseriesUpdateImporter implements TimeseriesUpdateImporter {

    private static final IsoDateSerializer dateSerializer = new IsoDateSerializer(ContentConstants.JSON_DATE_PATTERN);
    private final InputStream inputStream;

    /**
     * Initialise a new data import for the given CSV file path.
     * @param inputStream
     */
    public CsvTimeseriesUpdateImporter(InputStream inputStream) {
        this.inputStream = inputStream;
    }


    /**
     * Import data and return as a collection of timeseries update commands.
     *
     * @return
     */
    @Override
    public ArrayList<TimeseriesUpdateCommand> importData() throws IOException {
        ArrayList<TimeseriesUpdateCommand> commands = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream, Charset.forName("UTF8")), ',')) {
            String[] strings = reader.readNext();

            while (strings != null) {

                if (strings.length > 0) {
                    TimeseriesUpdateCommand command = new TimeseriesUpdateCommand();
                    command.cdid = strings[0];
                    command.dataset = strings[1];

//                    if (strings.length > 2)
//                        command.title = strings[2];//command.releaseDate = dateSerializer.deserialize(strings[1]);

                    command.preunit = strings[2];
                    command.unit = strings[3];

                    commands.add(command);
                }

                strings = reader.readNext();
            }
        } catch (FileNotFoundException e) {
            throw new IOException("File not found.", e);
        }
//        catch (ParseException e) {
//            throw new IOException("Failed to parse release date", e);
//        }

        return commands;
    }
}
