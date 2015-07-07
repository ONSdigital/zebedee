package com.github.onsdigital.zebedee.util;

import com.github.davidcarboni.ResourceUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.w3c.dom.Node;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class XlsToHtmlConverterTest {

    @Test
    public void convertToHtmlPageShouldReturnDocument() throws IOException, ParserConfigurationException {

        // Given an xls file
        File xlsFile = ResourceUtils.getFile("/xls/example-table.xls");

        // When the convert method is called.
        Node document = XlsToHtmlConverter.convertToHtmlPage(xlsFile);

        // Then a Document instance is returned with the HTML content of the XLS file.
        assertNotNull(document);
    }

    @Test
    public void convertToTableShouldReturnDocument() throws IOException, ParserConfigurationException {

        // Given an xls file
        File xlsFile = ResourceUtils.getFile("/xls/example-table.xls");

        // When the convert method is called.
        Node document = XlsToHtmlConverter.convertToTable(xlsFile);

        // Then a Document instance is returned with the HTML content of the XLS file.
        assertNotNull(document);
    }

    @Test
    public void saveShouldPersistDocument() throws IOException, ParserConfigurationException, TransformerException {

        // Given a document instance.
        File xlsFile = ResourceUtils.getFile("/xls/example-table.xls");
        Node document = XlsToHtmlConverter.convertToTable(xlsFile);

        // When the save method is called.
        Path fileToSaveTo = Paths.get("src/test/resources/xls/example-table.html");
        XlsToHtmlConverter.save(document, fileToSaveTo);

        // Then a Document instance is returned with the HTML content of the XLS file.
        assertTrue(Files.exists(fileToSaveTo));
    }


    @Test
    public void toStringForTableShouldReturnString() throws IOException, ParserConfigurationException, TransformerException {

        // Given a document instance.
        File xlsFile = ResourceUtils.getFile("/xls/example-table.xls");
        Node table = XlsToHtmlConverter.convertToHtmlPage(xlsFile);

        // When the toString method is called.
        String output = XlsToHtmlConverter.docToString(table);

        //System.out.println(output);

        // Then a Document instance is returned with the HTML content of the XLS file.
        assertTrue(StringUtils.isNotBlank(output));
    }

    @Test
    public void toStringShouldReturnString() throws IOException, ParserConfigurationException, TransformerException {

        // Given a document instance.
        File xlsFile = ResourceUtils.getFile("/xls/example-subscript-table.xls");
        Node table = XlsToHtmlConverter.convertToTable(xlsFile);

        // When the toString method is called.
        String output = XlsToHtmlConverter.docToString(table);

        //System.out.println(output);

        // Then a Document instance is returned with the HTML content of the XLS file.
        assertTrue(StringUtils.isNotBlank(output));
    }

    @Test
    public void convertToTableShouldRenderSubscript() throws IOException, ParserConfigurationException, TransformerException {

        // Given an xls file with subscript and superscript content
        File xlsFile = ResourceUtils.getFile("/xls/example-subscript-table.xls");

        // When the convert method is called.
        Node table = XlsToHtmlConverter.convertToTable(xlsFile);
        String output = XlsToHtmlConverter.docToString(table);

        //System.out.println(output);
        // Then a Document instance is returned with the HTML content of the XLS file including subscript / superscript.
        assertTrue(StringUtils.isNotBlank(output));
        assertTrue(StringUtils.contains(output, "<sub>This is subscript1 </sub>"));
        assertTrue(StringUtils.contains(output, "<sub>subscript2</sub>"));
        assertTrue(StringUtils.contains(output, "<sup>superscript1</sup>"));
        assertTrue(StringUtils.contains(output, "<sup>superscript2</sup>"));
    }

}
