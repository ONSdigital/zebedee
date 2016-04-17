package com.github.onsdigital.zebedee.service;

import java.io.IOException;
import java.io.InputStream;

public interface PdfService {

    /**
     * Render a PDF for the given page URI.
     * @param uri - the uri to generate the PDF for.
     * @return - the input stream containing the PDF data.
     * @throws IOException
     */
    InputStream generatePdf(String uri) throws IOException;
}
