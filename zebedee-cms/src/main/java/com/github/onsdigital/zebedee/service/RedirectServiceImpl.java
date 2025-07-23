package com.github.onsdigital.zebedee.service;

import java.io.IOException;

import com.github.onsdigital.dis.redirect.api.sdk.RedirectClient;
import com.github.onsdigital.dis.redirect.api.sdk.exception.BadRequestException;
import com.github.onsdigital.dis.redirect.api.sdk.exception.RedirectAPIException;
import com.github.onsdigital.dis.redirect.api.sdk.exception.RedirectNotFoundException;
import com.github.onsdigital.dis.redirect.api.sdk.model.Redirect;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;

/**
 * Redirect related services
 */
public class RedirectServiceImpl implements RedirectService {

    private RedirectClient redirectClient;

    /**
     * Construct a new instance of the the redirect service
     *
     * @param redirectClient An instance of an Redirect API client to be used by the service
     */
    public RedirectServiceImpl(RedirectClient redirectClient) {
        this.redirectClient = redirectClient;
    }


    public Redirect getRedirect(String redirectPath)
        throws IOException, BadRequestException, RedirectNotFoundException,
        RedirectAPIException {
            info()
                .data("redirectPath", redirectPath)
                .log("getting redirect for path");

            return redirectClient.getRedirect(redirectPath);
        }
}
