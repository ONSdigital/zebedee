package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;

/**
 * Created by thomasridd on 1/24/16.
 */
public interface DataLink {
    public TimeSerieses callCSDBProcessor(String uri, ContentReader collectionReader) throws IOException, ZebedeeException;

    public TimeSerieses callCSVProcessor(String uri, ContentReader collectionReader) throws IOException, ZebedeeException;
}

