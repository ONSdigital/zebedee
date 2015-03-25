package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.TransferRequest;
import com.github.onsdigital.zebedee.model.*;
import com.github.onsdigital.zebedee.model.Collection;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by kanemorgan on 24/03/2015.
 */
@Api
public class Transfer {
    @POST
    public boolean move(HttpServletRequest request, HttpServletResponse response, TransferRequest params) throws IOException {
        boolean result = true;
        Session session = Root.zebedee.sessions.get(request);

        // get the source collection
        Collection source = getSource(params,request);

        Path sourcePath = source.find(session.email,params.uri);
        if (Files.notExists(sourcePath)){
            response.setStatus(HttpStatus.NOT_FOUND_404);
            result = false;
        }

        // get the destination file
        Collection destination = Root.zebedee.getCollections().getCollection(params.destination);
        Path destinationPath = destination.getInProgressPath(params.uri);

        if (Files.exists(destinationPath)){
            response.setStatus(HttpStatus.CONFLICT_409);
            result = false;
        }

        // user has permission
        if (!Root.zebedee.permissions.canEdit(session.email)){
            response.setStatus(HttpStatus.FORBIDDEN_403);
            result = false;
        }

        PathUtils.move(sourcePath, destinationPath);

        return result;
    }

    private Collection getSource(TransferRequest params, HttpServletRequest request) throws  IOException{
        return params.source == null ? Collections.getCollection(request) : Root.zebedee.getCollections().getCollection(params.source);
    }
}
