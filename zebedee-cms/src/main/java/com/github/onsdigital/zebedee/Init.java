package com.github.onsdigital.zebedee;

import com.github.davidcarboni.restolino.framework.Startup;
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

    @Override
    public void init() {
        info().log("inside CMS INIT");

        info().log("loading CMS feature flags");
        CMSFeatureFlags.cmsFeatureFlags();

        info().log("Root.init()");
        Root.init();

        info().log("ZebedeeReader.setCollectionReaderFactory");
        ZebedeeReader.setCollectionReaderFactory(new ZebedeeCollectionReaderFactory(Root.zebedee));

        info().log("CollectionHistoryDaoFactory.initialise();");
        CollectionHistoryDaoFactory.initialise();
        info().log("Zebedee Start up");
    }
}
