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
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.configuration.CMSFeatureFlags;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReaderFactory;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDaoFactory;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * Created by bren on 31/07/15.
 */
public class Init implements Startup {

    @Override
    public void init() {

        try {
            LogSerialiser serialiser = new JacksonLogSerialiser(true);
            LogStore logStore = new MDCLogStore(serialiser);
            Logger logger = new LoggerImpl("com.zebedee.app");

            LogConfig config = new Builder()
                    .logger(logger)
                    .dataNamespace("app.data")
                    .logStore(logStore)
                    .serialiser(serialiser)
                    .create();

            DPLogger.init(config);

        } catch (LoggingException ex) {
            System.err.println("failed to init DP Logger");
            System.exit(1);
        }

        logInfo("inside CMS INIT").log();

        logInfo("loading CMS feature flags").log();
        CMSFeatureFlags.cmsFeatureFlags();

        logInfo("Root.init()").log();
        Root.init();

        logInfo("ZebedeeReader.setCollectionReaderFactory").log();
        ZebedeeReader.setCollectionReaderFactory(new ZebedeeCollectionReaderFactory(Root.zebedee));

        logInfo("CollectionHistoryDaoFactory.initialise();").log();
        CollectionHistoryDaoFactory.initialise();
        logDebug("Zebedee Start up").log();
    }
}
