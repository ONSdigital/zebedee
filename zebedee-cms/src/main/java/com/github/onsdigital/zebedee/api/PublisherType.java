package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.UnexpectedErrorException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.util.ZebedeeApiHelper;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * Created by dave on 5/13/16.
 */
@Api
public class PublisherType {

    private static ZebedeeApiHelper apiHelper = ZebedeeApiHelper.getInstance();

    @GET
    public void getCollectionUserType(HttpServletRequest request, HttpServletResponse response) throws ZebedeeException {
        Session session = apiHelper.getSession(request);
        try {
            writeToResponse(apiHelper.getPublisherType(session.email), response);
        } catch (IOException e) {
            logError(e, "Unexpected Error while writing json to httpServletResponse")
                    .logAndThrow(UnexpectedErrorException.class);
        }
    }

    private void writeToResponse(com.github.onsdigital.zebedee.PublisherType publisherType, HttpServletResponse response) throws IOException {
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("publisherType", publisherType.name());
        IOUtils.copy(apiHelper.objectAsInputStream(jsonResponse), response.getOutputStream());
    }
}
