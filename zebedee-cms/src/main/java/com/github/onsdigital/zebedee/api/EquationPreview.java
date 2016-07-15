package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.service.MathjaxEquationService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;

/**
 * Given some Tex equation input, return the equation in SVG format.
 */
@Api
public class EquationPreview {

    @POST
    public String renderEquation(HttpServletRequest request, HttpServletResponse response, String input) {
        return MathjaxEquationService.render(input);
    }
}