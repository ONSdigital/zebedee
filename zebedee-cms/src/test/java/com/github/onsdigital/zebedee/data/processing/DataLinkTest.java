package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.configuration.Configuration;
import org.junit.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by thomasridd on 1/19/16.
 */
public class DataLinkTest {

    /**
     * Brian connectivity
     */
    @Test
    public void csdbUri_whenEnvVariablesAreSet_givesURIBasedOnEnvVariable() throws Exception {
        // Given
        // we set our own brian_url
        Map<String, String> envNew = new HashMap<>();
        envNew.put("brian_url", "/csdbURIShouldComeFromEnvVariable");

        DataLinkBrian dataLink = new DataLinkBrian();
        dataLink.env = envNew;


        // When
        // we get the service csdbURI
        URI uri = dataLink.csdbURI();

        // Then
        // we expect a standard response
        assertEquals("/csdbURIShouldComeFromEnvVariable/Services/ConvertCSDB", uri.toString());
    }
    @Test
    public void csdbUri_whenEnvVariablesNotSet_givesURIBasedOnConfigurationDefault() throws Exception {
        // Given
        // we set no brian_url
        DataLinkBrian dataLink = new DataLinkBrian();
        dataLink.env = new HashMap<>();

        // When
        // we get the service csdbURI
        URI uri = dataLink.csdbURI();

        // Then
        // we expect a standard response
        assertEquals(Configuration.getBrianUrl() + "/Services/ConvertCSDB", uri.toString());
    }
    @Test
    public void csvUri_whenEnvVariablesAreSet_givesURIBasedOnEnvVariable() throws Exception {
        // Given
        // we set our own brian_url
        Map<String, String> envNew = new HashMap<>();
        envNew.put("brian_url", "/csvURIShouldComeFromEnvVariable");

        DataLinkBrian dataLink = new DataLinkBrian();
        dataLink.env = envNew;


        // When
        // we get the service csdbURI
        URI uri = dataLink.csvURI();

        // Then
        // we expect a standard response
        assertEquals("/csvURIShouldComeFromEnvVariable/Services/ConvertCSV", uri.toString());
    }
    @Test
    public void csvUri_whenEnvVariablesNotSet_givesURIBasedOnConfigurationDefault() throws Exception {
        // Given
        // we set no brian_url
        DataLinkBrian dataLink = new DataLinkBrian();
        dataLink.env = new HashMap<>();

        // When
        // we get the service csvURI
        URI uri = dataLink.csvURI();

        // Then
        // we expect a standard response
        assertEquals(Configuration.getBrianUrl() + "/Services/ConvertCSV", uri.toString());
    }

}