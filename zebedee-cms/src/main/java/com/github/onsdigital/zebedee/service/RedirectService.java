package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.dis.redirect.api.sdk.model.Redirect;
import com.github.onsdigital.zebedee.json.CollectionRedirect;
import com.github.onsdigital.zebedee.util.slack.Notifier;


import java.io.IOException;
import java.util.List;

/**
 * Provides high level redirect functionality
 */
public interface RedirectService {

    void generateRedirectListForCollection(Collection collection, CollectionReader collectionReader)
        throws IOException, ZebedeeException;
    
    public CollectionRedirect getCollectionRedirect(Redirect redirect) throws ZebedeeException;

    void publishRedirectsForCollection(Collection collection, Notifier notifier) throws IOException;
}
