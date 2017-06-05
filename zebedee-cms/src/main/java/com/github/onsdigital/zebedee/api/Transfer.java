package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.json.TransferRequest;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.PathUtils;
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

    /**
     * Moves files between collections using the endpoint <code>/Transfer/[CollectionName]</code>
     *
     * @param request This should contain a X-Florence-Token header for the current session
     * @param response <ul>
     *                 <li>If the either collection does not exist:  {@link HttpStatus#NOT_FOUND_404}</li>
     *                 <li>If the file does not exist:  {@link HttpStatus#NOT_FOUND_404}</li>
     *                 <li>If user not authorised to transfer:  {@link HttpStatus#UNAUTHORIZED_401}</li>
     *                 <li>A file with that name already exists in the second collection:  {@link HttpStatus#CONFLICT_409}</li>
     * @param params A {@link TransferRequest} object
     * @return success true/false
     * @throws IOException
     */
    @POST
    public boolean move(HttpServletRequest request, HttpServletResponse response, TransferRequest params) throws IOException {
        boolean result = true;


        // user has permission
        Session session = Root.zebedee.getSessionsService().get(request);
        if (!Root.zebedee.getPermissionsService().canEdit(session.getEmail())){
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return false;
        }

        // get the source collection
        Collection source = getSource(params,request);
        if(source == null) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return false;
        }

        Path sourcePath = source.find(params.uri);
        if (Files.notExists(sourcePath)) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return false;
        }

        // get the destination file
        Collection destination = Root.zebedee.getCollections().list().getCollection(params.destination);
        if (destination == null) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return false;
        }

        Path destinationPath = destination.getInProgressPath(params.uri);

        if (Files.exists(destinationPath)){
            response.setStatus(HttpStatus.CONFLICT_409);
            return false;
        }

        PathUtils.moveFilesInDirectory(sourcePath, destinationPath);

        Audit.Event.COLLECTION_TRANSFERRED.parameters()
                .host(request)
                .fromTo(params.uri, params.destination)
                .user(session.getEmail())
                .log();
        return true;
    }

    private Collection getSource(TransferRequest params, HttpServletRequest request) throws  IOException{
        return params.source == null ? Collections.getCollection(request) : Root.zebedee.getCollections().list().getCollection(params.source);
    }
}
