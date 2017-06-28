package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.model.CollectionOwner;
import com.github.onsdigital.zebedee.exceptions.UnexpectedErrorException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
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
public class UserPublisherType {

    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();

    @GET
    public void getCollectionUserType(HttpServletRequest request, HttpServletResponse response) throws ZebedeeException {
        Session session = zebedeeCmsService.getSession(request);
        try {
            writeToResponse(zebedeeCmsService.getPublisherType(session.getEmail()), response);
        } catch (IOException e) {
            logError(e, "Unexpected Error while writing json to httpServletResponse")
                    .logAndThrow(UnexpectedErrorException.class);
        }
    }

    private void writeToResponse(CollectionOwner collectionOwner, HttpServletResponse response) throws IOException {
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("userPublisherType", collectionOwner.name());
        IOUtils.copy(zebedeeCmsService.objectAsInputStream(jsonResponse), response.getOutputStream());
    }
}
