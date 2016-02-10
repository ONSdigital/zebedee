package com.github.onsdigital.zebedee.search;

import com.github.davidcarboni.restolino.framework.Startup;
import com.github.onsdigital.zebedee.search.client.ElasticSearchClient;
import com.github.onsdigital.zebedee.search.indexing.Indexer;
import com.github.onsdigital.zebedee.search.indexing.SearchBoostTermsResolver;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by bren on 03/09/15.
 * <p>
 * Search module initialization entry point
 */
public class SearchInit implements Startup {
    @Override
    public void init() {
        try {
            ElasticSearchClient.init();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed starting elastic searc client", e);
        }
        loadIndex();

    }

    private void loadIndex() {
        final ExecutorService thread = Executors.newSingleThreadExecutor();
        thread.submit(() -> {
                    try {
                        SearchBoostTermsResolver.loadTerms();
                        Indexer.getInstance().reload();
                        return null;
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException("Loading search index failed", e);
                    } finally {
                        thread.shutdown();
                    }
                }
        );
    }
}
