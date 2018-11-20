package com.github.onsdigital.zebedee.search.fastText.requests;

import com.github.onsdigital.zebedee.search.fastText.response.InfoResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jetty.http.HttpScheme;

import java.net.URISyntaxException;

public class InfoRequest extends FastTextRequest<InfoResponse> {

    private static final String PATH = "/supervised/info";

    public InfoRequest(String requestId) {
        super(requestId, InfoResponse.class);
    }

    @Override
    protected URIBuilder uriBuilder() {
        return new URIBuilder()
                .setScheme(HttpScheme.HTTP.asString())
                .setHost(HOST)
                .setPath(PATH);
    }

    @Override
    public HttpRequestBase getRequestBase() throws URISyntaxException {
        return super.get();
    }
}
