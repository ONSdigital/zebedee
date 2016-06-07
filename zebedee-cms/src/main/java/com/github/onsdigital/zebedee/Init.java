package com.github.onsdigital.zebedee;

import com.github.davidcarboni.restolino.framework.Startup;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReaderFactory;
import com.github.onsdigital.zebedee.persistence.HibernateServiceImpl;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDao;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDaoImpl;
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
        CollectionHistoryDao.setInstance(new CollectionHistoryDaoImpl(HibernateServiceImpl.getInstance()));
        logDebug("Zebedee Start up").log();
    }
}
