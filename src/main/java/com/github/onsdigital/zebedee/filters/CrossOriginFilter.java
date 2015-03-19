package com.github.onsdigital.zebedee.filters;

import com.github.davidcarboni.restolino.framework.Filter;
import com.github.onsdigital.zebedee.configuration.Configuration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

public class CrossOriginFilter implements Filter {

    private static final List<String> trustedDomains = Arrays.asList(Configuration.getFlorenceUrl());

    /**
     * This filter adds required headers to the Http response allowing cross origin calls.
     * There is no configuration to enable this filter, it is recognised automatically at run time.
     *
     * @param request
     * @param response
     * @return
     */
    @Override
    public boolean filter(HttpServletRequest request, HttpServletResponse response) {

        String origin = request.getHeader("origin");

        if (trustedDomains.contains(origin)) {
            response.addHeader("Access-Control-Allow-Origin", origin);
            response.addHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
            response.addHeader("Access-Control-Allow-Credentials", "true");
        }

        return true;
    }
}
