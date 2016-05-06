package com.github.onsdigital.zebedee.logging;

import ch.qos.logback.classic.Level;
import com.github.onsdigital.logging.builder.LogMessageBuilder;
import com.github.onsdigital.zebedee.logging.events.ZebedeeLogEvent;
import com.github.onsdigital.zebedee.model.Collection;

import static com.github.onsdigital.zebedee.logging.events.ZebedeeLogEvent.DEBUG_MESSAGE;
import static com.github.onsdigital.zebedee.logging.events.ZebedeeLogEvent.EXCEPTION;

/**
 * Created by dave on 5/5/16.
 */
public class ZebedeeLogBuilder extends LogMessageBuilder {

    public static final String LOG_NAME = "com.github.onsdigital.logging";

    /**
     * Get a {@link ZebedeeLogBuilder} for the specified {@link ZebedeeLogEvent}.
     */
    public static ZebedeeLogBuilder logEvent(ZebedeeLogEvent eventType) {
        return new ZebedeeLogBuilder(eventType);
    }

    public static ZebedeeLogBuilder logError(Exception ex) {
        return new ZebedeeLogBuilder(EXCEPTION)
                .addParameter("class", ex.getClass().getName())
                .addParameter("exception", ex);
    }

    public static ZebedeeLogBuilder debugMessage(String message) {
        return new ZebedeeLogBuilder(DEBUG_MESSAGE, Level.DEBUG).addParameter("message", message);
    }

    private ZebedeeLogBuilder(ZebedeeLogEvent eventType) {
        super(eventType.getDescription());
    }

    private ZebedeeLogBuilder(ZebedeeLogEvent eventType, Level level) {
        super(eventType.getDescription(), level);
    }

    public ZebedeeLogBuilder user(String email) {
        addParameter("user", email);
        return this;
    }

    public ZebedeeLogBuilder collectionPath(Collection collection) {
        addParameter("collection", collection.path.toString());
        return this;
    }

    public ZebedeeLogBuilder collectionName(Collection collection) {
        addParameter("collectionName", collection.description.name);
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
    public ZebedeeLogBuilder addParameter(String key, Object value) {
        return (ZebedeeLogBuilder)super.addParameter(key, value);
    }

    public ZebedeeLogBuilder errorContext(String context) {
        addParameter("errorContext", context);
        return this;
    }

    @Override
    public String getLoggerName() {
        return LOG_NAME;
    }

}
