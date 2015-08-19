package com.github.onsdigital.zebedee.reader.api.endpoint;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.api.ReadRequestHandler;
import com.github.onsdigital.zebedee.util.ResponseUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

/**
 * Created by bren on 19/08/15.
 */
@Api
public class FileSize {

    /**
     * Returns file size for requested response
     *
     * @param request
     * @param response
     * @throws IOException
     * @throws ZebedeeException
     */
    @GET
    public void get(HttpServletRequest request, HttpServletResponse response) throws IOException, ZebedeeException {
        try (com.github.onsdigital.zebedee.reader.Resource resource = new ReadRequestHandler().findResource(request)) {
            ResponseUtils.sendResponse(new com.github.onsdigital.zebedee.reader.api.bean.FileSize(resource.getSize()), response);
        }
    }
}