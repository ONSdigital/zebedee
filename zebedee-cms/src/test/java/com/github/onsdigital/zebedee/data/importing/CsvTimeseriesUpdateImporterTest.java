package com.github.onsdigital.zebedee.data.importing;

import com.github.davidcarboni.ResourceUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CsvTimeseriesUpdateImporterTest {

    @Test(expected = IOException.class)
    public void shouldThrowExceptionIfCsvNotFound() throws IOException {

        // Given a CsvDataImporter with a csv path that does not exist.
        TimeseriesUpdateImporter dataImporter = new CsvTimeseriesUpdateImporter(Paths.get("fileDoesNotExists"));

        // When the importData method is called.
        dataImporter.importData();

        // Then an IO exception is thrown.
    }

    @Test
    public void shouldParseCommandsWithNoHeaders() throws IOException {

        // Given a CsvDataImporter with a csv that contains no headers.
        TimeseriesUpdateImporter dataImporter = new CsvTimeseriesUpdateImporter(ResourceUtils.getFile("/timeseries-import/no-headers.csv").toPath());

        // When the importData method is called.
        ArrayList<TimeseriesUpdateCommand> commands = dataImporter.importData();

        // Then commands are created based on the conventional input: CDID,name
        assertNotNull(commands);
        assertEquals(2, commands.size());

        assertEquals("CXNV", commands.get(0).cdid);
        assertEquals("the new title for CXNV", commands.get(0).title);

        assertEquals("CXNW", commands.get(1).cdid);
        assertEquals("and the new title for CXNW", commands.get(1).title);
    }
}
