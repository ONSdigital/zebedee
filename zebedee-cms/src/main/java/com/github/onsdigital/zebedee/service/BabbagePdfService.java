package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.util.URIUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * Render PDF output for a given URI using babbage.
 */
public class BabbagePdfService implements PdfService {

    private static final String babbageUri = "http://babbage-publishing-develop:8080"; //"http://localhost:8080/"; // only ever reading from local babbage instance
    private static final String pdfEndpoint = "/pdf-new"; // only ever reading from local babbage instance

    private final Session session;
    private Collection collection;

    public BabbagePdfService(Session session, Collection collection) {
        this.session = session;
        this.collection = collection;
    }

    /**
     * Render a PDF for the given page URI.
     * @param uri - the uri to generate the PDF for.
     * @return - the input stream containing the PDF data.
     * @throws IOException
     */
    @Override
    public InputStream generatePdf(String uri) throws IOException {

        // no need to check locally here as on publishing we will always want to generate one for preview
        // we will check if one exists already in babbage

        // loop back to babbage to render PDF until we break out the HTML rendering / PDF generation into its own service.

        String trimmedUri = URIUtils.removeTrailingSlash(URIUtils.removeLeadingSlash(uri));
        String src = babbageUri + trimmedUri + pdfEndpoint;

        // if the url is absolute, go get it using HTTP client.
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet httpGet = new HttpGet(src);
        httpGet.addHeader("Cookie", "access_token=" + session.id);
        httpGet.addHeader("Cookie", "collection=" + collection.description.id);
        HttpResponse response = client.execute(httpGet);

        return response.getEntity().getContent();
    }
}
