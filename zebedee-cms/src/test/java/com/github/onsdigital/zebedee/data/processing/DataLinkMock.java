package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;

/**
 * Created by thomasridd on 1/24/16.
 */
public class DataLinkMock implements DataLink{
    TimeSerieses returnThis;
    public String lastCall = "";

    public DataLinkMock(TimeSerieses returnValues) {
        this.returnThis = returnValues;
    }
    @Override
    public TimeSerieses callCSDBProcessor(String uri, ContentReader collectionReader) throws IOException, ZebedeeException {
        lastCall = "csdb";
        return returnThis;
    }

    @Override
    public TimeSerieses callCSVProcessor(String uri, ContentReader collectionReader) throws IOException, ZebedeeException {
        lastCall = "csv";
        return returnThis;
    }
}
