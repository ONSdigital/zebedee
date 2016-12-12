package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.verification.http.ClientConfiguration;
import com.github.onsdigital.zebedee.verification.http.PooledHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;

/**
 * Render equations in text by calling the external rendering service.
 */
public class EquationService {

    private static final String exportServerUrl = Configuration.getMathjaxServiceUrl();
    private static PooledHttpClient client;
    private static EquationService instance;

    //singleton
    private EquationService() {
    }

    /**
     * Given an input LaTex equation, generate SVG output.
     *
     * @param input - The string containing the LaTex equations to render.
     * @return
     */
    public static EquationServiceResponse render(String input) throws IOException {

        // Only attempt to render the equation if the export server url has been defined in the environment
        if (StringUtils.isNotEmpty(exportServerUrl)) {
            EquationServiceResponse response = getInstance().sendPost("", input);
            return response;
        }

        // just return the input if the export URL is not defined in the environment.
        System.out.println("Not attempting to render equations - the MATHJAX_EXPORT_SERVER environment variable is not defined.");
        return null;
    }

    private static EquationService getInstance() {
        if (instance == null) {
            synchronized (EquationService.class) {
                if (instance == null) {
                    instance = new EquationService();
                    System.out.println("Initializing Mathjax processor http client");
                    client = new PooledHttpClient(Configuration.getMathjaxServiceUrl(), createConfiguration());
                }
            }
        }
        return instance;
    }

    private static ClientConfiguration createConfiguration() {
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setMaxTotalConnection(10);
        configuration.setDisableRedirectHandling(true);
        return configuration;
    }

    private EquationServiceResponse sendPost(String path, String input) throws IOException {
        try (CloseableHttpResponse response = client.sendPost(path, new HashMap<>(), input)) {
            String responseString = EntityUtils.toString(response.getEntity());
            return ContentUtil.deserialise(responseString, EquationServiceResponse.class);
        }
    }
}
