package com.github.onsdigital.zebedee;

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
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.configuration.CMSFeatureFlags;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReaderFactory;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDaoFactory;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

/**
 * Created by bren on 31/07/15.
 */
public class Init implements Startup {

    private static final String FORMAT_LOGS_KEY = "FORMAT_LOGGING";

    @Override
    public void init() {
        LogSerialiser serialiser = getLogSerialiser();
        LogStore store = new MDCLogStore(serialiser);
        Logger logger = new LoggerImpl("zebedee");

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

        info().log("loading CMS feature flags");
        CMSFeatureFlags.cmsFeatureFlags();

        Root.init();
        ZebedeeReader.setCollectionReaderFactory(new ZebedeeCollectionReaderFactory(Root.zebedee));

        CollectionHistoryDaoFactory.initialise();
        info().log("zebedee cms start up completed");
    }

    private LogSerialiser getLogSerialiser() {
        boolean formatLogging = Boolean.valueOf(System.getenv(FORMAT_LOGS_KEY));
        return new JacksonLogSerialiser(formatLogging);
    }
}
