package com.github.onsdigital.zebedee.logging;

import ch.qos.logback.classic.Level;
import com.github.onsdigital.logging.builder.LogMessageBuilder;
import com.github.onsdigital.zebedee.model.Collection;

/**
 * Created by dave on 5/5/16.
 */
public class ZebedeeLogBuilder extends LogMessageBuilder {

    public static final String LOG_NAME = "com.github.onsdigital.logging";
    private static final String ZEBEDEE_EXCEPTION = "Zebedee Exception";

    public static ZebedeeLogBuilder logError(Throwable t) {
        return new ZebedeeLogBuilder(ZEBEDEE_EXCEPTION)
                .addParameter("class", t.getClass().getName())
                .addParameter("exception", t);
    }

    public static ZebedeeLogBuilder logError(Throwable t, String errorContext) {
        return new ZebedeeLogBuilder(ZEBEDEE_EXCEPTION)
                .addParameter("errorContext", errorContext)
                .addParameter("class", t.getClass().getName())
                .addParameter("exception", t);
    }

    //addParameter("errorContext", context);

    /**
     * Log a debug level message.
     */
    public static ZebedeeLogBuilder logDebug(String message) {
        return new ZebedeeLogBuilder(message, Level.DEBUG);
    }

    /**
     * Log an info level message.
     */
    public static ZebedeeLogBuilder logInfo(String message) {
        return new ZebedeeLogBuilder(message, Level.INFO);
    }

    private ZebedeeLogBuilder(String description) {
        super(description);
    }

    private ZebedeeLogBuilder(String description, Level level) {
        super(description, level);
    }

    public ZebedeeLogBuilder user(String email) {
        addParameter("user", email);
        return this;
    }

    public ZebedeeLogBuilder collectionPath(Collection collection) {
        addParameter("collection", collection.path.toString());
        return this;
    }

    public ZebedeeLogBuilder collectionId(Collection collection) {
        addParameter("collectionId", collection.description.id);
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

    public ZebedeeLogBuilder timeTaken(long timeTaken) {
        addParameter("timeTaken(MS)", timeTaken);
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

    @Override
    public String getLoggerName() {
        return LOG_NAME;
    }

}
