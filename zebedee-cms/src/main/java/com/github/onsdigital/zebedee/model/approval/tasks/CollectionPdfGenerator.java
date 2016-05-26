package com.github.onsdigital.zebedee.model.approval.tasks;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.service.PdfService;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * Generates a PDF for each page in a collection that needs one.
 */
public class CollectionPdfGenerator {

    private static final List<PageType> pagesWithPdf;

    static {
        pagesWithPdf = new ArrayList<>();
        pagesWithPdf.add(PageType.article);
        pagesWithPdf.add(PageType.bulletin);
        pagesWithPdf.add(PageType.compendium_chapter);
    }

    private final PdfService pdfService;

    /**
     * Create a new instance to use the provided PdfService.
     * @param pdfService
     */
    public CollectionPdfGenerator(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    public void generatePdfsInCollection(CollectionWriter collectionWriter, List<ContentDetail> collectionContent) {

        for (ContentDetail contentDetail : collectionContent) {
            boolean pdfShouldBeAdded = pagesWithPdf.contains(PageType.valueOf(contentDetail.type));

            if (pdfShouldBeAdded) {
                String pdfUri = null;
                try (InputStream inputStream = pdfService.generatePdf(contentDetail.uri)) {
                    pdfUri = contentDetail.uri + "/page.pdf";
                    collectionWriter.getReviewed().write(inputStream, pdfUri);

                } catch (IOException | BadRequestException e) {
                    logError(e, "Error while generating collection PDF").addParameter("uri", pdfUri).log();
                }
            }
        }
    }
}
