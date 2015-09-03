package com.github.onsdigital.zebedee.search;

import com.github.davidcarboni.restolino.framework.Startup;
import com.github.onsdigital.zebedee.search.client.ElasticSearchClient;
import com.github.onsdigital.zebedee.search.indexing.Indexer;

import java.io.IOException;

/**
 * Created by bren on 03/09/15.
 * <p>
 * Search module initialization entry point
 */
public class SearchInit implements Startup {
    @Override
    public void init() {
        ElasticSearchClient.init();
        try {
            Indexer.getInstance().loadIndex();
        } catch (IOException e) {
            throw new RuntimeException("Loading search index failed", e);
        }
    }
}
