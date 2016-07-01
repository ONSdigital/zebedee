package com.github.onsdigital.zebedee.data.processing;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logError;

public class TimeseriesDataFix {


    public static void main(String[] args) throws IOException, OpenXML4JException, SAXException, InterruptedException {

        if (args == null || args.length < 1) {
            logDebug("Please provide a root path to apply the data fix.").log();
            return;
        }

        String inputPath = args[0];
        logDebug("Applying timeSeries data fix").path(Paths.get(inputPath)).log();

        ContentReader contentReader = new FileSystemContentReader(Paths.get(inputPath));
        DataIndex dataIndex = new DataIndex(contentReader);

        logDebug("Data indexing").log();

        CsdbFinder csdbFinder = new CsdbFinder();
        List<Path> csdbPaths = csdbFinder.find(Paths.get(inputPath));

        while (!dataIndex.indexBuilt) {
            Thread.sleep(1000);
            System.out.print(".");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

        for (Path csdbPath : csdbPaths) {

            try {

                int count = 0;

                logDebug("Processing csdb").path(csdbPath)
                        .addParameter("lastModified", sdf.format(csdbPath.toFile().lastModified()))
                        .log();

                DataLinkBrian brian = new DataLinkBrian();
                TimeSerieses timeSeries = brian.getTimeSeries(brian.csdbURI(), Files.newInputStream(csdbPath), csdbPath.getFileName().toString());

                for (TimeSeries series : timeSeries) {

                    try {
                        // Build new timeseries
                        DataProcessor processor = new DataProcessor();
                        String publishUri = processor.getDatasetBasedUriForTimeseries(series, null, dataIndex);

                        // Try to get an existing timeseries
                        try {
                            TimeSeries existing = (TimeSeries) contentReader.getContent(publishUri);

                            // check if the title has been updated.
                            if (!series.getDescription().getTitle().equals(existing.getDescription().getTitle())) {
                                //Log.print("The title for timeseries %s updated to %s", existing.getDescription().getTitle(), series.getDescription().getTitle());
                                count++;
                            }
                        } catch (ZebedeeException e) {
                            logError(e, "Error while applying timeseries data fix");
                        }

                    } catch (Exception e) {
                        logError(e, "Error while applying timeseries data fix");
                    }
                }

                logDebug("Timeseries data fix processing completed")
                        .addParameter("csdb", csdbPath.toString())
                        .path(csdbPath)
                        .addParameter("numberOfChanges", count)
                        .log();

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public static void removeSeasonallyAdjusted(String inputPath) throws IOException, OpenXML4JException, SAXException {
        TimeseriesFinder finder = new TimeseriesFinder();
        List<Path> timeseries = finder.findTimeseries(Paths.get(inputPath));
        logDebug("Processing timeseries").addParameter("size", timeseries.size()).log();

        for (Path timeseriesPath : timeseries) {
            //removeSeasonalAdjustmentValue(timeseriesPath);
        }

        CsdbFinder csdbFinder = new CsdbFinder();
        List<Path> csdbPaths = csdbFinder.find(Paths.get(inputPath));
        logDebug("Found CSDB files").addParameter("numberOfFiles", csdbPaths.size()).log();

        for (Path csdbPath : csdbPaths) {

            try {
                // determine xls path
                Path csvPath = csdbPath.getParent().resolve(FilenameUtils.getBaseName(csdbPath.toString()) + ".csv");
                logDebug("Updating csv").path(csvPath).log();
                Path outputCsvPath = csdbPath.getParent().resolve(FilenameUtils.getBaseName(csdbPath.toString()) + "_updated.csv");
                updateCsv(csvPath, outputCsvPath);
                Files.move(outputCsvPath, csvPath, StandardCopyOption.REPLACE_EXISTING);

                Path xlsPath = csdbPath.getParent().resolve(FilenameUtils.getBaseName(csdbPath.toString()) + ".xlsx");
                logDebug("Updating xls").path(xlsPath).log();
                Path outputXlsxPath = xlsPath.getParent().resolve(FilenameUtils.getBaseName(csdbPath.toString()) + "_updated.xlsx");
                updateXlsx(csvPath, outputXlsxPath);
                Files.move(outputXlsxPath, xlsPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (FileNotFoundException ex) {
                logError(ex, "Files not found for CSDB file").path(csdbPath).log();
            }
        }
    }

    // read an existing CSV file and output to XLSX
    private static void updateXlsx(Path inputPath, Path updatedPath) throws IOException, OpenXML4JException, SAXException {

        try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(inputPath.toFile()), Charset.forName("UTF8")), ',')) {

            Workbook wb = new SXSSFWorkbook(30);
            Sheet sheet = wb.createSheet("data");

            int rownum = 0;
            String[] strings = reader.readNext();

            while (strings != null) {

                if (!strings[0].equals("Seasonally Adjusted")) {
                    Row row = sheet.createRow(rownum++);

                    int colnum = 0;
                    for (String gridCell : strings) {
                        row.createCell(colnum).setCellValue(gridCell);
                        colnum++;
                    }
                }

                strings = reader.readNext();
            }

            try (OutputStream stream = new FileOutputStream(updatedPath.toFile())) {
                wb.write(stream);
            }
        }
    }

    private static void updateCsv(Path inputPath, Path outputPath) throws IOException {

        try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(inputPath.toFile()), Charset.forName("UTF8")), ',')) {
            try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(outputPath.toFile()), Charset.forName("UTF8")), ',')) {

                String[] strings = reader.readNext();

                while (strings != null) {

                    if (!strings[0].equals("Seasonally Adjusted")) {
                        writer.writeNext(strings);
                    }

                    strings = reader.readNext();
                }
            }
        }
    }
}
