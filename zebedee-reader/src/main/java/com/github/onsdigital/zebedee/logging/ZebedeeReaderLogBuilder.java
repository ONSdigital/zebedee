package com.github.onsdigital.zebedee.logging;

import ch.qos.logback.classic.Level;
import com.github.onsdigital.logging.builder.LogMessageBuilder;
import com.github.onsdigital.zebedee.logging.events.ZebedeeReaderLogEvent;

import java.nio.file.Path;

import static com.github.onsdigital.zebedee.logging.events.ZebedeeReaderLogEvent.DEBUG_MESSAGE;
import static com.github.onsdigital.zebedee.logging.events.ZebedeeReaderLogEvent.ELASTIC_SEARCH_DEBUG;
import static com.github.onsdigital.zebedee.logging.events.ZebedeeReaderLogEvent.EXCEPTION;

/**
 * Created by dave on 5/5/16.
 */
public class ZebedeeReaderLogBuilder extends LogMessageBuilder {

    public static final String LOG_NAME = "com.github.onsdigital.logging";

    /**
     * Get a {@link ZebedeeReaderLogBuilder} for the specified {@link ZebedeeReaderLogBuilder}.
     */
    public static ZebedeeReaderLogBuilder logEvent(ZebedeeReaderLogEvent eventType) {
        return new ZebedeeReaderLogBuilder(eventType);
    }

    public static ZebedeeReaderLogBuilder logError(Throwable t) {
        return new ZebedeeReaderLogBuilder(EXCEPTION)
                .addParameter("class", t.getClass().getName())
                .addParameter("exception", t);
    }

    public static ZebedeeReaderLogBuilder debugMessage(String message) {
        return (ZebedeeReaderLogBuilder) new ZebedeeReaderLogBuilder(DEBUG_MESSAGE, Level.DEBUG)
                .addMessage(message);
    }

    public static ZebedeeReaderLogBuilder debugMessage(String message, Level level) {
        return (ZebedeeReaderLogBuilder) new ZebedeeReaderLogBuilder(DEBUG_MESSAGE, level)
                .addMessage(message);
    }

    public static ZebedeeReaderLogBuilder elasticSearchLog(String message) {
        return (ZebedeeReaderLogBuilder) new ZebedeeReaderLogBuilder(ELASTIC_SEARCH_DEBUG, Level.INFO)
                .addMessage(message);
    }


    private ZebedeeReaderLogBuilder(ZebedeeReaderLogEvent eventType) {
        super(eventType.getDescription());
    }

    private ZebedeeReaderLogBuilder(ZebedeeReaderLogEvent eventType, Level level) {
        super(eventType.getDescription(), level);
    }

    public ZebedeeReaderLogBuilder user(String email) {
        addParameter("user", email);
        return this;
    }

    public ZebedeeReaderLogBuilder table(String tableName) {
        addParameter("table", tableName);
        return this;
    }

    public ZebedeeReaderLogBuilder path(Path path) {
        addParameter("path", path.toString());
        return this;
    }

    @Override
    public ZebedeeReaderLogBuilder addParameter(String key, Object value) {
        return (ZebedeeReaderLogBuilder) super.addParameter(key, value);
    }

    public ZebedeeReaderLogBuilder errorContext(String context) {
        addParameter("errorContext", context);
        return this;
    }

    @Override
    public String getLoggerName() {
        return LOG_NAME;
    }

}
