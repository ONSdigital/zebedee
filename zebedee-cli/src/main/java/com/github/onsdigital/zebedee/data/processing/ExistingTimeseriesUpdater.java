package com.github.onsdigital.zebedee.data.processing;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.github.onsdigital.zebedee.data.importing.CsvTimeseriesUpdateImporter;
import com.github.onsdigital.zebedee.data.importing.TimeseriesUpdateCommand;
import com.github.onsdigital.zebedee.data.importing.TimeseriesUpdateImporter;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.util.Log;
import com.github.onsdigital.zebedee.util.TimeseriesUpdater;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Given as CSV indexed with the timeseries CDID, update each timeseries with the given data
 * and update any CSV / XLS download files with the new titles.
 */
public class ExistingTimeseriesUpdater {


    public static void updateTimeseriesData(String[] args) throws Exception {

        // args[1] - source data directory
        // args[2] - destination directory to save the updated timeseries (can be a collection or master)
        // args[3] - path to the CSV file containing the names

        Path source = Paths.get(args[1]);
        Path destination = Paths.get(args[2]);
        Path csvInput = Paths.get(args[3]);

        updateTimeseries(source, destination, csvInput);
    }

    public static void updateTimeseries(Path source, Path destination, Path csvInput) throws IOException, InterruptedException {

        // build the data index so we know where to find timeseries files given the CDID
        ContentReader contentReader = new ContentReader(source);
        ContentWriter contentWriter = new ContentWriter(destination);

        DataIndex dataIndex = buildDataIndex(contentReader);

        // read the CSV and update the timeseries titles.
        TimeseriesUpdateImporter importer = new CsvTimeseriesUpdateImporter(csvInput);

        System.out.println("Importing CSV file...");
        ArrayList<TimeseriesUpdateCommand> updateCommandsImported = importer.importData();

        ArrayList<TimeseriesUpdateCommand> updateCommands = TimeseriesUpdater.filterTimeseriesThatDoNotExist(dataIndex, updateCommandsImported);

        System.out.println("Updating timeseries with new metadata...");
        TimeseriesUpdater.updateTimeseriesMetadata(contentReader, contentWriter, dataIndex, updateCommands);

        System.out.println("Finding all CSDB files...");
        List<TimeseriesDatasetDownloads> datasetDownloads = findCsdbFiles(source);

        System.out.println("Working out which CSDB files need their download files updated...");
        Set<TimeseriesDatasetDownloads> datasetDownloadsToUpdate = determineWhatDownloadsNeedUpdating(updateCommands, datasetDownloads);

        System.out.println("Generating new downloads...");
        for (TimeseriesDatasetDownloads timeseriesDatasetDownloads : datasetDownloadsToUpdate) {

            try {
                // get all the update commands for this particular CSDB
                Set<TimeseriesUpdateCommand> commandsForThisDataset = getCommandsForDataset(updateCommands, timeseriesDatasetDownloads);

                // populate the column indexes for each command so we know what column to update the title for.
                populateCsvColumnIndexesToUpdate(timeseriesDatasetDownloads, commandsForThisDataset, source);
                generateCsv(source, destination, timeseriesDatasetDownloads, commandsForThisDataset);
                generateXlsx(source, destination, timeseriesDatasetDownloads, commandsForThisDataset);

            } catch (Exception e) {
                Log.print(e);
            }
        }
    }

