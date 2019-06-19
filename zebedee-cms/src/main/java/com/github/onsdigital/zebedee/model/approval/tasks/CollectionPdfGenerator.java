package com.github.onsdigital.zebedee.model.approval.tasks;

import com.github.onsdigital.logging.v2.event.SimpleEvent;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.service.PdfService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

/**
 * Generates a PDF for each page in a collection that needs one.
 */
public class CollectionPdfGenerator {

    private static final List<PageType> pagesWithPdf;

    private static ExecutorService executorService = Executors.newFixedThreadPool(5);

    static {
        pagesWithPdf = new ArrayList<>();
        pagesWithPdf.add(PageType.article);
        pagesWithPdf.add(PageType.bulletin);
        pagesWithPdf.add(PageType.compendium_landing_page);
        pagesWithPdf.add(PageType.compendium_chapter);
        pagesWithPdf.add(PageType.static_methodology);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> executorService.shutdown()));
    }

    private PdfService pdfService;

    /**
     * Create a new instance to use the provided PdfService.
     *
     * @param pdfService
     */
    public CollectionPdfGenerator(PdfService pdfService) {
        this.pdfService = pdfService;
    }

/*
    public void generatePdfsInCollection(CollectionWriter collectionWriter, Iterable<ContentDetail> collectionContent) {

        for (ContentDetail contentDetail : collectionContent) {
            boolean pdfShouldBeAdded = pagesWithPdf.contains(PageType.valueOf(contentDetail.type));

            if (pdfShouldBeAdded) {
                String pdfUri = null;
                try {
                    pdfService.generatePdf(collectionWriter.getReviewed(), contentDetail.uri);
                } catch (IOException e) {
                    error().data("uri", pdfUri).logException(e, "Error while generating collection PDF");
                }
            }
        }
    }
*/


    public void generatePdfsInCollection(CollectionWriter collectionWriter, List<ContentDetail> collectionContent)
            throws ZebedeeException {
        ContentWriter writer = collectionWriter.getReviewed();

        List<Callable<Boolean>> jobs = collectionContent.stream()
                .filter(isPDFPage())
                .map(c -> toCallable(writer, "TODO", c.uri))
                .collect(Collectors.toList());

        try {
            List<Future<Boolean>> results = executorService.invokeAll(jobs);
            for (Future<Boolean> r : results) {
                r.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            error().exception(e.getCause()).log("check generate PDF future returned an error");
        } catch (Exception ex) {
            throw new InternalServerError("error generating collection PDF content", ex);
        }
    }

    public Callable<Boolean> toCallable(ContentWriter writer, String collectionName, String uri) {
        return () -> {
            SimpleEvent e = info().data("uri", uri).data("collection_name", collectionName);
            try {
                pdfService.generatePdf(writer, uri);
                e.log("content PDF generated successfully");
            } catch (Exception ex) {
                e.exception(ex).log("error generating PDF content");
                throw ex;
            }
            return true;
        };
    }

    private Predicate<ContentDetail> isPDFPage() {
        return (contentDetail -> pagesWithPdf.contains(PageType.valueOf(contentDetail.type)));
    }
}
