package com.github.onsdigital.zebedee.reader;

import au.com.bytecode.opencsv.CSVWriter;
import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.dynamic.timeseries.Point;
import com.github.onsdigital.zebedee.content.dynamic.timeseries.Series;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.chart.Chart;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnexpectedErrorException;
import com.github.onsdigital.zebedee.reader.util.factory.CSVWriterFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logDebug;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING;

/**
 * Created by thomasridd on 07/10/15.
 */
public class DataGenerator {

    // The date format including the BST timezone. Dates are stored at UTC and must be formated to take BST into account.
    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("dd-MM-yyyy", TimeZone.getTimeZone("Europe/London"));

    /**
     * Regex to match a decimal number
     */
    private static final Pattern DECIMAL_REGEX = Pattern.compile("^-?\\d+\\.\\d+$");
    private static final String[] MONTHS = "JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC".split(",");
    private static final String[] QUARTERS = "Q1,Q2,Q3,Q4".split(",");
    private static final String BASE_FORMAT = "0.";
    private static final String DECIMAL_PLACEHOLDER = "0";
    private static final String UTF8 = "UTF8";
    private static final String MIME_TYPE = "application/octet-stream";
    private static final int METADATA_ROWS = 8;
    private static DateFormat FILE_NAME_DATE = new SimpleDateFormat("ddMMyy");

    static final String UNSUPPORTED_CONTENT_TYPE_MSG = "Cannot generate dowload data for provided Content type";
    static final String UNSUPPORTED_FORMAT_MSG = "Requested format is not currently supported.";
    static final String XLS_EXT = "xls";
    static final String XLSX_EXT = "xlsx";
    static final String CSV_EXT = "csv";
    static final String SHEET_NAME = "data";
    static final String TITLE_COL = "Title";
    static final String CDID_COL = "CDID";
    static final String SOURCE_DATASET_COL = "Source dataset ID";
    static final String PRE_UNIT_COL = "PreUnit";
    static final String UNIT_COL = "Unit";
    static final String RELEASE_DATE_COL = "Release date";
    static final String NEXT_RELEASE_COL = "Next release";
    static final String NOTES_COL = "Important notes";
    static final String SERIES_NAME = "series";
    static final String CHART_NAME = "chart";

    private Supplier<Workbook> xlsWorkbookSupplier = () -> new HSSFWorkbook();
    private Supplier<Workbook> xlsxWorkbookSupplier = () -> new SXSSFWorkbook(30);
    private CSVWriterFactory csvWriterFactory = (writer, separator) -> new CSVWriter(writer, separator);

    /**
     * Generate download data for the requested content.
     *
     * @param content the content to generate the download {@link Resource} for.
     * @param format  of the download to generate.
     * @return {@link Resource} containing the content of the in the requested format.
     * @throws IOException              problem generating the download data.
     * @throws UnexpectedErrorException
     */
    public Resource generateData(Content content, String format) throws IOException, BadRequestException {
        if (content instanceof Chart) {
            return generateChartData((Chart) content, format);
        }
        if (content instanceof TimeSeries) {
            return generateTimeseriesData((TimeSeries) content, format);
        }
        if (content instanceof Series) {
            return generateSeriesData((Series) content, format);
        }
        logDebug(UNSUPPORTED_CONTENT_TYPE_MSG)
                .addParameter("class", content.getClass().getSimpleName())
                .log();
        throw new BadRequestException(UNSUPPORTED_CONTENT_TYPE_MSG);
    }


    Resource generateTimeseriesData(List<TimeSeries> series, String format) throws IOException, BadRequestException {
        List<List<String>> grid = timeSeriesDataGrid(series);
        String filename = new StringBuilder(SERIES_NAME)
                .append("-")
                .append(FILE_NAME_DATE.format(new Date()))
                .append(".")
                .append(format)
                .toString();
        return generateResourceFromDataGrid(grid, filename);
    }


