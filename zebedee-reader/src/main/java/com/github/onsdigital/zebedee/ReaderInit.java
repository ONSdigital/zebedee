package com.github.onsdigital.zebedee;

import com.github.davidcarboni.restolino.framework.Startup;
import com.github.onsdigital.logging.v2.DPLogger;
import com.github.onsdigital.logging.v2.Logger;
import com.github.onsdigital.logging.v2.LoggerImpl;
import com.github.onsdigital.logging.v2.LoggingException;
import com.github.onsdigital.logging.v2.config.Builder;
import com.github.onsdigital.logging.v2.config.LogConfig;
import com.github.onsdigital.logging.v2.serializer.JacksonLogSerialiser;
import com.github.onsdigital.logging.v2.serializer.LogSerialiser;
import com.github.onsdigital.logging.v2.storage.LogStore;
import com.github.onsdigital.logging.v2.storage.MDCLogStore;
import com.github.onsdigital.zebedee.search.client.ElasticSearchClient;
import com.github.onsdigital.zebedee.search.indexing.Indexer;
import com.github.onsdigital.zebedee.search.indexing.SearchBoostTermsResolver;
import org.elasticsearch.client.transport.NoNodeAvailableException;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.zebedee.search.configuration.SearchConfiguration.getSearchAlias;

public class ReaderInit implements Startup {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void init() {
        try {
            Logger logger = new LoggerImpl("com.github.onsdigital.zebedee.reader");
            LogSerialiser serialiser = new JacksonLogSerialiser();
            LogStore logStore = new MDCLogStore(serialiser);
            LogConfig config = new Builder()
                    .dataNamespace("zebedee.reader.data")
                    .logger(logger)
                    .logStore(logStore)
                    .serialiser(serialiser)
                    .create();

            DPLogger.init(config);
        } catch (LoggingException ex) {
            // TODO
        }

        info().log("loading zebedee reader feature flags");
        ReaderFeatureFlags.readerFeatureFlags();

        info().log("initialising zededee reader elasticSearch client");
        try {
            ElasticSearchClient.init();
        } catch (IOException e) {
            error().logAndThrow(new RuntimeException(e), "error initalising zedebee reader elasticSearch client");
        }

        info().log("loading search index");
        EXECUTOR.submit(loadIndexTask());
    }

    private Callable<Object> loadIndexTask() {
        return () -> {
            try {
                info().log("search initialisation...");

                info().log("Loading search terms...");
                SearchBoostTermsResolver.loadTerms();

                String searchAlias = getSearchAlias();

                if (Indexer.getInstance().isIndexAvailable(searchAlias)) {
                    info().log("It looks like the search index already exists. Not attempting to reindex.");
                } else {
                    info().log("Search index for the website not found creating and populating index...");
                    long startSearchReindex = System.currentTimeMillis();
                    Indexer.getInstance().reload();
                    long endSearchReindex = System.currentTimeMillis();
                    info().data("time_taken", (endSearchReindex - startSearchReindex))
                            .log("indexing compeleted");
                }

                info().log("Search initialisation complete...");

                return null;
            } catch (NoNodeAvailableException e) {
                error().logException(e, "Failed to communicate with elastic search to index content. Search will not work.");
                return null;
            } catch (Exception e) {
                error().logAndThrow(new RuntimeException("Loading search index failed", e),
                        "error attempting to load search index");
                return null;
            } finally {
                EXECUTOR.shutdown();
            }
        };
    }
}
