package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.content.base.ContentLanguage;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.URIUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;

import java.io.IOException;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static java.text.MessageFormat.format;

/**
 * Render PDF output for a given URI using babbage.
 */
public class BabbagePdfService implements PdfService {

    private static final String pdfEndpoint = "/pdf-new"; // only ever reading from local babbage instance

    private final Session session;
    private Collection collection;

    public BabbagePdfService(Session session, Collection collection) {
        this.session = session;
        this.collection = collection;
    }

    /**
     * Render a PDF for the given page URI.
     *
     * @param contentWriter Writer where the PDF will be written to
     * @param uri           the uri to generate the PDF for.
     * @param language      The language required for the content
     * @return the input stream containing the PDF data.
     * @throws IOException
     */
    @Override
    public void generatePdf(ContentWriter contentWriter, String uri, ContentLanguage language) throws IOException {
        // no need to check locally here as on publishing we will always want to generate one for preview
        // we will check if one exists already in babbage
        // loop back to babbage to render PDF until we break out the HTML rendering / PDF generation into its own service.
        String trimmedUri = URIUtils.removeTrailingSlash(uri);
        String src = Configuration.getBabbageUrl() + trimmedUri + pdfEndpoint;
        String pdfURI = String.format("%s/page%s.pdf", uri, language.getFileSuffix());

        info().data("src", src).log("Reading PDF");

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpGet httpGet = new HttpGet(src);
            httpGet.addHeader("Cookie", "access_token=" + session.getId());
            httpGet.addHeader("Cookie", "collection=" + collection.getDescription().getId());
            httpGet.addHeader("Cookie", "lang=" + language.getId());

            try (CloseableHttpResponse response = client.execute(httpGet)) {

                int status = response.getCode();
                if (status != 200) {
                    String body = response.toString();
                    error().data("status_code", status)
                            .data("body", body)
                            .log("generate PDF failure");

                    throw new IOException(format("Failed to generate PDF for URI {0}. Response: {1} {2}", uri, status, body));
                }
                contentWriter.write(response.getEntity().getContent(), pdfURI);
            } catch (Exception e) {
                error().data("path", pdfURI).logException(e, "Error while generating collection PDF");
                throw new IOException(e);
            }
        }
    }
}
