package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.csdb.CsdbImporter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

@Api
public class CsdbKey {
    @GET
    public String getPublicKey(HttpServletRequest request, HttpServletResponse response) throws IOException, ZebedeeException {
        return Root.zebedee.getApplicationKeys().getEncodedPublicKey(CsdbImporter.APPLICATION_KEY_ID);
    }
}
