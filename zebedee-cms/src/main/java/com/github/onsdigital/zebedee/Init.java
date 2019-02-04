package com.github.onsdigital.zebedee;

import com.github.davidcarboni.restolino.framework.Startup;
import com.github.onsdigital.logging.v2.DPLogger;
import com.github.onsdigital.logging.v2.config.Config;
import com.github.onsdigital.logging.v2.config.Builder;
import com.github.onsdigital.logging.v2.serializer.JacksonLogSerialiser;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.configuration.CMSFeatureFlags;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReaderFactory;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDaoFactory;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;
import org.slf4j.LoggerFactory;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * Created by bren on 31/07/15.
 */
public class Init implements Startup {
    @Override
    public void init() {
        Config config = new Builder()
                .logger(LoggerFactory.getLogger("com.zebedee.app"))
                .serialiser(new JacksonLogSerialiser())
                .dataNamespace("zebedee.data")
                .create();
        DPLogger.init(config);

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
