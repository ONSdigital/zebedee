package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.model.ContentWriter;

import java.io.IOException;

/**
 * dummy implementation of the PDF service to return an empty PDF.
 */
public class DummyPdfService implements PdfService {

    @Override
    public void generatePdf(ContentWriter contentWriter, String uri) throws IOException {
        //return new ByteArrayInputStream(new byte[]{});
    }
}
