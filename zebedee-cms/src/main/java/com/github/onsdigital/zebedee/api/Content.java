package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import java.io.IOException;
import java.io.InputStream;

@Api
public class Content {

    /**
     * Retrieves file content for the endpoint <code>/Content/[CollectionName]/?uri=[uri]</code>
     * <p/>
     * <p>This may be working content from the collection. Defaults to current website content</p>
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response No respons message.
     * @return
     * @throws IOException           If an error occurs in processing data, typically to the filesystem, but also on the HTTP connection.
     * @throws NotFoundException     If the requested URI does not exist in the collection.
     * @throws BadRequestException   IF the request cannot be completed because of a problem with request parameters
     * @throws UnauthorizedException If the user does not have viewer permission.
     */
    @GET
    public void read(HttpServletRequest request, HttpServletResponse response) throws IOException, NotFoundException, BadRequestException, UnauthorizedException {

        Session session = Root.zebedee.sessions.get(request);
        Collection collection = Collections.getCollection(request);
        String uri = request.getParameter("uri");

        //Resolve references to other content types by reading referenced content into requested content
        boolean resolveReferences = request.getParameter("resolve") != null;
        System.out.println("Reading content under " + uri + " Resolve references: " + resolveReferences);
        Root.zebedee.collections.readContent(collection, uri, session, response);
    }

    /**
     * Posts file content to the endpoint <code>/Content/[CollectionName]/?uri=[uri]</code>
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     *                 <ul>Body should contain
     *                 <li>Page content - JSON Serialized content</li>
     *                 <li>File Upload - A multipart content object with part "file" as binary data </li>
     *                 </ul>
     * @param response Returns true or false according to whether the URI was written.
     * @return true/false
     * @throws IOException           If an error occurs in processing data, typically to the filesystem, but also on the HTTP connection.
     * @throws BadRequestException   IF the request cannot be completed because of a problem with request parameters
     * @throws UnauthorizedException If the user does not have publisher permission.
     * @throws ConflictException     If the URI is being edited in another collection
     * @throws NotFoundException     If the file cannot be edited for some other reason
     */
    @POST
    public boolean saveContent(HttpServletRequest request, HttpServletResponse response) throws IOException, NotFoundException, BadRequestException, UnauthorizedException, ConflictException {

        // We have to get the request InputStream before reading any request parameters
        // otherwise the call to get a request parameter will actually consume the body:
        InputStream requestBody = request.getInputStream();

        Session session = Root.zebedee.sessions.get(request);

        Collection collection = Collections.getCollection(request);

        String uri = request.getParameter("uri");
        Boolean overwriteExisting = BooleanUtils.toBoolean(StringUtils.defaultIfBlank(request.getParameter("overwriteExisting"), "true"));

        if (overwriteExisting) {
            Root.zebedee.collections.writeContent(collection, uri, session, request, requestBody);
        } else {
            Root.zebedee.collections.createContent(collection, uri, session, request, requestBody);
        }

        return true;
    }

    /**
     * Deletes file content from the endpoint <code>/Content/[CollectionName]/?uri=[uri]</code>
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response Returns true or false according to whether the URI was deleted.
     * @return
     * @throws IOException           If an error occurs in processing data, typically to the filesystem, but also on the HTTP connection.
     * @throws BadRequestException   IF the request cannot be completed because of a problem with request parameters
     * @throws NotFoundException     If the requested URI does not exist in the collection.
     * @throws UnauthorizedException If the user does not have publisher permission.
     * @throws ConflictException     If the URI is being edited in another collection
     */
    @DELETE
    public boolean delete(HttpServletRequest request, HttpServletResponse response) throws IOException, BadRequestException, NotFoundException, UnauthorizedException {

        Session session = Root.zebedee.sessions.get(request);

        Collection collection = Collections.getCollection(request);
        String uri = request.getParameter("uri");

        return Root.zebedee.collections.deleteContent(collection, uri, session);
    }
}
