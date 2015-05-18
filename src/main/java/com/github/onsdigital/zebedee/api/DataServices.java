package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.converter.JSONToFileConverter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import java.io.IOException;


/**
 * On the fly conversion of .json objects to CSV/ JSON-STAT/ yadayada
 *
 * Dataservices is currently part of Zebedee for convenience
 *
 * Later it should be implemented as part of Brian or whatever our data service is called
 *
 * Created by thomasridd on 13/05/15.
 */
@Api
public class DataServices {

    /**
     * Converts a .json object to data
     *
     * Output should be data plus meta-data
     *
     * @param request
     * @param response <ul>
     *                 </ul>
     * @return
     * @throws IOException
     */
    @POST
    public void convertFiles(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // Grab parameters
        String output = request.getParameter("output");
        String input = request.getParameter("input");

        // And write
        JSONToFileConverter.writeRequestJSONToOutputFormat(request, response, input, output);

        return;
    }
}
