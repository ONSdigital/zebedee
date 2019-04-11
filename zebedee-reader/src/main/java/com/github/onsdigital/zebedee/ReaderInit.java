package com.github.onsdigital.zebedee;

import ch.qos.logback.classic.LoggerContext;
import com.github.davidcarboni.restolino.framework.Startup;
import com.github.onsdigital.logging.v2.DPLogger;
import com.github.onsdigital.logging.v2.Logger;
import com.github.onsdigital.logging.v2.LoggerImpl;
import com.github.onsdigital.logging.v2.LoggingException;
import com.github.onsdigital.logging.v2.config.Builder;
import com.github.onsdigital.logging.v2.serializer.JacksonLogSerialiser;
import com.github.onsdigital.logging.v2.serializer.LogSerialiser;
import com.github.onsdigital.logging.v2.storage.LogStore;
import com.github.onsdigital.logging.v2.storage.MDCLogStore;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.search.client.ElasticSearchClient;
import com.github.onsdigital.zebedee.search.indexing.Indexer;
import com.github.onsdigital.zebedee.search.indexing.SearchBoostTermsResolver;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.onsdigital.zebedee.logging.ReaderLogger.error;
import static com.github.onsdigital.zebedee.logging.ReaderLogger.info;
import static com.github.onsdigital.zebedee.search.configuration.SearchConfiguration.getSearchAlias;

public class ReaderInit implements Startup {

    private static final String FORMAT_LOGS_KEY = "FORMAT_LOGGING";
    private static final String DEFAULT_LOGGER_NAME_KEY = "default.logger.name";
    private static final String READER_LOGGER_NAME = "zebedee";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void init() {
        if (initReaderLogger()) {
            LogSerialiser serialiser = getLogSerialiser();
            LogStore store = new MDCLogStore(serialiser);
            Logger logger = new LoggerImpl(READER_LOGGER_NAME);

            try {
                DPLogger.init(new Builder()
                        .serialiser(serialiser)
                        .logStore(store)
                        .logger(logger)
                        .create());
            } catch (LoggingException ex) {
                System.err.println(ex);
                System.exit(1);
            }
        }

        info().log("loading zebedee reader configuration");
        ReaderConfiguration.get();

        info().log("initialising zededee reader elasticSearch client");
        try {
            ElasticSearchClient.init();
        } catch (IOException e) {
            throw error().logException(new RuntimeException(e),
                    "error initalising zedebee reader elasticSearch client");
        }

        info().log("loading search index");
        EXECUTOR.submit(loadIndexTask());
    }

    private Callable<Object> loadIndexTask() {
        return () -> {
            try {
                info().log("Search initialisation...");

                info().log("Loading search terms...");
                SearchBoostTermsResolver.loadTerms();

                String searchAlias = getSearchAlias();

                if (Indexer.getInstance().isIndexAvailable(searchAlias)) {
                    info().log("It looks like the search index already exists. Not attempting to reindex.");
                } else {
                    info().log("Search index for the website not found. Creating and populating index...");
                    long startSearchReindex = System.currentTimeMillis();
                    Indexer.getInstance().reload();
                    long endSearchReindex = System.currentTimeMillis();
                    info().log("Time taken indexing search: " + ((endSearchReindex - startSearchReindex)));
                }

                info().log("Search initialisation complete...");

                return null;
            } catch (NoNodeAvailableException e) {
                error().logException(e, "Failed to communicate with elastic search to index content. Search will not work.");
                return null;
            } catch (Exception e) {
                throw new RuntimeException(error().logException(e, "error attempting to load search index"));
            } finally {
                EXECUTOR.shutdown();
            }
        };
    }

    private boolean initReaderLogger() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        String defaultName = loggerContext.getProperty(DEFAULT_LOGGER_NAME_KEY);
        String loggerName = DPLogger.logConfig().getLogger().getName();
        return StringUtils.equals(defaultName, loggerName);
    }

    private LogSerialiser getLogSerialiser() {
        boolean formatLogging = Boolean.valueOf(System.getenv(FORMAT_LOGS_KEY));
        return new JacksonLogSerialiser(formatLogging);
    }
}
