package com.github.onsdigital.zebedee.json.converter;

import au.com.bytecode.opencsv.CSVWriter;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.google.common.primitives.Chars;
import sun.misc.IOUtils;

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

        if(inputFormat.equals("chart")){

            if(outputFormat.equals("csv")) {
                // Deserialise the chart object
//                StringWriter writer = new StringWriter();
//                org.apache.commons.io.IOUtils.copy(request.getInputStream(), writer);
//                String json = writer.toString();

                ChartObject chartObject = Serialiser.deserialise(request, ChartObject.class);

                response.setContentType("application/csv");
                try(OutputStream stream = response.getOutputStream()) {
                    writeChartToCSV(chartObject, stream);
                }
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
