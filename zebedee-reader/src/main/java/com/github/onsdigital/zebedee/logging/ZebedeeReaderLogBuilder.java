package com.github.onsdigital.zebedee.logging;

import ch.qos.logback.classic.Level;
import com.github.onsdigital.logging.builder.LogMessageBuilder;

import java.nio.file.Path;

/**
 * Created by dave on 5/5/16.
 */
public class ZebedeeReaderLogBuilder extends LogMessageBuilder {

    private static final String LOG_NAME = "com.github.onsdigital.logging";

    private static final String ELASTIC_SEARCH_PREFIX = "[ElasticSearch log] ";
    private static final String ZEBEDEE_READER_EXCEPTION = "Zebedee Reader Exception";
    private static final String CLASS = "class";
    private static final String EXCEPTION = "exception";
    private static final String ERROR_CONTEXT = "exception";
    private static final String USER = "user";
    private static final String TABLE = "table";
    private static final String CDID = "CDID";
    private static final String PATH = "path";
    private static final String URI = "uri";
    private static final String COLLECTION_ID = "collectionId";

    public static ZebedeeReaderLogBuilder logError(Throwable t) {
        return new ZebedeeReaderLogBuilder(ZEBEDEE_READER_EXCEPTION)
                .addParameter(CLASS, t.getClass().getName())
                .addParameter(EXCEPTION, t);
    }

    public static ZebedeeReaderLogBuilder logError(Throwable t, String errorContext) {
        return new ZebedeeReaderLogBuilder(ZEBEDEE_READER_EXCEPTION)
                .addParameter(ERROR_CONTEXT, errorContext)
                .addParameter(CLASS, t.getClass().getName())
                .addParameter(EXCEPTION, t);
    }

    public static ZebedeeReaderLogBuilder logDebug(String message) {
        return new ZebedeeReaderLogBuilder(message, Level.DEBUG);
    }

    public static ZebedeeReaderLogBuilder logInfo(String message) {
        return new ZebedeeReaderLogBuilder(message, Level.INFO);
    }


    public static ZebedeeReaderLogBuilder elasticSearchLog(String message) {
        return (ZebedeeReaderLogBuilder) new ZebedeeReaderLogBuilder(ELASTIC_SEARCH_PREFIX + message, Level.INFO)
                .addMessage(message);
    }

    private ZebedeeReaderLogBuilder(String description) {
        super(description);
    }

    private ZebedeeReaderLogBuilder(String description, Level level) {
        super(description, level);
    }

    public ZebedeeReaderLogBuilder user(String email) {
        addParameter(USER, email);
        return this;
    }

    public ZebedeeReaderLogBuilder table(String tableName) {
        addParameter(TABLE, tableName);
        return this;
    }

    public ZebedeeReaderLogBuilder cdid(String cdid) {
        addParameter(CDID, cdid);
        return this;
    }

    public ZebedeeReaderLogBuilder path(Path path) {
        addParameter(PATH, path.toString());
        return this;
    }

    public ZebedeeReaderLogBuilder uri(String uri) {
        addParameter(URI, uri);
        return this;
    }

    public ZebedeeReaderLogBuilder collectionId(String collectionId) {
        addParameter(COLLECTION_ID, collectionId);
        return this;
    }

    @Override
    public ZebedeeReaderLogBuilder addParameter(String key, Object value) {
        return (ZebedeeReaderLogBuilder) super.addParameter(key, value);
    }

    @Override
    public String getLoggerName() {
        return LOG_NAME;
    }

}
