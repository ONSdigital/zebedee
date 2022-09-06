package com.github.onsdigital.zebedee.model.approval.tasks;

import com.github.onsdigital.zebedee.content.base.ContentLanguage;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.logging.CMSLogEvent;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.service.PdfService;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.content.page.base.PageType.ARTICLE;
import static com.github.onsdigital.zebedee.content.page.base.PageType.BULLETIN;
import static com.github.onsdigital.zebedee.content.page.base.PageType.COMPENDIUM_CHAPTER;
import static com.github.onsdigital.zebedee.content.page.base.PageType.COMPENDIUM_LANDING_PAGE;
import static com.github.onsdigital.zebedee.content.page.base.PageType.STATIC_METHODOLOGY;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;
import static java.text.MessageFormat.format;

/**
 * Generates a PDF for each page in a collection that needs one.
 */
public class CollectionPdfGenerator {

    private static final List<PageType> PDF_GENERATING_PAGES = Arrays.asList(ARTICLE, BULLETIN, COMPENDIUM_LANDING_PAGE,
            COMPENDIUM_CHAPTER, STATIC_METHODOLOGY);

    private PdfService pdfService;

    private Predicate<ContentDetail> isPDFPage = (c -> PDF_GENERATING_PAGES.contains(c.getType()));

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
     * @param contentReader
     * @param contentWriter
     * @param collectionContent
     * @throws ZebedeeException
     */
    public void generatePDFsForCollection(Collection collection, ContentReader contentReader, ContentWriter contentWriter,
                                          List<ContentDetail> collectionContent) throws ZebedeeException {
        List<ContentDetail> filtered = filterPDFContent(collectionContent);

        int index = 1;
        for (ContentDetail detail : filtered) {
            generatePDFForContent(collection, contentWriter, detail.uri, detail.description.language);

            if (!ContentLanguage.WELSH.getId().equals(detail.description.language)) {
                // If this is an English version, check if a Welsh data file exists

                Resource r;
                try {
                    r = contentReader.getResource(detail.uri + "/" + ContentLanguage.WELSH.getDataFileName());
                } catch (Exception e) {
                    // It's ok. If there is no Welsh data file, we won't generate a Welsh PDF
                    r = null;
                }

                if (r != null) {
                    generatePDFForContent(collection, contentWriter, detail.uri, ContentLanguage.WELSH.getId());
                }
            }

            info().collectionID(collection)
                    .data("uri", detail.uri)
                    .log(format("successfully generated collection content PDF {0}/{1}", index, filtered.size()));
            index++;
        }

        info().collectionID(collection)
                .log(format("successfully generated {0}/{0} PDFs for collection content", filtered.size()));
    }

    private List<ContentDetail> filterPDFContent(List<ContentDetail> content) {
        return content.stream()
                .filter(isPDFPage)
                .collect(Collectors.toList());
    }

    private boolean generatePDFForContent(Collection collection, ContentWriter writer, String uri, String language)
            throws InternalServerError {
        ContentLanguage lang = ContentLanguage.ENGLISH;
        if (ContentLanguage.WELSH.getId().equalsIgnoreCase(language)) {
            lang = ContentLanguage.WELSH;
        }
        CMSLogEvent e = info().data("uri", uri).collectionID(collection);
        try {
            pdfService.generatePdf(writer, uri, lang);
            e.log("content PDF generated successfully");
            return true;
        } catch (Exception ex) {
            e.exception(ex).log("error generating PDF content");
            throw new InternalServerError("error generating PDF content", ex);
        }
    }

}
