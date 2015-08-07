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
 * Created by bren on 04/08/15.
 */
@Api
public class BreadCrumb {
    @GET
    public void get(HttpServletRequest request, HttpServletResponse response) throws IOException, ZebedeeException {
        ResponseUtils.sendResponse(new ReadRequestHandler().getParents(request),response);
    }
}
