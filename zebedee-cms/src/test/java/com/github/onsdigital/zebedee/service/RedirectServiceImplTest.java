package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.dis.redirect.api.sdk.RedirectClient;
import com.github.onsdigital.dis.redirect.api.sdk.model.Redirect;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RedirectServiceImplTest {
    @Mock
    RedirectClient mockRedirectAPI;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetRedirect() throws Exception {
        // Given the API returns a redirect for a key
        when(mockRedirectAPI.getRedirect("/from")).thenReturn(new Redirect("/from", "/to"));
        RedirectService redirectService = new RedirectServiceImpl(mockRedirectAPI);

        // When getRedirect is called
        Redirect redirect = redirectService.getRedirect("/from");

        // Then the expected Redirect should be returned
        assertEquals("/to", redirect.getTo());
    }
}

