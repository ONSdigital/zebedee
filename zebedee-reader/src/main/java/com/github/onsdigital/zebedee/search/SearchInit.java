package com.github.onsdigital.zebedee.search;

import com.github.davidcarboni.restolino.framework.Startup;
import com.github.onsdigital.zebedee.search.client.ElasticSearchClient;
import com.github.onsdigital.zebedee.search.indexing.Indexer;
import com.github.onsdigital.zebedee.search.indexing.SearchBoostTermsResolver;
import org.elasticsearch.client.transport.NoNodeAvailableException;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.search.configuration.SearchConfiguration.getSearchAlias;

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
            throw new RuntimeException("Failed starting elastic search client", e);
        }
        loadIndex();

    }

    private void loadIndex() {
        final ExecutorService thread = Executors.newSingleThreadExecutor();
        thread.submit(() -> {
                    try {
                        logInfo("Search initialisation...").log();

                        logInfo("Loading search terms...").log();
                        SearchBoostTermsResolver.loadTerms();

                        String searchAlias = getSearchAlias();

                        if (Indexer.getInstance().isIndexAvailable(searchAlias)) {
                            logInfo("It looks like the search index already exists. Not attempting to reindex.").log();
                        } else {
                            logInfo("Search index for the website not found. Creating and populating index...").log();
                            long startSearchReindex = System.currentTimeMillis();
                            Indexer.getInstance().reload();
                            long endSearchReindex = System.currentTimeMillis();
                            logInfo("Time taken indexing search: " + ((endSearchReindex - startSearchReindex))).log();
                        }

                        logInfo("Search initialisation complete...").log();

                        return null;
                    } catch (NoNodeAvailableException e) {
                        logError(e, "Failed to communicate with elastic search to index content. Search will not work.").log();
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
