package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

/**
 * Created by kanemorgan on 01/04/2015.
 */
@Api
public class Approve {
    @POST
    public  boolean approveCollection(HttpServletRequest request,HttpServletResponse response) throws IOException {
        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);

        // check everything is completed
        if (!collection.inProgressUris().isEmpty() || !collection.completeUris().isEmpty()) {
            response.setStatus(HttpStatus.CONFLICT_409);
            return false;
        }

        response.setStatus(HttpStatus.OK_200);
        collection.description.approvedStatus = true;

        return collection.save();

    }
}
