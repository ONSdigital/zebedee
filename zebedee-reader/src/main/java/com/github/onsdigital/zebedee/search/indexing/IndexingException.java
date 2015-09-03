package com.github.onsdigital.zebedee.search.indexing;

/**
 * Created by bren on 13/07/15.
 */
public class IndexingException extends RuntimeException {

    public IndexingException() {
    }

    public IndexingException(String message) {
        super(message);
    }

    public IndexingException(String message, Throwable cause) {
        super(message, cause);
    }

    public IndexingException(Throwable cause) {
        super(cause);
    }

    public IndexingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
