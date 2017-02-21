package com.github.onsdigital.zebedee.logging;

import ch.qos.logback.classic.Level;
import com.github.onsdigital.logging.builder.LogMessageBuilder;

import java.nio.file.Path;

/**
 * Created by dave on 5/5/16.
 */
public class ZebedeeReaderLogBuilder extends LogMessageBuilder {

    private static final String LOG_NAME = "com.github.onsdigital.zebedee-reader";

    private static final String ELASTIC_SEARCH_PREFIX = "[ElasticSearch] ";
    private static final String ZEBEDEE_READER_EXCEPTION = "Zebedee Reader Exception";
    private static final String USER = "user";
    private static final String TABLE = "table";
    private static final String CDID = "CDID";
    private static final String PATH = "path";
    private static final String URI = "uri";
    private static final String COLLECTION_ID = "collectionId";

    private ZebedeeReaderLogBuilder(String description) {
        super(description);
    }

    private ZebedeeReaderLogBuilder(String description, Level level) {
        super(description, level);
    }

    private ZebedeeReaderLogBuilder(Throwable t, String description) {
        super(t, description);
    }

    public static ZebedeeReaderLogBuilder logError(Throwable t) {
        return new ZebedeeReaderLogBuilder(t, ZEBEDEE_READER_EXCEPTION);
    }

    public static ZebedeeReaderLogBuilder logError(Throwable t, String errorContext) {
        return new ZebedeeReaderLogBuilder(t, ZEBEDEE_READER_EXCEPTION + ": " + errorContext);
    }

    public static ZebedeeReaderLogBuilder logWarn(String message) {
        return new ZebedeeReaderLogBuilder(message, Level.WARN);
    }

    public static ZebedeeReaderLogBuilder logDebug(String message) {
        return new ZebedeeReaderLogBuilder(message, Level.DEBUG);
    }

    public static ZebedeeReaderLogBuilder logTrace(String message) {
        return new ZebedeeReaderLogBuilder(message, Level.TRACE);
    }

    public static ZebedeeReaderLogBuilder logInfo(String message) {
        return new ZebedeeReaderLogBuilder(message, Level.INFO);
    }

    public static ZebedeeReaderLogBuilder elasticSearchLog(String message) {
        return (ZebedeeReaderLogBuilder) new ZebedeeReaderLogBuilder(ELASTIC_SEARCH_PREFIX + message, Level.INFO)
                .addMessage(message);
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

    public ZebedeeReaderLogBuilder expected(Object expected) {
        addParameter("expected", expected.toString());
        return this;
    }

    public ZebedeeReaderLogBuilder actual(Object actual) {
        addParameter("actual", actual.toString());
        return this;
    }

    public ZebedeeReaderLogBuilder collectionId(String collectionId) {
        addParameter(COLLECTION_ID, collectionId != null ? collectionId : "");
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
