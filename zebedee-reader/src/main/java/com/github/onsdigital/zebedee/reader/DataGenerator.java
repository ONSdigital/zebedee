package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.zebedee.content.json.ChartObj;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import org.apache.commons.lang3.StringUtils;

import au.com.bytecode.opencsv.CSVWriter;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.probeContentType;
import static java.nio.file.Files.size;

/**
 * Created by thomasridd on 07/10/15.
 */
public class DataGenerator {

    /**
     * Get data generated from a resource corresponding to a chart file
     *
     * @param resource
     * @param format csv/xls/xlsx
     * @return
     * @throws IOException
     */
    public Resource generateData(Resource resource, String format) throws IOException {
        // For now we are going to assume that all generated data from a resource is chart data
        return generateChartData(resource, format);
    }

    /**
     * Get data generated from a timeseries
     *
     * @param timeSeries
     * @param format csv/xls/xlsx
     * @return
     */
    public Resource generateData(TimeSeries timeSeries, String format) throws IOException {
        return generateTimeseriesData(timeSeries, format);
    }

    /**
     * Get data for a list of timeseries
     *
     * Currently future functionality
     *
     * @param timeSerieses
     * @param format
     * @return
     */
    public Resource generateData(List<TimeSeries> timeSerieses, String format) throws IOException {
        return generateTimeseriesData(timeSerieses, format);
    }

    /**
     * Build the data grid for a chart object
     *
     * @param resource a chart resource file
     * @param format csv/xls/xlsx
     * @return
     * @throws IOException
     */
    Resource generateChartData(Resource resource, String format) throws IOException {

        Path filePath = Files.createTempFile("chart", "." + format);
        try(InputStream stream = resource.getData()) {

            ChartObj chartObj = ContentUtil.deserialise(stream, ChartObj.class);

            List<List<String>> grid = chartDataGrid(chartObj);

            if (format.equalsIgnoreCase("xls")) {
                writeDataGridToXls(filePath, grid);
            } else if (format.equalsIgnoreCase("xlsx")) {
                writeDataGridToXlsx(filePath, grid);
            } else if (format.equalsIgnoreCase("csv")) {
                writeDataGridToCsv(filePath, grid);
            }
        }
        return buildResource(filePath);
    }

    /**
     * Build a resource from the path given
     *
     * @param path
     * @return
     * @throws IOException
     */
    protected Resource buildResource(Path path) throws IOException {
        Resource resource = new Resource();
        resource.setName(path.getFileName().toString());
        resource.setMimeType(probeContentType(path));
        resource.setData(newInputStream(path));
        resource.setSize(size(path));
        return resource;
    }

    /**
     * Output a grid of strings to XLSX
     * ( Excel format for generator is currently xls )
     *
     * @param xlsPath
     * @param grid
     * @throws IOException
     */
    static void writeDataGridToXlsx(Path xlsPath, List<List<String>> grid) throws IOException {

        Workbook wb = new SXSSFWorkbook(30);

        Sheet sheet = wb.createSheet("data");

        int rownum = 0;
        for (List<String> gridRow : grid) {
            Row row = sheet.createRow(rownum++);

            int colnum = 0;
            for (String gridCell : gridRow) {
                row.createCell(colnum++).setCellValue(gridCell);
            }
        }

        try (OutputStream stream = Files.newOutputStream(xlsPath)) {
            wb.write(stream);
        }
    }

    /**
     * Output a grid of strings to XLS
     *
     * @param xlsPath
     * @param grid
     * @throws IOException
     */
    static void writeDataGridToXls(Path xlsPath, List<List<String>> grid) throws IOException {

        Workbook wb = new HSSFWorkbook();

        Sheet sheet = wb.createSheet("data");

        int rownum = 0;
        for (List<String> gridRow : grid) {
            Row row = sheet.createRow(rownum++);

            int colnum = 0;
            for (String gridCell : gridRow) {
                row.createCell(colnum++).setCellValue(gridCell);
            }
        }

        try (OutputStream stream = Files.newOutputStream(xlsPath)) {
            wb.write(stream);
        }
    }

