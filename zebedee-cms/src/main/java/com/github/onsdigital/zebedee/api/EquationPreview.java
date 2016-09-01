package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.service.EquationService;
import com.github.onsdigital.zebedee.service.EquationServiceResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

/**
 * Given some Tex equation input, return the equation in SVG format.
 */
@Api
public class EquationPreview {

    @POST
    public String renderEquation(HttpServletRequest request, HttpServletResponse response, String input) throws IOException {
        EquationServiceResponse equationServiceResponse = EquationService.render(input);
        String rendered = equationServiceResponse == null ? input : equationServiceResponse.svg;
        return rendered;
    }
}