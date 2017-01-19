package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.model.ContentWriter;

import java.io.IOException;
import java.io.InputStream;

public interface PdfService {

    /**
     * Render a PDF for the given page URI.
     * @param uri - the uri to generate the PDF for.
     * @return - the input stream containing the PDF data.
     * @throws IOException
     */
    void generatePdf(ContentWriter contentWriter, String uri) throws IOException;
}