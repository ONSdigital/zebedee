package com.github.onsdigital.zebedee;

import com.github.davidcarboni.restolino.framework.Startup;

public class Init implements Startup {
    @Override
    public void init() {
        ReaderFeatureFlags.readerFeatureFlags();
    }
}
