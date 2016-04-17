package com.github.onsdigital.zebedee.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * dummy implementation of the PDF service to return an empty PDF.
 */
public class DummyPdfService implements PdfService {

    @Override
    public InputStream generatePdf(String uri) throws IOException {
        return new ByteArrayInputStream(new byte[]{});
    }
}