    Resource generateSeriesData(Series series, String format) throws IOException, BadRequestException {
        List<List<String>> grid = generateSeriesGrid(series);
        String filename = new StringBuilder(series.getDescription().getCdid())
                .append("-")
                .append(FILE_NAME_DATE.format(new Date()))
                .append(".")
                .append(format)
                .toString();
        return generateResourceFromDataGrid(grid, filename);
    }

    /**
     * @param chart
     * @param format
     * @return
     * @throws IOException
     * @throws UnexpectedErrorException
     */
    Resource generateChartData(Chart chart, String format) throws IOException, BadRequestException {
        List<List<String>> grid = chartDataGrid(chart);
        String filename = new StringBuilder(chart.getTitle().replace(" ", "_"))
                .append(".")
                .append(format)
                .toString();
        return generateResourceFromDataGrid(grid, filename);
    }

    /**
     * @param grid
     * @param fileName
     * @return
     * @throws IOException
     * @throws UnexpectedErrorException
     */
    Resource generateResourceFromDataGrid(List<List<String>> grid, String fileName) throws IOException,
            BadRequestException {
        switch (getExtension(fileName)) {
            case XLS_EXT:
                return createResource(fileName, workbookToBytes(grid, xlsWorkbookSupplier));
            case XLSX_EXT:
                return createResource(fileName, workbookToBytes(grid, xlsxWorkbookSupplier));
            case CSV_EXT:
                return createResource(fileName, csvToBytes(grid));
            default:
                logDebug(UNSUPPORTED_FORMAT_MSG)
                        .addParameter("format", getExtension(fileName))
                        .log();
                throw new BadRequestException(UNSUPPORTED_FORMAT_MSG);
        }
    }

    Resource generateTimeseriesData(TimeSeries series, String format) throws IOException, BadRequestException {
        List<TimeSeries> serieses = new ArrayList<>();
        serieses.add(series);
        return generateTimeseriesData(serieses, format);
    }

    /**
     * @param timeSerieses
     * @param format
     * @return
     * @throws IOException
     * @throws UnexpectedErrorException
     */
    public Resource generateData(List<TimeSeries> timeSerieses, String format) throws IOException, BadRequestException {
        return generateTimeseriesData(timeSerieses, format);
    }

    byte[] workbookToBytes(List<List<String>> grid, Supplier<Workbook> workbookSupplier)
            throws IOException {
        try (
                Workbook wb = workbookSupplier.get();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ) {
            Sheet sheet = wb.createSheet(SHEET_NAME);
            int rowIndex = 0;
            for (List<String> gridRow : grid) {

                Row r = sheet.createRow(rowIndex++);
                int columnIndex = 0;
                for (String cellValueStr : gridRow) {
                    Cell cell = r.createCell(columnIndex);

                    if (CELL_TYPE_NUMERIC == determineCellType(rowIndex, columnIndex, cellValueStr)) {
                        if (DECIMAL_REGEX.matcher(cellValueStr).matches()) {
                            // Little bit nasty but even with the cell type set as numeric adding a value where
                            // the decimal value is 0 it will remove the decimal value displaying it as an int
                            // not a float. Example '55.0' will be displayed as '55'.
                            // To combat this we create a custom data format for each string value to force it to
                            // display the decimal places even if they are all zero.
                            CellStyle style = wb.createCellStyle();
                            style.setDataFormat(wb.createDataFormat().getFormat(getDataFormat(cellValueStr)));
                            cell.setCellStyle(style);
                        }
                        cell.setCellType(CELL_TYPE_NUMERIC);
                        cell.setCellValue(Double.parseDouble(cellValueStr));
                    } else {
                        cell.setCellType(CELL_TYPE_STRING);
                        cell.setCellValue(cellValueStr);
                    }
                    columnIndex++;
                }
            }
            wb.write(baos);
            byte[] wordbookBytes = baos.toByteArray();
            return wordbookBytes;
        }
    }

