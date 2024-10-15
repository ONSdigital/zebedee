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
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.content.page.base.PageType.ARTICLE;
import static com.github.onsdigital.zebedee.content.page.base.PageType.BULLETIN;
import static com.github.onsdigital.zebedee.content.page.base.PageType.COMPENDIUM_CHAPTER;
import static com.github.onsdigital.zebedee.content.page.base.PageType.COMPENDIUM_LANDING_PAGE;
import static com.github.onsdigital.zebedee.content.page.base.PageType.STATIC_METHODOLOGY;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;

import java.security.InvalidParameterException;

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
            generatePDFForContent(collection, contentWriter, detail.getUri(), detail.getDescription().getLanguage());

            // FIXME: This conditional section is a workaround to generate both language
            // PDFs when changes to content are made for both English and Welsh languages.
            //
            // In that case, collectionContent will contain a single entry for each url
            // (instead of one for each language), and the value of description.language
            // can not be guaranteed (it could be either Welsh or English)
            //
            // This is because ContentDetailUtil.resolveDetails returns a Set and
            // ContentDetails objects are considered equals when they have the same url
            // (ignoring the language)
            //
            // Ideally, the equals implementation should take language into account and we
            // would receive here one ContentDetail for every language, making this
            // conditional section unnecessary.
            // However, zebedee heavily relies on that Set keeping one ContentDetail per url
            // (for example for the content tree) and changing it will have a lot of
            // unexpected implications
            Optional<ContentLanguage> otherLanguage = getOtherLanguage(detail, contentReader);
            if (otherLanguage.isPresent()) {
                generatePDFForContent(collection, contentWriter, detail.getUri(), otherLanguage.get());
            }

            info().collectionID(collection)
                    .data("uri", detail.uri)
                    .data("pdf_index",index)
                    .data("pdf_count",filtered.size())
                    .log("successfully generated collection content PDF");
            index++;
        }

        info().collectionID(collection)
                .data("pdf_count",filtered.size())
                .log("successfully generated PDFs for collection content");
    }

    private List<ContentDetail> filterPDFContent(List<ContentDetail> content) {
        return content.stream()
                .filter(isPDFPage)
                .collect(Collectors.toList());
    }

    /**
     * Finds if there is a version of the given ContentDetail in the given
     * ContentReader for another language. Ie if the ContentDetail is Welsh, it
     * finds the English. It returns the other language if there is such a file
     * 
     * @param detail
     * @param contentReader
     * @return A ContentLanguage if there is a file for that language or empty if
     *         there isn't
     */
    private static Optional<ContentLanguage> getOtherLanguage(ContentDetail detail, ContentReader contentReader) {
        ContentLanguage otherLanguage;
        switch (detail.getDescription().getLanguage()) {
        case ENGLISH:
            otherLanguage = ContentLanguage.WELSH;
            break;
        case WELSH:
        default:
            otherLanguage = ContentLanguage.ENGLISH;
        }
        
        // Check if a data file for the other language exists
        Resource r = null;
        try {
            r = contentReader.getResource(detail.getUri() + "/" + otherLanguage.getDataFileName());
        } catch (Exception e) {
            // It's ok. If there is no other language data file, we won't generate its PDF
        }
        
        return r != null ? Optional.of(otherLanguage) : Optional.empty();
    }

    private boolean generatePDFForContent(Collection collection, ContentWriter writer, String uri, ContentLanguage language)
            throws InternalServerError {
        if (language == null) {
            throw new InvalidParameterException("Language can't be null");
        }
        CMSLogEvent e = info().data("uri", uri).data("lang", language.toString()).collectionID(collection);
        try {
            pdfService.generatePdf(writer, uri, language);
            e.log("content PDF generated successfully");
            return true;
        } catch (Exception ex) {
            e.exception(ex).log("error generating PDF content");
            throw new InternalServerError("error generating PDF content", ex);
        }
    }

}
