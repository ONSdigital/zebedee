package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import org.apache.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;

/**
 * health endpoint always returns 200 OK.
 */
@Api
public class Health {

    @GET
    public void getHealth(HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(HttpStatus.SC_OK);
    }
}