    /**
     * Output a grid of strings to CSV
     *
     * @param csvPath path to write to
     * @param grid    grid to output to
     * @throws IOException
     */
    byte[] csvToBytes(List<List<String>> grid) throws IOException {
        try (
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(baos, Charset.forName(UTF8));
                CSVWriter writer = csvWriterFactory.getCSVWriter(outputStreamWriter, ',');
        ) {
            for (List<String> gridRow : grid) {
                String[] row = gridRow.toArray(new String[gridRow.size()]);
                writer.writeNext(row);
            }
            writer.flush();
            byte[] csvBytes = baos.toByteArray();
            return csvBytes;
        }
    }

    private String getDataFormat(String cellValueStr) {
        String[] segments = cellValueStr.split("\\.");
        String decimals = segments[segments.length - 1];

        StringBuilder format = new StringBuilder(BASE_FORMAT);
        asList(decimals.split(""))
                .stream()
                .forEach(decimalValue -> format.append(DECIMAL_PLACEHOLDER));
        return format.toString();
    }

    private int determineCellType(int rowIndex, int cellIndex, String callValue) {
        if (rowIndex <= METADATA_ROWS || cellIndex == 0 || StringUtils.isEmpty(callValue)) {
            return Cell.CELL_TYPE_STRING;
        }
        try {
            Float.parseFloat(callValue);
        } catch (NumberFormatException e) {
            logDebug("XLS Cell value could not be parsed to Float, value will be written as String.")
                    .addParameter("nonNumericValue", callValue)
                    .log();
            return CELL_TYPE_STRING;
        }
        return CELL_TYPE_NUMERIC;
    }


