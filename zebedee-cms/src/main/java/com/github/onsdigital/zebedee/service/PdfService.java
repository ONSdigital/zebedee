package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.content.base.ContentLanguage;
import com.github.onsdigital.zebedee.model.ContentWriter;

import java.io.IOException;

public interface PdfService {

    /**
     * Render a PDF for the given page URI.
     * 
     * @param contentWriter Writer where the PDF will be written to
     * @param uri           the uri to generate the PDF for.
     * @param language      The language required for the content
     * @return the input stream containing the PDF data.
     * @throws IOException
     */
    void generatePdf(ContentWriter contentWriter, String uri, ContentLanguage language) throws IOException;
}