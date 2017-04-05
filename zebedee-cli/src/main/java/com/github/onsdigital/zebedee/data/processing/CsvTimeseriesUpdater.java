package com.github.onsdigital.zebedee.data.processing;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.github.onsdigital.zebedee.data.importing.CsvTimeseriesUpdateImporter;
import com.github.onsdigital.zebedee.data.importing.TimeseriesUpdateCommand;
import com.github.onsdigital.zebedee.data.importing.TimeseriesUpdateImporter;
import com.github.onsdigital.zebedee.data.processing.setup.DataIndexBuilder;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.content.CompoundContentReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import com.github.onsdigital.zebedee.util.TimeseriesUpdater;
import org.apache.commons.lang3.time.FastDateFormat;
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
import java.util.*;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logError;

/**
 * Given as CSV indexed with the timeseries CDID, update each timeseries with the given data
 * and update any CSV / XLS download files with the new titles.
 */
public class CsvTimeseriesUpdater {

    private static FastDateFormat outputDateFormat = FastDateFormat.getInstance("dd-MM-yyyy", TimeZone.getTimeZone("Europe/London"));

    public static void updateTimeseriesFromCsv(String[] args) throws Exception {

        // args[1] - source data directory
        // args[2] - destination directory to save the updated timeseries (can be a collection or master)
        // args[3] - path to the CSV file containing the names

        Path source = Paths.get(args[1]);
        Path destination = Paths.get(args[2]);
        Path csvInput = Paths.get(args[3]);

        updateTimeseriesFromCsv(source, destination, csvInput);
    }

    public static void updateTimeseriesFromCsv(Path source, Path destination, Path csvInput) throws IOException, InterruptedException {

        // build the data index so we know where to find timeseries files given the CDID
        ContentReader contentReader = new FileSystemContentReader(source);
        ContentWriter contentWriter = new ContentWriter(destination);

        DataIndex dataIndex = DataIndexBuilder.buildDataIndex(contentReader);

        // read the CSV and update the timeseries titles.
        TimeseriesUpdateImporter importer = new CsvTimeseriesUpdateImporter(new FileInputStream(csvInput.toFile()));

        logDebug("Importing CSV file")
                .addParameter("source", source.toString())
                .addParameter("destination", destination.toString())
                .addParameter("csvInput", csvInput.toString())
                .log();
        ArrayList<TimeseriesUpdateCommand> updateCommandsImported = importer.importData();

        ArrayList<TimeseriesUpdateCommand> updateCommands = TimeseriesUpdater.filterTimeseriesThatDoNotExist(dataIndex, updateCommandsImported);

        logDebug("Updating timeseries with new metadata")
                .addParameter("source", source.toString())
                .addParameter("destination", destination.toString())
                .addParameter("csvInput", csvInput.toString())
                .log();
        TimeseriesUpdater.updateTimeseriesMetadata(new CompoundContentReader(contentReader), contentWriter, updateCommands);

        logDebug("Finding all CSDB files").log();
        List<TimeseriesDatasetFiles> datasetDownloads = findCsdbFiles(source);

        logDebug("Working out which CSDB files need their download files updated").log();
        Set<TimeseriesDatasetFiles> datasetDownloadsToUpdate = determineWhatDownloadsNeedUpdating(updateCommands, datasetDownloads);

        logDebug("Generating new downloads").log();
        for (TimeseriesDatasetFiles timeseriesDatasetFiles : datasetDownloadsToUpdate) {

            try {
                // get all the update commands for this particular CSDB
                Set<TimeseriesUpdateCommand> commandsForThisDataset = getCommandsForDataset(updateCommands, timeseriesDatasetFiles);

                // populate the column indexes for each command so we know what column to update the title for.
                populateCsvColumnIndexesToUpdate(timeseriesDatasetFiles, commandsForThisDataset, source);
                generateCsv(source, destination, timeseriesDatasetFiles, commandsForThisDataset);
                generateXlsx(source, destination, timeseriesDatasetFiles, commandsForThisDataset);

            } catch (Exception e) {
                logError(e, "Error updating timeSeries.")
                        .addParameter("source", source.toString())
                        .addParameter("destination", destination.toString())
                        .addParameter("csvInput", csvInput.toString())
                        .log();
            }
        }
    }

