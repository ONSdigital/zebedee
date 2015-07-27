package com.github.onsdigital.zebedee.json.converter;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by thomasridd on 13/05/15.
 */
public class JSONToFileConverterTest {
    ChartObject chart;

    @Before
    public void buildJSONObject() {
        chart = new ChartObject();
        chart.title = "my title";
        chart.subtitle = "my subtitle";
        chart.unit = "my unit";
        chart.source = "my source";

        chart.series = Arrays.asList("A", "B");

        LinkedHashMap<String, String> dataPoint = new LinkedHashMap<>();
        dataPoint.put("Row", "Row1"); dataPoint.put("A", "1.0"); dataPoint.put("B", "2.0");
        chart.data.add(dataPoint);

        dataPoint= new LinkedHashMap<>();
        dataPoint.put("Row", "Row2"); dataPoint.put("A", "5.0"); dataPoint.put("B", "1.0");
        chart.data.add(dataPoint);

        dataPoint = new LinkedHashMap<>();
        dataPoint.put("Row", "Row3"); dataPoint.put("A", "1.1"); dataPoint.put("B", "0.05");
        chart.data.add(dataPoint);
    }

    @Test
    public void jsonTestDoesWriteToOutput() throws IOException {
        // Given
        // an output stream
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // When
            // we write our example chart to it
            JSONToFileConverter.writeChartToCSV(chart, baos);

            // Then
            // the result is not null
            String result = new String(baos.toByteArray(), Charset.defaultCharset());
            assertTrue(result.equals("") == false);
        }
    }

    @Test
    public void jsonTestDoesWriteToCSV() throws IOException {
        // Given
        // an output stream
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // When
            // we write our example chart to it
            JSONToFileConverter.writeChartToCSV(chart, baos);

            // Then
            // we expect a csv object to have been written to baos
            String result = new String(baos.toByteArray(), Charset.defaultCharset());

            assertTrue(result.indexOf(",") >= 0);
        };
    }

    @Test
    public void jsonTestDoesWriteTitles() throws IOException {
        // Given
        // the string result
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JSONToFileConverter.writeChartToCSV(chart, baos);
            String result = new String(baos.toByteArray(), Charset.defaultCharset());


            // When
            // we get the first line
            String[] results = result.split("\n");
            String firstline = results[0];

            // Then
            // we expect a csv object to have been written to baos
            assertEquals("\"Row\",\"A\",\"B\"", firstline);
        };
    }

    @Test
    public void jsonTestDoesWriteData() throws IOException {
        // Given
        // the string result
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JSONToFileConverter.writeChartToCSV(chart, baos);
            String result = new String(baos.toByteArray(), Charset.defaultCharset());

            // When
            // we get the first line
            String[] results = result.split("\n");


            // Then
            // we expect the csv object to have been written to baos
            assertTrue(results.length >= 4);
            assertEquals(String.format("\"%s\",\"%s\",\"%s\"", "Row1", "1.0", "2.0"), results[1]);
            assertEquals(String.format("\"%s\",\"%s\",\"%s\"", "Row2", "5.0", "1.0"), results[2]);
            assertEquals(String.format("\"%s\",\"%s\",\"%s\"", "Row3", "1.1", "0.05"), results[3]);
        };
    }

    @Test
    public void jsonTestDoesWriteBlankline() throws IOException {
        // Given
        // the string result
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JSONToFileConverter.writeChartToCSV(chart, baos);
            String result = new String(baos.toByteArray(), Charset.defaultCharset());

            // When
            // we get the first line
            String[] results = result.split("\n");


            // Then
            // we expect the csv object to have been written to baos
            assertTrue(results.length >= 5);
            assertEquals("\"\"", results[4]);
        };
    }

    @Test
    public void jsonTestDoesWriteMetadata() throws IOException {
        // Given
        // the string result
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JSONToFileConverter.writeChartToCSV(chart, baos);
            String result = new String(baos.toByteArray(), Charset.defaultCharset());

            // When
            // we get the first line
            String[] results = result.split("\n");


            // Then
            // we expect the csv object to have been written to baos
            assertTrue(results.length >= 9);
            assertEquals(String.format("\"%s\",\"%s\"", "title", chart.title), results[5]);
            assertEquals(String.format("\"%s\",\"%s\"", "subtitle", chart.subtitle), results[6]);
            assertEquals(String.format("\"%s\",\"%s\"", "unit", chart.unit), results[7]);
            assertEquals(String.format("\"%s\",\"%s\"", "source", chart.source), results[8]);
        };
    }

}