    public static void generateXlsx(Path source, Path destination, TimeseriesDatasetDownloads timeseriesDatasetDownloads, Set<TimeseriesUpdateCommand> commandsForThisDataset) throws IOException {
        int rowIndex = 0;
        File inputCsv = source.resolve(timeseriesDatasetDownloads.getCsvPath()).toFile();
        System.out.println("inputCsv = " + inputCsv);
        File outputTempXls = destination.resolve(timeseriesDatasetDownloads.getXlsTempPath()).toFile();
        File outputFinalXls = destination.resolve(timeseriesDatasetDownloads.getXlsPath()).toFile();
        Files.createDirectories(outputTempXls.toPath().getParent());
        System.out.println("outputXls = " + outputTempXls);

        try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(inputCsv), Charset.forName("UTF8")), ',')) {

            Workbook wb = new SXSSFWorkbook(30);
            Sheet sheet = wb.createSheet("data");

            int rownum = 0;
            String[] strings = reader.readNext();
            //System.out.println("strings.length = " + strings.length);

            while (strings != null) {

                if (rowIndex == 0) { // the row with all the titles in
                    // set the updated titles
                    for (TimeseriesUpdateCommand command : commandsForThisDataset) {
                        Integer index = command.datasetCsvColumn.get(timeseriesDatasetDownloads.getCsdbId());
                        if (index != null) {
                            strings[index] = command.title;
                        }
                    }
                }

                Row row = sheet.createRow(rownum++);

                int colnum = 0;
                for (String gridCell : strings) {
                    row.createCell(colnum).setCellValue(gridCell);
                    colnum++;
                }

                strings = reader.readNext();
                rowIndex++;
            }

            try (OutputStream stream = new FileOutputStream(outputTempXls)) {
                wb.write(stream);
            }
        }

        Files.move(outputTempXls.toPath(), outputFinalXls.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void generateCsv(Path source, Path destination, TimeseriesDatasetDownloads timeseriesDatasetDownloads, Set<TimeseriesUpdateCommand> commandsForThisDataset) throws IOException {
        int rowIndex = 0;
        File inputCsv = source.resolve(timeseriesDatasetDownloads.getCsvPath()).toFile();
        System.out.println("inputCsv = " + inputCsv);
        File outputTempCsv = destination.resolve(timeseriesDatasetDownloads.getCsvTempPath()).toFile();
        File outputFinalCsv = destination.resolve(timeseriesDatasetDownloads.getCsvPath()).toFile();
        Files.createDirectories(outputTempCsv.toPath().getParent());
        System.out.println("outputTempCsv = " + outputTempCsv);

        try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(inputCsv), Charset.forName("UTF8")), ',')) {
            try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(outputTempCsv), Charset.forName("UTF8")), ',')) {

                String[] strings = reader.readNext();

                while (strings != null) {

                    if (rowIndex == 0) { // the row with all the titles in
                        // set the updated titles
                        for (TimeseriesUpdateCommand command : commandsForThisDataset) {
                            Integer index = command.datasetCsvColumn.get(timeseriesDatasetDownloads.getCsdbId());
                            if (index != null) {
                                System.out.println("Setting CSV title from " + strings[index] +
                                        " to: " + command.title +
                                        " index: " + index +
                                        " cdid: " + command.cdid +
                                        " CSDB: " + timeseriesDatasetDownloads.getCsdbId());
                                strings[index] = command.title;
                            }
                        }
                    }

                    writer.writeNext(strings);
                    strings = reader.readNext();
                    rowIndex++;
                }
            }
        }

        Files.move(outputTempCsv.toPath(), outputFinalCsv.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void populateCsvColumnIndexesToUpdate(TimeseriesDatasetDownloads timeseriesDatasetDownloads, Set<TimeseriesUpdateCommand> commandsForThisDataset, Path source) throws IOException {
        File inputCsv = source.resolve(timeseriesDatasetDownloads.getCsvPath()).toFile();
        try (CSVReader reader = new CSVReader(new InputStreamReader(
                new FileInputStream(inputCsv), Charset.forName("UTF8")), ',')) {

            String[] strings = reader.readNext(); // first row - titles
            strings = reader.readNext(); // second row - CDID's

            int columnIndex = 0;
            for (String cdid : strings) {
                for (TimeseriesUpdateCommand command : commandsForThisDataset) {
                    if (command.cdid.equalsIgnoreCase(cdid)) {

//                        for (String sourceDataset : command.sourceDatasets) {
//                            if (sourceDataset.equalsIgnoreCase(timeseriesDatasetDownloads.getCsdbId())) {
//
//                                System.out.println("CSV index for command :" + command.cdid);
//                                System.out.println("csdb id: " + timeseriesDatasetDownloads.getCsdbId());
//                                System.out.println("column index: " + columnIndex);
//                                command.datasetCsvColumn.put(timeseriesDatasetDownloads.getCsdbId(), columnIndex);
//                            }
//                        }

//                        System.out.println("CSV index for command :" + command.cdid);
//                        System.out.println("csdb id: " + timeseriesDatasetDownloads.getCsdbId());
//                        System.out.println("column index: " + columnIndex);
                        command.datasetCsvColumn.put(timeseriesDatasetDownloads.getCsdbId(), columnIndex);
                    }
                }

                columnIndex++;
            }
        }
    }

    public static Set<TimeseriesUpdateCommand> getCommandsForDataset(ArrayList<TimeseriesUpdateCommand> updateCommands, TimeseriesDatasetDownloads timeseriesDatasetDownloads) {
        Set<TimeseriesUpdateCommand> commandsForThisDataset = new HashSet<>();
        for (TimeseriesUpdateCommand updateCommand : updateCommands) {

            if (updateCommand.sourceDatasets != null) {
                for (String sourceDataset : updateCommand.sourceDatasets) {
                    if (sourceDataset.equalsIgnoreCase(timeseriesDatasetDownloads.getCsdbId())) {
                        System.out.println("updateCommand " + updateCommand.cdid + " added to dataset " + timeseriesDatasetDownloads.getCsdbId());
                        commandsForThisDataset.add(updateCommand);
                    }
                }
            } else {
                System.out.println("There are no source datasets defined for CDID: " + updateCommand.cdid);

                commandsForThisDataset.add(updateCommand);
            }

        }
        return commandsForThisDataset;
    }

    public static Set<TimeseriesDatasetDownloads> determineWhatDownloadsNeedUpdating(ArrayList<TimeseriesUpdateCommand> updateCommands, List<TimeseriesDatasetDownloads> datasetDownloads) {
        Set<TimeseriesDatasetDownloads> datasetDownloadsToUpdate = new HashSet<>();
        for (TimeseriesUpdateCommand command : updateCommands) {

            if (command.sourceDatasets != null) {
                for (String sourceDataset : command.sourceDatasets) {
                    for (TimeseriesDatasetDownloads datasetDownload : datasetDownloads) {
                        if (sourceDataset.equalsIgnoreCase(datasetDownload.getCsdbId())) {
                            System.out.println("datasetDownload to update = " + datasetDownload.getCsdbId());
                            datasetDownloadsToUpdate.add(datasetDownload);
                        }
                    }
                }
            }

        }
        return datasetDownloadsToUpdate;
    }

    public static List<TimeseriesDatasetDownloads> findCsdbFiles(Path source) {
        CsdbFinder csdbFinder = new CsdbFinder();
        csdbFinder.find(source);
        List<TimeseriesDatasetDownloads> datasetDownloads = new ArrayList<>();
        for (String uri : csdbFinder.uris) {
            datasetDownloads.add(new TimeseriesDatasetDownloads(Paths.get(uri)));
        }
        return datasetDownloads;
    }

    public static DataIndex buildDataIndex(ContentReader contentReader) throws InterruptedException {
        DataIndex dataIndex = new DataIndex(contentReader);

        while (!dataIndex.indexBuilt) {
            Thread.sleep(1000);
            System.out.print(".");
        }
        System.out.println("");
        return dataIndex;
    }
}
