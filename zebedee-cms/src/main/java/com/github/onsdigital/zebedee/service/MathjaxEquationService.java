package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.verification.http.ClientConfiguration;
import com.github.onsdigital.zebedee.verification.http.PooledHttpClient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;

/**
 * Render equations in text by calling the external rendering service.
 */
public class MathjaxEquationService {

    private static PooledHttpClient client;
    private static MathjaxEquationService instance;
    private static final String exportServerUrl = Configuration.getMathjaxServiceUrl();

    //singleton
    private MathjaxEquationService() {
    }

    /**
     * Given an input LaTex equation, generate SVG output.
     *
     * @param input - The string containing the LaTex equations to render.
     * @return
     */
    public static String render(String input) {

        // Only attempt to render the equation if the export server url has been defined in the environment
        if (StringUtils.isNotEmpty(exportServerUrl)) {
            String output = getInstance().sendPost("", input);
            return output;
        }

        // just return the input if the export URL is not defined in the environment.
        System.out.println("Not attempting to render equations - the MATHJAX_EXPORT_SERVER environment variable is not defined.");
        return input;
    }

    private String sendPost(String path, String input) {
        CloseableHttpResponse response = null;
        try {
            return EntityUtils.toString(client.sendPost(path, new HashMap<>(), input).getEntity());
        } catch (IOException e) {
            IOUtils.closeQuietly(response);
            return input;
        }
    }

    private static MathjaxEquationService getInstance() {
        if (instance == null) {
            synchronized (MathjaxEquationService.class) {
                if (instance == null) {
                    instance = new MathjaxEquationService();
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
}
