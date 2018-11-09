package com.github.onsdigital.zebedee;

import com.github.davidcarboni.restolino.framework.Startup;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logInfo;

public class ReaderInit implements Startup {

    @Override
    public void init() {
        logInfo("running zebedee reader init function").log();
    }
}
