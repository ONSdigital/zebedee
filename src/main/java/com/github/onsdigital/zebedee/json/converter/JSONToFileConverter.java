package com.github.onsdigital.zebedee.json.converter;

import au.com.bytecode.opencsv.CSVWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by thomasridd on 13/05/15.
 */
public class JSONToFileConverter {

    public static void writeChartToCSV(ChartObject chartObject, OutputStream output) throws IOException {

        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(output, Charset.forName("UTF8")), ',')) {

            writeChartTitles(chartObject, writer);
            writeChartData(chartObject, writer);
            writeBlankLine(writer);
            writeChartMetadata(chartObject, writer);

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

        for(String rowName: chartObject.data.keySet()) {
            data = new ArrayList<>();

            data.add(rowName);
            HashMap<String,String> item = chartObject.data.get(rowName);

            for(String colName: chartObject.series) {
                data.add(item.get(colName));
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
