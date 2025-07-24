package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.dis.redirect.api.sdk.model.Redirect;

/**
 * A no-op RedirectService that does nothing. This is used for the case where redirect API
 * is disabled via the feature flags.
 */
public class NoOpRedirectService implements RedirectService {

    @Override
    public Redirect getRedirect(String redirectPath) {
        return new Redirect();
    }
}
