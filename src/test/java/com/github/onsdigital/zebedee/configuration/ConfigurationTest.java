package com.github.onsdigital.zebedee.configuration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConfigurationTest {
    @Test
    public void shouldReturnDefaultFlorenceUrl() {
        assertEquals("http://localhost:8081", Configuration.getFlorenceUrl());
    }
}
