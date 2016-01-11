package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.reader.util.ReaderResponseResponseUtils;
import com.github.onsdigital.zebedee.reader.util.RequestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import java.io.IOException;

/**
 * Created by bren on 01/07/15.
 * <p>
 * Starts download for requested file in content directory
 */
@Api
public class File {
    @GET
    public Object post(@Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException, ZebedeeException {
        try(Resource resource = RequestUtils.getResource(request)) {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + resource.getName() + "\"");
            ReaderResponseResponseUtils.sendResponse(resource, response);
            return null;
        }
    }
}