    public static void generateXlsx(Path source, Path destination, TimeseriesDatasetFiles timeseriesDatasetFiles, Set<TimeseriesUpdateCommand> commandsForThisDataset) throws IOException {
        int rowIndex = 0;
        File inputCsv = source.resolve(timeseriesDatasetFiles.getCsvPath()).toFile();
        logDebug("Generating Xlsx").addParameter("inputCsv", inputCsv).log();
        File outputTempXls = destination.resolve(timeseriesDatasetFiles.getXlsTempPath()).toFile();
        File outputFinalXls = destination.resolve(timeseriesDatasetFiles.getXlsPath()).toFile();
        Files.createDirectories(outputTempXls.toPath().getParent());
        logDebug("Generated Xlsx").addParameter("outputXls", outputTempXls).log();

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
                        Integer index = command.datasetCsvColumn.get(timeseriesDatasetFiles.getCsdbId());
                        if (index != null) {
                            strings[index] = command.title;
                        }
                    }
                }

//                if (rowIndex == 4) { // the row with all the release dates in
//                    // set the updated titles
//                    for (TimeseriesUpdateCommand command : commandsForThisDataset) {
//                        Integer index = command.datasetCsvColumn.get(timeseriesDatasetDownloads.getCsdbId());
//                        if (index != null) {
//                            System.out.println("Setting CSV date from " + strings[index] +
//                                    " to: " + command.releaseDate +
//                                    " index: " + index +
//                                    " cdid: " + command.cdid +
//                                    " CSDB: " + timeseriesDatasetDownloads.getCsdbId());
//                            strings[index] = outputDateFormat.format(command.releaseDate);
//                        }
//                    }
//                }

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

    public static void generateCsv(Path source, Path destination, TimeseriesDatasetFiles timeseriesDatasetFiles, Set<TimeseriesUpdateCommand> commandsForThisDataset) throws IOException {
        int rowIndex = 0;
        File inputCsv = source.resolve(timeseriesDatasetFiles.getCsvPath()).toFile();
        logDebug("Generate CSV").addParameter("input", inputCsv.getName()).log();
        File outputTempCsv = destination.resolve(timeseriesDatasetFiles.getCsvTempPath()).toFile();
        File outputFinalCsv = destination.resolve(timeseriesDatasetFiles.getCsvPath()).toFile();
        Files.createDirectories(outputTempCsv.toPath().getParent());
        logDebug("Generate CSV").addParameter("outputTempCsv", outputTempCsv.getName()).log();

        try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(inputCsv), Charset.forName("UTF8")), ',')) {
            try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(outputTempCsv), Charset.forName("UTF8")), ',')) {

                String[] strings = reader.readNext();

                while (strings != null) {

//                    if (rowIndex == 0) { // the row with all the titles in
//                        // set the updated titles
//                        for (TimeseriesUpdateCommand command : commandsForThisDataset) {
//                            Integer index = command.datasetCsvColumn.get(timeseriesDatasetFiles.getCsdbId());
//                            if (index != null) {
//                                logDebug("Generating CSV")
//                                        .addParameter("fromTitle", strings[index])
//                                        .addParameter("toTitle", command.title)
//                                        .addParameter("index", index)
//                                        .addParameter("csdb", command.cdid)
//                                        .addParameter("CSDB", timeseriesDatasetFiles.getCsdbId())
//                                        .log();
//                                strings[index] = command.title;
//                            }
//                        }
//                    }

                    if (rowIndex == 2) { // the row with all the titles in
                        // set the updated titles
                        for (TimeseriesUpdateCommand command : commandsForThisDataset) {
                            Integer index = command.datasetCsvColumn.get(timeseriesDatasetFiles.getCsdbId());
                            if (index != null) {
                                strings[index] = command.preunit;
                            }
                        }
                    }
                    if (rowIndex == 3) { // the row with all the titles in
                        // set the updated titles
                        for (TimeseriesUpdateCommand command : commandsForThisDataset) {
                            Integer index = command.datasetCsvColumn.get(timeseriesDatasetFiles.getCsdbId());
                            if (index != null) {
                                strings[index] = command.unit;
                            }
                        }
                    }
//                    if (rowIndex == 4) { // the row with all the release dates in
//                        // set the updated titles
//                        for (TimeseriesUpdateCommand command : commandsForThisDataset) {
//                            Integer index = command.datasetCsvColumn.get(timeseriesDatasetDownloads.getCsdbId());
//                            if (index != null) {
//                                System.out.println("Setting CSV date from " + strings[index] +
//                                        " to: " + command.releaseDate +
//                                        " index: " + index +
//                                        " cdid: " + command.cdid +
//                                        " CSDB: " + timeseriesDatasetDownloads.getCsdbId());
//                                strings[index] = outputDateFormat.format(command.releaseDate);
//                            }
//                        }
//                    }

                    writer.writeNext(strings);
                    strings = reader.readNext();
                    rowIndex++;
                }
            }
        }

        Files.move(outputTempCsv.toPath(), outputFinalCsv.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void populateCsvColumnIndexesToUpdate(TimeseriesDatasetFiles timeseriesDatasetFiles, Set<TimeseriesUpdateCommand> commandsForThisDataset, Path source) throws IOException {
        File inputCsv = source.resolve(timeseriesDatasetFiles.getCsvPath()).toFile();
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
                        command.datasetCsvColumn.put(timeseriesDatasetFiles.getCsdbId(), columnIndex);
                    }
                }

                columnIndex++;
            }
        }
    }

    public static Set<TimeseriesUpdateCommand> getCommandsForDataset(ArrayList<TimeseriesUpdateCommand> updateCommands, TimeseriesDatasetFiles timeseriesDatasetFiles) {
        Set<TimeseriesUpdateCommand> commandsForThisDataset = new HashSet<>();
        for (TimeseriesUpdateCommand updateCommand : updateCommands) {

            if (updateCommand.dataset != null) {
                if (updateCommand.dataset.equalsIgnoreCase(timeseriesDatasetFiles.getCsdbId())) {
                    logDebug("UpdateCommand added to dataset")
                            .addParameter("cdid", updateCommand.cdid)
                            .addParameter("csdbid", timeseriesDatasetFiles.getCsdbId())
                            .addParameter("url", updateCommand.uri)
                            .log();
                    commandsForThisDataset.add(updateCommand);
                }
            } else {
                logDebug("No source datasets defined for CDID").addParameter("cdid", updateCommand.cdid).log();
                commandsForThisDataset.add(updateCommand);
            }

        }
        return commandsForThisDataset;
    }

    public static Set<TimeseriesDatasetFiles> determineWhatDownloadsNeedUpdating(ArrayList<TimeseriesUpdateCommand> updateCommands, List<TimeseriesDatasetFiles> datasetDownloads) {
        Set<TimeseriesDatasetFiles> datasetDownloadsToUpdate = new HashSet<>();
        for (TimeseriesUpdateCommand command : updateCommands) {

            for (TimeseriesDatasetFiles datasetDownload : datasetDownloads) {
                if (command.dataset.equalsIgnoreCase(datasetDownload.getCsdbId())) {
                    datasetDownloadsToUpdate.add(datasetDownload);
                }
            }

        }
        return datasetDownloadsToUpdate;
    }

    public static List<TimeseriesDatasetFiles> findCsdbFiles(Path source) {
        CsdbFinder csdbFinder = new CsdbFinder();
        csdbFinder.find(source);
        List<TimeseriesDatasetFiles> datasetDownloads = new ArrayList<>();
        for (String uri : csdbFinder.uris) {
            datasetDownloads.add(new TimeseriesDatasetFiles(Paths.get(uri)));
        }
        return datasetDownloads;
    }
}
