package com.github.onsdigital.zebedee.model.publishing.legacycacheapi;

import com.github.davidcarboni.httpino.*;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.model.publishing.WebsiteResponse;
import com.github.onsdigital.zebedee.util.EncryptionUtils;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.io.IOException;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

public class LegacyCacheApiClient {
    private static final String CACHE_TIMES_RESOURCE_PATH = "/v1/cache-times/";

    private LegacyCacheApiClient() {}

    public static void sendPayloads(Http http, Host host, Iterable<LegacyCacheApiPayload> payloads) throws IOException {
        String legacyCacheApiServiceToken = Configuration.getLegacyCacheAPIAuthToken();
        NameValuePair authHeader = new BasicNameValuePair("Authorization", legacyCacheApiServiceToken);

        for (LegacyCacheApiPayload payload : payloads) {
            String resourceId = EncryptionUtils.createMD5Checksum(payload.uriToUpdate);
            String path = CACHE_TIMES_RESOURCE_PATH + resourceId;

            info().data("path", path)
                    .data("payload", Serialiser.serialise(payload))
                    .log("sending request to Legacy Cache API");

            Endpoint endpoint = new Endpoint(host, path);

            Response<WebsiteResponse> response = http.put(endpoint, payload, WebsiteResponse.class, authHeader);
            logResponse(response, payload.collectionId);
        }
    }

    private static void logResponse(Response<WebsiteResponse> response, String collectionId) {
        String responseMessage = response.getBody() == null ? response.getReasonPhrase() : response.getBody().getMessage();

        if (response.getStatusCode() > 302) {
            error().data("responseMessage", responseMessage)
                    .data("collectionId", collectionId)
                    .log("Error response from Legacy Cache API");
        } else {
            info().data("responseMessage", responseMessage)
                    .data("collectionId", collectionId)
                    .log("Response from Legacy Cache API");
        }
    }
}
