package com.github.onsdigital.zebedee.data.importing;

import com.github.davidcarboni.ResourceUtils;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CsvTimeseriesUpdateImporterTest {

    @Test
    public void shouldParseCommandsWithNoHeaders() throws IOException {

        // Given a CsvDataImporter with a csv that contains no headers.
        TimeseriesUpdateImporter dataImporter = new CsvTimeseriesUpdateImporter(new FileInputStream(ResourceUtils.getFile("/timeseries-import/no-headers.csv")));

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
