package com.github.onsdigital.zebedee.data.importing;

import au.com.bytecode.opencsv.CSVReader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Import data from a CSV to update timeseries data.
 */
public class CsvTimeseriesUpdateImporter implements TimeseriesUpdateImporter {

    private final Path csvPath;

    /**
     * Initialise a new data import for the given CSV file path.
     *
     * @param csvPath
     */
    public CsvTimeseriesUpdateImporter(Path csvPath) {
        this.csvPath = csvPath;
    }

    /**
     * Import data and return as a collection of timeseries update commands.
     *
     * @return
     */
    @Override
    public ArrayList<TimeseriesUpdateCommand> importData() throws IOException {
        ArrayList<TimeseriesUpdateCommand> commands = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(csvPath.toFile()), Charset.forName("UTF8")), ',')) {
            String[] strings = reader.readNext();

            while (strings != null) {

                if (strings.length > 0) {
                    TimeseriesUpdateCommand command = new TimeseriesUpdateCommand();
                    command.cdid = strings[0];

                    if (strings.length > 1)
                        command.title = strings[1];

                    commands.add(command);
                }

                strings = reader.readNext();
            }
        } catch (FileNotFoundException e) {
            throw new IOException("File not found.", e);
        }

        return commands;
    }
}
