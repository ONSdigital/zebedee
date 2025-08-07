package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.dis.redirect.api.sdk.model.Redirect;
import com.github.onsdigital.zebedee.json.CollectionRedirect;

import java.io.IOException;
import java.util.List;

/**
 * A no-op RedirectService that does nothing. This is used for the case where redirect API
 * is disabled via the feature flags.
 */
public class NoOpRedirectService implements RedirectService {

    @Override
    public void generateRedirectListForCollection(Collection collection, CollectionReader collectionReader) {
        // NoOp implementation
    }

    @Override
    public CollectionRedirect getCollectionRedirect(Redirect redirect){
        return null;
    }

    @Override
    public void publishRedirectsForCollection(List<CollectionRedirect> redirects, String collectionId) throws IOException {
        // NoOp implementation
    }
}
