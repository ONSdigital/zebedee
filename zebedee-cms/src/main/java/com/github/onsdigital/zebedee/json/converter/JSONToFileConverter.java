package com.github.onsdigital.zebedee.json.converter;

import au.com.bytecode.opencsv.CSVWriter;
import com.github.davidcarboni.restolino.json.Serialiser;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by thomasridd on 13/05/15.
 */
public class JSONToFileConverter {

    /**
     *
     *
     * @param request where the input json is coming from
     * @param response where to write the output to
     * @param inputFormat the kind of object being read - currently supported "chart"
     * @param outputFormat the kind of output being requested - currently supported "csv"
     * @throws IOException
     */
    public static void writeRequestJSONToOutputFormat(HttpServletRequest request, HttpServletResponse response, String inputFormat, String outputFormat) throws IOException {

        if(inputFormat.equalsIgnoreCase("chart")){

            ChartObject chartObject = Serialiser.deserialise(request, ChartObject.class);

            if(outputFormat.equalsIgnoreCase("csv")) {
                // Deserialise the chart object
                response.setContentType("application/csv");
                try(OutputStream stream = response.getOutputStream()) {
                    writeChartToCSV(chartObject, stream);
                }
            }
            else if (outputFormat.equalsIgnoreCase("xlsx")) {
                response.setContentType("application/csv");
                try(OutputStream stream = response.getOutputStream()) {
                    writeChartToXLSX(chartObject, stream);
                }
            } else if (outputFormat.equalsIgnoreCase("jsonstat")) {

            } else if (outputFormat.equalsIgnoreCase("json")) {

            }

        } else {

        }
    }

    /**
     * Writes chartObject to output stream as CSV
     *
     * @param chartObject a chart object
     * @param output
     * @throws IOException
     */
    public static void writeChartToCSV(ChartObject chartObject, OutputStream output) throws IOException {

        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(output, Charset.forName("UTF8")), ',')) {


            writeChartTitles(chartObject, writer);      // Column headings
            writeChartData(chartObject, writer);        // Data
            writeBlankLine(writer);                     // Blank line
            writeChartMetadata(chartObject, writer);    // Metadata (title, subtitle, unit, source)

        } catch (Exception e) {
            e.printStackTrace();
        }

        return;
    }

    /**
     * Writes chartObject to output stream as XLSX
     *
     * @param chartObject a chart object
     * @param output
     * @throws IOException
     */
    public static void writeChartToXLSX(ChartObject chartObject, OutputStream output) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet meta = workbook.createSheet("meta");
            Sheet sheet = workbook.createSheet("data");

            writeChartMetadataXLSX(chartObject, meta);
            writeChartDataXLSX(chartObject, sheet);

            workbook.write(output);
        }
    }

    private static void writeChartMetadataXLSX(ChartObject chartObject, Sheet sheet) {
        Row title = sheet.createRow(0);
        Row subtitle = sheet.createRow(1);
        Row unit = sheet.createRow(2);
        Row source = sheet.createRow(3);

        title.createCell(0).setCellValue("Title"); title.createCell(1).setCellValue(chartObject.title);
        subtitle.createCell(0).setCellValue("Subtitle"); subtitle.createCell(1).setCellValue(chartObject.subtitle);
        unit.createCell(0).setCellValue("Unit"); unit.createCell(1).setCellValue(chartObject.unit);
        source.createCell(0).setCellValue("Source"); source.createCell(1).setCellValue(chartObject.source);
    }
    private static void writeChartDataXLSX(ChartObject chartObject, Sheet sheet) {
        Row title = sheet.createRow(0);

        int col = 1;
        for(String series: chartObject.series){
            title.createCell(col++).setCellValue(series);
        }

        int rowNo = 1;
        for(HashMap<String, String> rowData: chartObject.data) {
            Row row = sheet.createRow(rowNo++);
            row.createCell(0).setCellValue( rowData.get(chartObject.categoryKey()) );

            col = 1;
            for(String colName: chartObject.series) {
                row.createCell(col++).setCellValue(rowData.get(colName));
            }
        }
    }

    private static void writeChartTitles(ChartObject chartObject, CSVWriter writer) throws IOException {
        List<String> titles = new ArrayList<>();
        titles.add("Row");
        for(String series: chartObject.series) {
            titles.add(series);
        }
        writer.writeNext(titles.toArray(new String[titles.size()]));
    }

    private static void writeChartData(ChartObject chartObject, CSVWriter writer) throws IOException {
        List<String> data;

        for(HashMap<String ,String> row: chartObject.data) {
            data = new ArrayList<>();

            data.add(row.get(chartObject.categoryKey()));

            for(String colName: chartObject.series) {
                data.add(row.get(colName));
            }

            writer.writeNext(data.toArray(new String[data.size()]));
        }
    }

    private static void writeBlankLine(CSVWriter writer) {
        writer.writeNext(new String[]{""});
    }

    private static void writeChartMetadata(ChartObject chartObject, CSVWriter writer) {
        writer.writeNext(new String[]{"title", chartObject.title});
        writer.writeNext(new String[]{"subtitle", chartObject.subtitle});
        writer.writeNext(new String[]{"unit", chartObject.unit});
        writer.writeNext(new String[]{"source", chartObject.source});
    }

}
