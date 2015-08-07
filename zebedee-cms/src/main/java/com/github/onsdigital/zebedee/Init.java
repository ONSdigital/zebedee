package com.github.onsdigital.zebedee;

import com.github.davidcarboni.restolino.framework.Startup;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.reader.api.ReadRequestHandler;
import com.github.onsdigital.zebedee.util.CollectionViewAuthoriser;

/**
 * Created by bren on 31/07/15.
 */
public class Init implements Startup {
    @Override
    public void init() {
        Root.init();
        ReadRequestHandler.setAuthorisationHandler(new CollectionViewAuthoriser());

    }
}
