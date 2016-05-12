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

    private static final String CLASS = "class";
    private static final String EXCEPTION = "exception";
    private static final String ERROR_CONTEXT = "exception";
    private static final String USER = "user";
    private static final String TABLE = "table";
    private static final String COLLECTION_ID = "collectionId";
    private static final String COLLECTION = "collection";
    private static final String COLLECTION_NAME = "collectionName";
    private static final String COLLECTION_LOG_DESC = "collectionLogDesc";
    private static final String TIME_TAKEN = "timeTaken(ms)";
    private static final String PATH = "path";

    private ZebedeeLogBuilder(String description) {
        super(description);
    }

    private ZebedeeLogBuilder(String description, Level level) {
        super(description, level);
    }

    public static ZebedeeLogBuilder logError(Throwable t) {
        return new ZebedeeLogBuilder(ZEBEDEE_EXCEPTION)
                .addParameter(CLASS, t.getClass().getName())
                .addParameter(EXCEPTION, t);
    }

    public static ZebedeeLogBuilder logError(Throwable t, String errorContext) {
        return new ZebedeeLogBuilder(ZEBEDEE_EXCEPTION)
                .addParameter(ERROR_CONTEXT, errorContext)
                .addParameter(CLASS, t.getClass().getName())
                .addParameter(EXCEPTION, t);
    }

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

    public ZebedeeLogBuilder user(String email) {
        addParameter(USER, email);
        return this;
    }

    public ZebedeeLogBuilder collectionPath(Collection collection) {
        addParameter(COLLECTION, collection.path.toString());
        return this;
    }

    public ZebedeeLogBuilder collectionId(Collection collection) {
        addParameter(COLLECTION_ID, collection.description.id);
        return this;
    }

    public ZebedeeLogBuilder collectionName(Collection collection) {
        addParameter(COLLECTION_NAME, collection.description.name);
        return this;
    }

    public ZebedeeLogBuilder collectionName(String name) {
        addParameter(COLLECTION_NAME, name);
        return this;
    }

    public ZebedeeLogBuilder table(String tableName) {
        addParameter(TABLE, tableName);
        return this;
    }

    public ZebedeeLogBuilder timeTaken(long timeTaken) {
        addParameter(TIME_TAKEN, timeTaken);
        return this;
    }

    public ZebedeeLogBuilder collectionLogDesc(CollectionLogDesc desc) {
        addParameter(COLLECTION_LOG_DESC, desc);
        return this;
    }

    public ZebedeeLogBuilder path(String path) {
        addParameter(PATH, path);
        return this;
    }

    @Override
    public ZebedeeLogBuilder addParameter(String key, Object value) {
        return (ZebedeeLogBuilder) super.addParameter(key, value != null ? value : "");
    }

    @Override
    public String getLoggerName() {
        return LOG_NAME;
    }

}
