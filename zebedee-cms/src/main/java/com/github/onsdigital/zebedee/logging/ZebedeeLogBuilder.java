package com.github.onsdigital.zebedee.logging;

import ch.qos.logback.classic.Level;
import com.github.onsdigital.logging.builder.LogMessageBuilder;
import com.github.onsdigital.zebedee.exceptions.*;
import com.github.onsdigital.zebedee.model.Collection;

import javax.ws.rs.core.Response;

/**
 * Created by dave on 5/5/16.
 */
public class ZebedeeLogBuilder extends LogMessageBuilder {

    public static final String LOG_NAME = "com.github.onsdigital.logging";
    private static final String ZEBEDEE_EXCEPTION = "Zebedee Exception";
    private static final String STACK_TRACE_KEY = "stackTrace";
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

    private ZebedeeLogBuilder(Throwable t, String description) {
        super(t, description);
    }

    private ZebedeeLogBuilder(String description, Level level) {
        super(description, level);
    }

    public static ZebedeeLogBuilder logError(Throwable t) {
        return new ZebedeeLogBuilder(t, ZEBEDEE_EXCEPTION);
    }

    public static ZebedeeLogBuilder logError(Throwable t, String errorContext) {
        return new ZebedeeLogBuilder(t, ZEBEDEE_EXCEPTION + " " + errorContext);
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

    /**
     * Log an info level message.
     */
    public static ZebedeeLogBuilder logWarn(String message) {
        return new ZebedeeLogBuilder(message, Level.WARN);
    }

    /**
     * Log the error and throw the exception.
     */
    public void logAndThrow(Class<? extends ZebedeeException> exceptionClass) throws ZebedeeException {
        this.log();

        if (BadRequestException.class.equals(exceptionClass)) {
            throw new BadRequestException(getStringProperty(ERROR_CONTEXT));
        } else if (CollectionNotFoundException.class.equals(exceptionClass)) {
            throw new CollectionNotFoundException(getStringProperty(ERROR_CONTEXT));
        } else if (ConflictException.class.equals(exceptionClass)) {
            throw new ConflictException(getStringProperty(ERROR_CONTEXT));
        } else if (NotFoundException.class.equals(exceptionClass)) {
            throw new NotFoundException(getStringProperty(ERROR_CONTEXT));
        } else if (UnauthorizedException.class.equals(exceptionClass)) {
            throw new UnauthorizedException(getStringProperty(ERROR_CONTEXT));
        } else if (CollectionEventHistoryException.class.equals(exceptionClass)) {
            throw new CollectionEventHistoryException(getStringProperty(ERROR_CONTEXT));
        }

        // Default to internal server error.
        throw new UnexpectedErrorException(getStringProperty(ERROR_CONTEXT),
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    public void throwUnchecked(Throwable error) {
        this.log();
        throw new RuntimeException(error);
    }

    private String getStringProperty(String key) {
        return (String) this.parameters.getParameters().get(key);
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
        addParameter(COLLECTION_ID, collection.getDescription().id);
        return this;
    }

    public ZebedeeLogBuilder collectionId(String collectionId) {
        addParameter(COLLECTION_ID, collectionId);
        return this;
    }

    public ZebedeeLogBuilder collectionName(Collection collection) {
        addParameter(COLLECTION_NAME, collection.getDescription().name);
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
