package com.github.onsdigital.zebedee.logging;

import com.github.onsdigital.logging.builder.ComplexLogMessageBuilder;
import com.github.onsdigital.zebedee.logging.events.ZebedeeLogEvent;
import com.github.onsdigital.zebedee.model.Collection;

/**
 * Created by dave on 5/5/16.
 */
public class ZebedeeLogBuilder extends ComplexLogMessageBuilder {

    public static final String LOG_NAME = "com.github.onsdigital.logging";

    /**
     * Get a {@link ZebedeeLogBuilder} for the specified {@link ZebedeeLogEvent}.
     */
    public static ZebedeeLogBuilder forEvent(ZebedeeLogEvent eventType) {
        return new ZebedeeLogBuilder(eventType);
    }

    private ZebedeeLogBuilder(ZebedeeLogEvent eventType) {
        super(eventType.getDescription());
    }

    public ZebedeeLogBuilder user(String email) {
        addParameter("user", email);
        return this;
    }

    public ZebedeeLogBuilder collectionPath(Collection collection) {
        addParameter("collection", collection.path.toString());
        return this;
    }

    public ZebedeeLogBuilder table(String tableName) {
        addParameter("table", tableName);
        return this;
    }

    public ZebedeeLogBuilder collectionLogDesc(CollectionLogDesc desc) {
        addParameter("collectionLogDesc", desc);
        return this;
    }

    @Override
    public String getLoggerName() {
        return LOG_NAME;
    }

}