    /**
     * Get a data grid for multiple time series
     * <p>
     * Lots of
     *
     * @param serieses
     * @return
     */
    private List<List<String>> timeSeriesDataGrid(List<TimeSeries> serieses) {
        List<List<String>> rows = new ArrayList<>();

        // Initialise the grid columns
        List<String> timeseriesUrls = timeSeriesIdList(serieses);

        // Initialise the grid rows
        Map<String, Map<String, String>> mapOfData = mapOfAllDataInTimeSeriesList(serieses);

        // Add the basic details header rows
        addTimeSeriesDetails(rows, timeseriesUrls, mapOfData);

        // Add years
        List<String> yearRange = yearRange(serieses);
        if (yearRange != null) {
            for (String year : yearRange) {
                List<String> newRow = new ArrayList<>();
                newRow.add(year);
                for (String url : timeseriesUrls) {
                    newRow.add(mapOfData.get(year).get(url));
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
                for (String url : timeseriesUrls) {
                    newRow.add(mapOfData.get(quarter).get(url));
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
                for (String url : timeseriesUrls) {
                    newRow.add(mapOfData.get(month).get(url));
                }
                rows.add(newRow);
            }
        }

        return rows;
    }

    private void addTimeSeriesDetails(List<List<String>> rows, List<String> timeseriesId, Map<String, Map<String,
            String>> mapOfData) {
        // Add detail rows
        List<String> titleRow = newRow(TITLE_COL);
        List<String> cdidRow = newRow(CDID_COL);
        List<String> datasetIdRow = newRow(SOURCE_DATASET_COL);
        List<String> preunit = newRow(PRE_UNIT_COL);
        List<String> unit = newRow(UNIT_COL);
        List<String> releaseDate = newRow(RELEASE_DATE_COL);
        List<String> nextRelease = newRow(NEXT_RELEASE_COL);
        List<String> importantNotes = newRow(NOTES_COL);

        // Write details for each cdid
        for (String id : timeseriesId) {
            titleRow.add(mapOfData.get(TITLE_COL).get(id));
            cdidRow.add(mapOfData.get(CDID_COL).get(id));
            datasetIdRow.add(mapOfData.get(SOURCE_DATASET_COL).get(id));
            preunit.add(mapOfData.get(PRE_UNIT_COL).get(id));
            unit.add(mapOfData.get(UNIT_COL).get(id));
            releaseDate.add(mapOfData.get(RELEASE_DATE_COL).get(id));
            nextRelease.add(mapOfData.get(NEXT_RELEASE_COL).get(id));
            importantNotes.add(mapOfData.get(NOTES_COL).get(id));
        }

        rows.add(titleRow);
        rows.add(cdidRow);
        rows.add(datasetIdRow);
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
    private List<String> newRow(String rowName) {
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
    private Map<String, Map<String, String>> mapOfAllDataInTimeSeriesList(List<TimeSeries> serieses) {
        HashMap<String, Map<String, String>> map = new HashMap<>();

        for (TimeSeries series : serieses) {
            String seriesIdentifier = series.getUri().toString();

            putCombination(seriesIdentifier, TITLE_COL, series.getDescription().getTitle(), map);
            putCombination(seriesIdentifier, CDID_COL, series.getDescription().getCdid(), map);
            putCombination(seriesIdentifier, SOURCE_DATASET_COL, series.getDescription().getDatasetId(), map);
            putCombination(seriesIdentifier, PRE_UNIT_COL, series.getDescription().getPreUnit(), map);
            putCombination(seriesIdentifier, UNIT_COL, series.getDescription().getUnit(), map);

            if (series.getDescription().getReleaseDate() == null) {
                putCombination(seriesIdentifier, RELEASE_DATE_COL, "", map);
            } else {
                putCombination(seriesIdentifier, RELEASE_DATE_COL, DATE_FORMAT.format(series.getDescription().getReleaseDate()), map);
            }

            putCombination(seriesIdentifier, NEXT_RELEASE_COL, series.getDescription().getNextRelease(), map);
            putCombination(seriesIdentifier, NOTES_COL, StringUtils.join(series.getNotes(), ", "), map);

            if (series.years != null) {
                for (TimeSeriesValue value : series.years) {
                    putCombination(seriesIdentifier, value.date, value.value, map);
                }
            }
            if (series.months != null) {
                for (TimeSeriesValue value : series.months) {
                    putCombination(seriesIdentifier, value.date, value.value, map);
                }
            }
            if (series.quarters != null) {
                for (TimeSeriesValue value : series.quarters) {
                    putCombination(seriesIdentifier, value.date, value.value, map);
                }
            }
        }

        return map;
    }

    /**
     * Get an ordered list of years that ought to be written on a spreadsheet
     * <p>
     * Correctly orders and fills holes for the list
     *
     * @param seriesList
     * @return
     */
    private List<String> yearRange(List<TimeSeries> seriesList) {
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
     * <p>
     * Correctly orders and fills holes for the list
     *
     * @param seriesList
     * @return
     */
    private List<String> quarterRange(List<TimeSeries> seriesList) {
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

        List<String> quarterLabels = new ArrayList<>();

        for (int i = minYear; i <= maxYear; i++) {
            for (int q = 0; q < 4; q++) {
                if (i == minYear) {
                    if (q >= minQuarter) {
                        quarterLabels.add(i + " " + QUARTERS[q]);
                    }
                } else if (i == maxYear) {
                    if (q <= maxQuarter) {
                        quarterLabels.add(i + " " + QUARTERS[q]);
                    }
                } else {
                    quarterLabels.add(i + " " + QUARTERS[q]);
                }
            }
        }

        return quarterLabels;
    }

    /**
     * Get an ordered list of months that ought to be written on a spreadsheet
     * <p>
     * Correctly orders and fills holes for the list
     *
     * @param seriesList
     * @return
     */
    private List<String> monthRange(List<TimeSeries> seriesList) {
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

        List<String> monthLabels = new ArrayList<>();

        for (int i = minYear; i <= maxYear; i++) {
            for (int q = 0; q < 12; q++) {
                if (i == minYear) {
                    if (q >= minMonth) {
                        monthLabels.add(i + " " + MONTHS[q]);
                    }
                } else if (i == maxYear) {
                    if (q <= maxMonth) {
                        monthLabels.add(i + " " + MONTHS[q]);
                    }
                } else {
                    monthLabels.add(i + " " + MONTHS[q]);
                }
            }
        }

        return monthLabels;
    }

    /**
     * get a list of series Urls
     * <p>
     * This is used to define headings for our spreadsheet
     *
     * @param serieses
     * @return
     */
    private List<String> timeSeriesIdList(List<TimeSeries> serieses) {
        List<String> ids = new ArrayList<>();
        for (TimeSeries series : serieses) {
            ids.add(series.getUri().toString());
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
    private void putCombination(String cdid, String row, String value, Map<String, Map<String, String>> map) {
        Map<String, String> submap = new HashMap<>();
        if (map.containsKey(row)) {
            submap = map.get(row);
        }
        submap.put(cdid, value);
        map.put(row, submap);
    }


    private Resource createResource(String filename, byte[] dataBytes) {
        Resource resource = new Resource();
        resource.setName(filename);
        resource.setMimeType(MIME_TYPE);
        resource.setData(new ByteArrayInputStream(dataBytes));
        return resource;
    }

    /**
     * Get chart data as a grid that can be added
     *
     * @param chart a chart object
     * @return a grid of data
     */
    List<List<String>> chartDataGrid(Chart chart) {
        List<List<String>> grid = new ArrayList<>();
        grid.add(rowFromPair(chart.getTitle(), ""));
        grid.add(rowFromPair(chart.getSubtitle(), ""));
        grid.add(rowFromPair("", ""));
        grid.add(rowFromPair("Notes", chart.getNotes()));
        grid.add(rowFromPair("Unit", chart.getUnit()));
        grid.add(rowFromPair("", ""));

        grid.add(chart.getHeaders());
        for (Map<String, String> point : chart.getData()) {
            grid.add(rowFromMap(chart.getHeaders(), point));
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
        for (String key : keys) {
            if (data.containsKey(key)) {
                row.add(data.get(key));
            } else {
                row.add("");
            }
        }
        return row;
    }

    private List<List<String>> generateSeriesGrid(Series series) {
        List<List<String>> grid = new ArrayList<>();

        PageDescription description = series.getDescription();

        grid.add(rowFromPair(TITLE_COL, description.getTitle()));
        grid.add(rowFromPair(CDID_COL, description.getCdid()));
        grid.add(rowFromPair(PRE_UNIT_COL, description.getPreUnit()));
        grid.add(rowFromPair(UNIT_COL, description.getUnit()));

        if (description.getReleaseDate() == null) {
            grid.add(rowFromPair(RELEASE_DATE_COL, ""));
        } else {
            grid.add(rowFromPair(RELEASE_DATE_COL, DATE_FORMAT.format(description.getReleaseDate())));
        }
        grid.add(rowFromPair(NEXT_RELEASE_COL, description.getNextRelease()));

        Set<Point> points = series.getSeries();
        for (Point point : points) {
            String value = point.getStringY();
            grid.add(rowFromPair(point.getName(), value == null ? "" : value));
        }

        return grid;
    }

    void setXLSWorkbookSupplier(Supplier<Workbook> xlsWorkbookSupplier) {
        this.xlsWorkbookSupplier = xlsWorkbookSupplier;
    }

    void setXLSXWorkbookSupplier(Supplier<Workbook> xlsxWorkbookSupplier) {
        this.xlsxWorkbookSupplier = xlsxWorkbookSupplier;
    }

    void setCsvWriterFactory(CSVWriterFactory csvWriterFactory) {
        this.csvWriterFactory = csvWriterFactory;
    }
}
