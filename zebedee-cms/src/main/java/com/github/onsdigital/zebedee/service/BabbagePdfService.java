package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.util.URIUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * Render PDF output for a given URI using babbage.
 */
public class BabbagePdfService implements PdfService {

    private static final String pdfEndpoint = "/pdf-new"; // only ever reading from local babbage instance
    private static final String PAGE_PDF_EXT = "/page.pdf";

    private final Session session;
    private Collection collection;

    public BabbagePdfService(Session session, Collection collection) {
        this.session = session;
        this.collection = collection;
    }

    /**
     * Render a PDF for the given page URI.
     *
     * @param uri - the uri to generate the PDF for.
     * @return - the input stream containing the PDF data.
     * @throws IOException
     */
    @Override
    public void generatePdf(ContentWriter contentWriter, String uri) throws IOException {
        // no need to check locally here as on publishing we will always want to generate one for preview
        // we will check if one exists already in babbage
        // loop back to babbage to render PDF until we break out the HTML rendering / PDF generation into its own service.
        String trimmedUri = URIUtils.removeTrailingSlash(uri);
        String src = Configuration.getBabbageUrl() + trimmedUri + pdfEndpoint;
        String pdfURI = uri + PAGE_PDF_EXT;

        logInfo("Reading PDF").addParameter("src", src).log();

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpGet httpGet = new HttpGet(src);
            httpGet.addHeader("Cookie", "access_token=" + session.getId());
            httpGet.addHeader("Cookie", "collection=" + collection.getDescription().getId());

            try (CloseableHttpResponse response = client.execute(httpGet)) {

                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new IOException("Failed to generate PDF for URI " + uri +
                            ". Response: " + response.getStatusLine().getStatusCode() +
                            " " + response.toString());
                }
                contentWriter.write(response.getEntity().getContent(), pdfURI);
            } catch (BadRequestException e) {
                logError(e, "Error while generating collection PDF").path(pdfURI).log();
            }
        }
    }
}
