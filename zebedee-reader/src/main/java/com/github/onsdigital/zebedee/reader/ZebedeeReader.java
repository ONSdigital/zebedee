package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.util.ContentReader;

/**
 * Created by bren on 29/07/15.
 *
 */
public class ZebedeeReader {
    private static ContentReader publishedReader = new ContentReader(ReaderConfiguration.getZebedeeRoot() + "/master") ;



}
