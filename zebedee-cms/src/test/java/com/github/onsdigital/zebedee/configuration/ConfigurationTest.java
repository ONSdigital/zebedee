package com.github.onsdigital.zebedee.configuration;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConfigurationTest {

    @Test
    public void shouldGetIfSet() {
        String expectedURI = "test-uri";

        // Given a redirect url env var set
        System.setProperty("REDIRECT_API_URL", expectedURI);

        // When config var is requested
        String redirectAPIURL = Configuration.getRedirectApiUrl();

        // Then it returns the expected value
        assertEquals(expectedURI, redirectAPIURL);
    }

    public void shouldGetDefaultIfNotSet() {
        // Given nothing set
        // When the config var is requested
        String redirectAPIURL = Configuration.getRedirectApiUrl();

        // Then it returns the default value
        assertEquals("http://localhost:29900", redirectAPIURL);
    }
    
}
