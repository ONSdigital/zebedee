package com.github.onsdigital.zebedee;

import com.github.davidcarboni.restolino.framework.Startup;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReaderFactory;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDaoFactory;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;

/**
 * Created by bren on 31/07/15.
 */
public class Init implements Startup {
    @Override
    public void init() {
        Root.init();
        ZebedeeReader.setCollectionReaderFactory(new ZebedeeCollectionReaderFactory(Root.zebedee));
        CollectionHistoryDaoFactory.initialise();
        logDebug("Zebedee Start up").log();
    }
}