    /**
     * Output a grid of strings to CSV
     *
     * @param csvPath path to write to
     * @param grid    grid to output to
     * @throws IOException
     */
    static void writeDataGridToCsv(Path csvPath, List<List<String>> grid) throws IOException {
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(Files.newOutputStream(csvPath), Charset.forName("UTF8")), ',')) {
            for (List<String> gridRow : grid) {
                String[] row = new String[gridRow.size()];
                row = gridRow.toArray(row);
                writer.writeNext(row);
            }
        }
    }

    /**
     * Get chart data as a grid that can be added
     *
     * @param chartObj a chart object
     * @return a grid of data
     */
    List<List<String>> chartDataGrid(ChartObj chartObj) {
        List<List<String>> grid = new ArrayList<>();
        grid.add( rowFromPair(chartObj.title, "") );
        grid.add( rowFromPair(chartObj.subtitle, "") );
        grid.add( rowFromPair("", ""));
        grid.add( rowFromPair("Notes", chartObj.notes) );
        grid.add( rowFromPair("Unit", chartObj.unit) );
        grid.add( rowFromPair("", ""));

        grid.add( chartObj.headers );
        for (Map<String, String> point: chartObj.data) {
            grid.add( rowFromMap(chartObj.headers, point));
        }

        return grid;
    }

    List<String> rowFromPair(String cell1, String cell2) {
        List<String> row = new ArrayList<>();
        row.add(cell1);
        row.add(cell2);
        return row;
    }
    List<String> rowFromMap(List<String> keys, Map<String, String> data) {
        List<String> row = new ArrayList<>();
        for (String key: keys) {
            if (data.containsKey(key)) {
                row.add(data.get(key));
            } else {
                row.add("");
            }
        }
        return row;
    }




    /*
     T I M E S E R I E S
     */

    /**
     * Generate a file for multiple time series
     *
     * @param series
     * @param format
     * @return
     * @throws IOException
     */
    Resource generateTimeseriesData(List<TimeSeries> series, String format) throws IOException {

        Path filePath = Files.createTempFile("data", "." + format);

        List<List<String>> grid = timeSeriesDataGrid(series);

        if (format.equalsIgnoreCase("xls")) {
            writeDataGridToXls(filePath, grid);
        } else if (format.equalsIgnoreCase("xlsx")) {
            writeDataGridToXls(filePath, grid);
        } else if (format.equalsIgnoreCase("csv")) {
            writeDataGridToXls(filePath, grid);
        }

        return buildResource(filePath);
    }

    /**
     * Generate a file for a single time series
     *
     * @param series
     * @param format
     * @return
     * @throws IOException
     */
    Resource generateTimeseriesData(TimeSeries series, String format) throws IOException {
        List<TimeSeries> serieses = new ArrayList<>();
        serieses.add(series);

        return generateTimeseriesData(serieses, format);
    }


    /**
     * Get a data grid for multiple time series
     *
     * Lots of
     *
     * @param serieses
     * @return
     */
    static List<List<String>> timeSeriesDataGrid(List<TimeSeries> serieses) {
        List<List<String>> rows = new ArrayList<>();

        // Initialise the grid columns
        List<String> orderedCDIDs = timeSeriesIdList(serieses);

        // Initialise the grid rows
        Map<String, Map<String, String>> mapOfData = mapOfAllDataInTimeSeriesList(serieses);

        // Add the basic details header rows
        addTimeSeriesDetails(rows, orderedCDIDs, mapOfData);

        // Add years
        List<String> yearRange = yearRange(serieses);
        if (yearRange != null) {
            for (String year : yearRange) {
                List<String> newRow = new ArrayList<>();
                newRow.add(year);
                for (String cdid : orderedCDIDs) {
                    newRow.add(mapOfData.get(year).get(cdid));
                }
                rows.add(newRow);
            }
        }

        // Add quarters
        List<String> quarterRange = quarterRange(serieses);
        if (quarterRange != null) {
            for (String quarter : quarterRange) {
                List<String> newRow = new ArrayList<>();
                newRow.add(quarter);
                for (String cdid : orderedCDIDs) {
                    newRow.add(mapOfData.get(quarter).get(cdid));
                }
                rows.add(newRow);
            }
        }

        // Add months
        List<String> monthRange = monthRange(serieses);
        if (monthRange != null) {
            for (String month : monthRange) {
                List<String> newRow = new ArrayList<>();
                newRow.add(month);
                for (String cdid : orderedCDIDs) {
                    newRow.add(mapOfData.get(month).get(cdid));
                }
                rows.add(newRow);
            }
        }

        return rows;
    }

    static void addTimeSeriesDetails(List<List<String>> rows, List<String> orderedCDIDs, Map<String, Map<String, String>> mapOfData) {
        // Add detail rows
        List<String> titleRow = newRow("Title");
        List<String> cdidRow = newRow("CDID");
        List<String> nationalStatistic = newRow("National Statistic");
        List<String> seasonallyAdjusted = newRow("Seasonally Adjusted");
        List<String> preunit = newRow("PreUnit");
        List<String> unit = newRow("Unit");
        List<String> releaseDate = newRow("Release date");
        List<String> nextRelease = newRow("Next release");
        List<String> importantNotes = newRow("Important notes");

        // Write details for each cdid
        for (String cdid : orderedCDIDs) {
            titleRow.add(mapOfData.get("Title").get(cdid));
            cdidRow.add(cdid);
            nationalStatistic.add(mapOfData.get("National Statistic").get(cdid));
            seasonallyAdjusted.add(mapOfData.get("Seasonally Adjusted").get(cdid));
            preunit.add(mapOfData.get("PreUnit").get(cdid));
            unit.add(mapOfData.get("Unit").get(cdid));
            releaseDate.add(mapOfData.get("Release date").get(cdid));
            nextRelease.add(mapOfData.get("Next release").get(cdid));
            importantNotes.add(mapOfData.get("Important notes").get(cdid));
        }

        rows.add(titleRow);
        rows.add(cdidRow);
        rows.add(nationalStatistic);
        rows.add(seasonallyAdjusted);
        rows.add(preunit);
        rows.add(unit);
        rows.add(releaseDate);
        rows.add(nextRelease);
        rows.add(importantNotes);
    }

    /**
     * Add a new row to a map with its name as the first field
     *
     * @param rowName
     * @return
     */
    static List<String> newRow(String rowName) {
        List<String> result = new ArrayList<>();
        result.add(rowName);
        return result;
    }

    /**
     * Get a map of maps containing all data so that map.get(CDID).get(TIME) gives value
     *
     * @param serieses
     * @return a Map of Maps as described above
     */
    static Map<String, Map<String, String>> mapOfAllDataInTimeSeriesList(List<TimeSeries> serieses) {
        HashMap<String, Map<String, String>> map = new HashMap<>();

        for (TimeSeries series : serieses) {
            putCombination(series.getCdid(), "Title", series.getDescription().getTitle(), map);
            putCombination(series.getCdid(), "CDID", series.getDescription().getCdid(), map);
            putCombination(series.getCdid(), "National Statistic", (series.getDescription().isNationalStatistic() ? "Y" : "N"), map);
            putCombination(series.getCdid(), "Seasonally Adjusted", (series.getDescription().getSeasonalAdjustment()), map);
            putCombination(series.getCdid(), "PreUnit", series.getDescription().getPreUnit(), map);
            putCombination(series.getCdid(), "Unit", series.getDescription().getUnit(), map);


            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
            if (series.getDescription().getReleaseDate() == null) {
                putCombination(series.getCdid(), "Release date", "", map);
            } else {
                putCombination(series.getCdid(), "Release date", format.format(series.getDescription().getReleaseDate()), map);
            }

            putCombination(series.getCdid(), "Next release", series.getDescription().getNextRelease(), map);

            putCombination(series.getCdid(), "Important notes", StringUtils.join(series.getNotes(), ", "), map);

            if (series.years != null) {
                for (TimeSeriesValue value : series.years) {
                    putCombination(series.getCdid(), value.date, value.value, map);
                }
            }
            if (series.months != null) {
                for (TimeSeriesValue value : series.months) {
                    putCombination(series.getCdid(), value.date, value.value, map);
                }
            }
            if (series.quarters != null) {
                for (TimeSeriesValue value : series.quarters) {
                    putCombination(series.getCdid(), value.date, value.value, map);
                }
            }
        }

        return map;
    }

    /**
     * Get an ordered list of years that ought to be written on a spreadsheet
     *
     * Correctly orders and fills holes for the list
     *
     * @param seriesList
     * @return
     */
    static List<String> yearRange(List<TimeSeries> seriesList) {
        TimeSeriesValue min = null;
        TimeSeriesValue max = null;
        for (TimeSeries series : seriesList) {
            for (TimeSeriesValue value : series.years) {
                if (min == null || min.compareTo(value) > 0) {
                    min = value;
                }
                if (max == null || max.compareTo(value) < 0) {
                    max = value;
                }
            }
        }

        if (min == null) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(min.toDate());
        int minYear = cal.get(Calendar.YEAR);
        cal.setTime(max.toDate());
        int maxYear = cal.get(Calendar.YEAR);

        List<String> yearLabels = new ArrayList<>();
        for (int i = minYear; i <= maxYear; i++) {
            yearLabels.add(i + "");
        }

        return yearLabels;
    }

    /**
     * Get an ordered list of quarters that ought to be written on a spreadsheet
     *
     * Correctly orders and fills holes for the list
     *
     * @param seriesList
     * @return
     */
    static List<String> quarterRange(List<TimeSeries> seriesList) {
        TimeSeriesValue min = null;
        TimeSeriesValue max = null;
        for (TimeSeries series : seriesList) {
            for (TimeSeriesValue value : series.quarters) {
                if (min == null || min.compareTo(value) > 0) {
                    min = value;
                }
                if (max == null || max.compareTo(value) < 0) {
                    max = value;
                }
            }
        }

        if (min == null) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(min.toDate());
        int minYear = cal.get(Calendar.YEAR);
        int minQuarter = cal.get(Calendar.MONTH) / 3;

        cal.setTime(max.toDate());
        int maxYear = cal.get(Calendar.YEAR);
        int maxQuarter = cal.get(Calendar.MONTH) / 3;

        String[] quarters = "Q1,Q2,Q3,Q4".split(",");

        List<String> quarterLabels = new ArrayList<>();

        for (int i = minYear; i <= maxYear; i++) {
            for (int q = 0; q < 4; q++) {
                if (i == minYear) {
                    if (q >= minQuarter) {
                        quarterLabels.add(i + " " + quarters[q]);
                    }
                } else if (i == maxYear) {
                    if (q <= maxQuarter) {
                        quarterLabels.add(i + " " + quarters[q]);
                    }
                } else {
                    quarterLabels.add(i + " " + quarters[q]);
                }
            }
        }

        return quarterLabels;
    }

    /**
     * Get an ordered list of months that ought to be written on a spreadsheet
     *
     * Correctly orders and fills holes for the list
     *
     * @param seriesList
     * @return
     */
    static List<String> monthRange(List<TimeSeries> seriesList) {
        TimeSeriesValue min = null;
        TimeSeriesValue max = null;
        for (TimeSeries series : seriesList) {
            for (TimeSeriesValue value : series.months) {
                if (min == null || min.compareTo(value) > 0) {
                    min = value;
                }
                if (max == null || max.compareTo(value) < 0) {
                    max = value;
                }
            }
        }

        if (min == null) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(min.toDate());
        int minYear = cal.get(Calendar.YEAR);
        int minMonth = cal.get(Calendar.MONTH);

        cal.setTime(max.toDate());
        int maxYear = cal.get(Calendar.YEAR);
        int maxMonth = cal.get(Calendar.MONTH);

        String[] months = "JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC".split(",");

        List<String> monthLabels = new ArrayList<>();

        for (int i = minYear; i <= maxYear; i++) {
            for (int q = 0; q < 12; q++) {
                if (i == minYear) {
                    if (q >= minMonth) {
                        monthLabels.add(i + " " + months[q]);
                    }
                } else if (i == maxYear) {
                    if (q <= maxMonth) {
                        monthLabels.add(i + " " + months[q]);
                    }
                } else {
                    monthLabels.add(i + " " + months[q]);
                }
            }
        }

        return monthLabels;
    }

    /**
     * get a list of series CDIDs
     *
     * This is used to define headings for our spreadsheet
     *
     * @param serieses
     * @return
     */
    static List<String> timeSeriesIdList(List<TimeSeries> serieses) {
        List<String> ids = new ArrayList<>();
        for (TimeSeries series : serieses) {
            ids.add(series.getCdid());
        }
        return ids;
    }

    /**
     * Write a data point to our collated timeseries map of maps
     *
     * @param cdid
     * @param row
     * @param value
     * @param map
     */
    private static void putCombination(String cdid, String row, String value, Map<String, Map<String, String>> map) {
        Map<String, String> submap = new HashMap<>();
        if (map.containsKey(row)) {
            submap = map.get(row);
        }

        submap.put(cdid, value);
        map.put(row, submap);
    }


}
