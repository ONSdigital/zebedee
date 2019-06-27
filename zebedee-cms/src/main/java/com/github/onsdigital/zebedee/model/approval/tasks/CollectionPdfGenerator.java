package com.github.onsdigital.zebedee.model.approval.tasks;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.logging.CMSLogEvent;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.service.PdfService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.content.page.base.PageType.article;
import static com.github.onsdigital.zebedee.content.page.base.PageType.bulletin;
import static com.github.onsdigital.zebedee.content.page.base.PageType.compendium_chapter;
import static com.github.onsdigital.zebedee.content.page.base.PageType.compendium_landing_page;
import static com.github.onsdigital.zebedee.content.page.base.PageType.static_methodology;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.warn;
import static java.text.MessageFormat.format;

/**
 * Generates a PDF for each page in a collection that needs one.
 */
public class CollectionPdfGenerator {

    private static final List<PageType> PDF_GENERATING_PAGES;
    private static final ExecutorService EXECUTOR_SERVICE;

    static {
        // The PDF generation process is done with the entire page content in memory. This can potentially be very large
        // so it is recommended to use a small pool to reduce the chances of running out of heap space.
        EXECUTOR_SERVICE = Executors.newFixedThreadPool(5); // TODO should this be configurable?

        PDF_GENERATING_PAGES = new ArrayList<PageType>() {{
            add(article);
            add(bulletin);
            add(compendium_landing_page);
            add(compendium_chapter);
            add(static_methodology);
        }};

        Runtime.getRuntime().addShutdownHook(new Thread(() -> closeExecutorService()));
    }

    private PdfService pdfService;

    private Predicate<ContentDetail> isPDFPage = (c -> PDF_GENERATING_PAGES.contains(PageType.valueOf(c.type)));

    /**
     * Create a new instance to use the provided PdfService.
     *
     * @param pdfService
     */
    public CollectionPdfGenerator(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    /**
     * @param collection
     * @param collectionWriter
     * @param collectionContent
     * @throws ZebedeeException
     */
    public void generatePDFsForCollection(Collection collection, CollectionWriter collectionWriter,
                                          List<ContentDetail> collectionContent) throws ZebedeeException {
        List<ContentDetail> filtered = filterPDFContent(collectionContent);

        int index = 1;
        for (ContentDetail detail : filtered) {
            generatePDFForContent(collection, collectionWriter.getReviewed(), detail.uri);

            warn().collectionID(collection)
                    .data("uri", detail.uri)
                    .log(format("successfully generated collection content PDF {0}/{1}", index, filtered.size()));
            index++;
        }

        warn().collectionID(collection)
                .log(format("successfully generated {0}/{0} PDFs for collection content", filtered.size()));
    }

    /**
     * Generate PDF's for pages that require them using a thread pool to allow concurrent throughput.
     *
     * @param collection        the collection the task is being invoked for.
     * @param collectionWriter  a CollectionWriter to use to write the generated PDF files back to the the collection
     *                          directories.
     * @param collectionContent the entire content of the collection.
     * @throws ZebedeeException PDF generation was unsuccessful.
     */
    public void generatePDFsForCollectionAsyc(Collection collection, CollectionWriter collectionWriter,
                                              List<ContentDetail> collectionContent) throws ZebedeeException {
        ContentWriter writer = collectionWriter.getReviewed();
        List<Callable<Boolean>> tasks = createGeneratePdfTasks(filterPDFContent(collectionContent), writer, collection);

        invokeAllAndCheckResults(tasks, collection);
    }

    private List<ContentDetail> filterPDFContent(List<ContentDetail> content) {
        return content.stream()
                .filter(isPDFPage)
                .collect(Collectors.toList());
    }

    private List<Callable<Boolean>> createGeneratePdfTasks(List<ContentDetail> filtered, ContentWriter writer,
                                                           Collection collection) {
        // Create a callable for each of the filtered content items.
        List<Callable<Boolean>> tasks = filtered
                .stream()
                .map(c -> newGeneratePDFCallable(writer, collection, c.uri))
                .collect(Collectors.toList());

        // log some useful info.
        info().collectionID(collection)
                .data("uris", filtered
                        .stream()
                        .map(c -> c.uri)
                        .collect(Collectors.toList()))
                .log("successfully created generated PDF tasks for collection content");

        return tasks;
    }

    private void invokeAllAndCheckResults(List<Callable<Boolean>> jobs, Collection collection)
            throws ZebedeeException {
        try {
            List<Future<Boolean>> results = EXECUTOR_SERVICE.invokeAll(jobs);
            checkFutures(collection, results);
        } catch (ZebedeeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InternalServerError("error generating collection PDF content", ex);
        }
    }

    private boolean generatePDFForContent(Collection collection, ContentWriter writer, String uri)
            throws InternalServerError {
        CMSLogEvent e = info().data("uri", uri).collectionID(collection);
        try {
            pdfService.generatePdf(writer, uri);
            e.log("content PDF generated successfully");
            return true;
        } catch (Exception ex) {
            e.exception(ex).log("error generating PDF content");
            throw new InternalServerError("error generating PDF content", ex);
        }
    }

    private Callable<Boolean> newGeneratePDFCallable(ContentWriter writer, Collection collection, String uri) {
        return () -> generatePDFForContent(collection, writer, uri);
    }

    private void checkFutures(Collection collection, List<Future<Boolean>> results) throws ZebedeeException {
        info().collectionID(collection).log("checking generate PDF future results");
        try {
            for (Future<Boolean> r : results) {
                r.get();
            }
        } catch (Exception ex) {
            throw new InternalServerError("checking generate PDF future returned an error", ex);
        }
    }

    private static void closeExecutorService() {
        info().log("shutting down CollectionPdfGenerator.EXECUTOR_SERVICE");
        EXECUTOR_SERVICE.shutdown();
    }

}
