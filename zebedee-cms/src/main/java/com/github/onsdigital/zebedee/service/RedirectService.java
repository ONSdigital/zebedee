package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.dis.redirect.api.sdk.exception.RedirectAPIException;
import com.github.onsdigital.dis.redirect.api.sdk.exception.RedirectNotFoundException;
import com.github.onsdigital.dis.redirect.api.sdk.exception.BadRequestException;
import com.github.onsdigital.dis.redirect.api.sdk.model.Redirect;

import java.io.IOException;

/**
 * Provides high level redirect functionality
 */
public interface RedirectService {

    /**
     * Get a redirect
     */
    Redirect getRedirect(String redirectID)
        throws IOException, BadRequestException, RedirectNotFoundException,
        RedirectAPIException;
}
