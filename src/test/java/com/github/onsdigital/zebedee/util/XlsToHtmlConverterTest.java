package com.github.onsdigital.zebedee.util;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.w3c.dom.Document;

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
    public void convertShouldReturnDocument() throws IOException, ParserConfigurationException {

        // Given an xls file
        File xlsFile = new File(getClass().getResource("/xls/example-table.xls").getFile());

        // When the convert method is called.
        Document document = XlsToHtmlConverter.convert(xlsFile);

        // Then a Document instance is returned with the HTML content of the XLS file.
        assertNotNull(document);
    }

    @Test
    public void saveShouldPersistDocument() throws IOException, ParserConfigurationException, TransformerException {

        // Given a document instance.
        File xlsFile = new File(getClass().getResource("/xls/example-table.xls").getFile());
        Document document = XlsToHtmlConverter.convert(xlsFile);

        // When the save method is called.
        Path fileToSaveTo = Paths.get("src/test/resources/xls/example-table.html");
        XlsToHtmlConverter.save(document, fileToSaveTo);

        // Then a Document instance is returned with the HTML content of the XLS file.
        assertTrue(Files.exists(fileToSaveTo));
    }

    @Test
    public void toStringShouldReturnString() throws IOException, ParserConfigurationException, TransformerException {

        // Given a document instance.
        File xlsFile = new File(getClass().getResource("/xls/example-table.xls").getFile());
        Document document = XlsToHtmlConverter.convert(xlsFile);

        // When the toString method is called.
        String output = XlsToHtmlConverter.docToString(document);

        // Then a Document instance is returned with the HTML content of the XLS file.
        assertTrue(StringUtils.isNotBlank(output));
    }
}
