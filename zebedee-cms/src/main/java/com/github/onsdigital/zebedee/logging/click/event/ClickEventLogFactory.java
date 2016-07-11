package com.github.onsdigital.zebedee.logging.click.event;

import ch.qos.logback.classic.Level;
import com.github.onsdigital.logging.builder.LogMessageBuilder;
import com.github.onsdigital.zebedee.model.ClickEvent;
import org.apache.commons.lang3.StringUtils;

/**
 * Provides static access to {@link ClickEventLogBuilder} - you could argue its unnecessary / a bit over
 * engineered however it provides a layer of abstraction making testing code that uses this considerably easier
 * (currently zebedee test coverage is terrible to every little helps).
 */
public class ClickEventLogFactory {

    private static ClickEventLogFactory instance = null;
    private static final String LOGGER_NAME = "com.github.onsdigital.logging.click.events";
    private static final String DESCRIPTION = "Florence click event";
    private static final String TRIGGER = "trigger";
    private static final String COLLECTION = "collection";
    private static final String USER = "user";

    public static ClickEventLogFactory getInstance() {
        if (instance == null) {
            instance = new ClickEventLogFactory();
        }
        return instance;
    }

    private ClickEventLogFactory() {
        // use static instance method.
    }

    public void log(ClickEvent details) {
        new ClickEventLogBuilder(details).log();
    }

    /**
     * Click Event log bulder implementation.
     */
    private class ClickEventLogBuilder extends LogMessageBuilder {
        private boolean hasContentToLog = false;

        private ClickEventLogBuilder(ClickEvent details) {
            super(DESCRIPTION, Level.DEBUG);

            if (details != null && StringUtils.isNotEmpty(details.getUser())) {
                this.addParameter(USER, details.getUser());
                hasContentToLog = true;
            }

            if (details != null && details.getTrigger() != null) {
                this.addParameter(TRIGGER, details.getTrigger());
                hasContentToLog = true;
            }

            if (details != null && details.getCollection() != null) {
                this.addParameter(COLLECTION, details.getCollection());
                hasContentToLog = true;
            }
        }

        @Override
        public void log() {
            if (hasContentToLog) {
                super.log();
            }
        }

        @Override
        public String getLoggerName() {
            return LOGGER_NAME;
        }
    }
}
