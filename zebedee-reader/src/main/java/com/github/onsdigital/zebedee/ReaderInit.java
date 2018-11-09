package com.github.onsdigital.zebedee;

import com.github.davidcarboni.restolino.framework.Startup;
import com.github.onsdigital.zebedee.search.client.ElasticSearchClient;
import com.github.onsdigital.zebedee.search.indexing.Indexer;
import com.github.onsdigital.zebedee.search.indexing.SearchBoostTermsResolver;
import org.elasticsearch.client.transport.NoNodeAvailableException;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.search.configuration.SearchConfiguration.getSearchAlias;

public class ReaderInit implements Startup {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void init() {
        logInfo("loading zebedee reader feature flags").log();
        ReaderFeatureFlags.readerFeatureFlags();

        logInfo("initialising zededee reader elasticSearch client").log();
        try {
            ElasticSearchClient.init();
        } catch (IOException e) {
            logError(e, "error initalising zedebee reader elasticSearch client").log();
            throw new RuntimeException("Failed starting elastic search client", e);
        }

        logInfo("loading search index");
        EXECUTOR.submit(loadIndexTask());
    }

    private Callable<Object> loadIndexTask() {
        return () -> {
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
                logError(e, "error attempting to load search index").log();
                throw new RuntimeException("Loading search index failed", e);
            } finally {
                EXECUTOR.shutdown();
            }
        };
    }
}
