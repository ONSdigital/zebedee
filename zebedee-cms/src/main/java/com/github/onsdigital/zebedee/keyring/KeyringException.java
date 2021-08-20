package com.github.onsdigital.zebedee.keyring;

import java.io.IOException;

import static java.text.MessageFormat.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Exception type for errors while accessing/storing collection keys in the {@link CollectionKeyCache}
 */
public class KeyringException extends IOException {

    private String collectionID;

    public KeyringException(final String message) {
        super(message);
    }

    public KeyringException(final String message, final String collectionID) {
        super(formatExceptionMsg(message, collectionID));
        this.collectionID = collectionID;
    }

    public KeyringException(final String message, final String collectionID, final Throwable cause) {
        super(formatExceptionMsg(message, collectionID), cause);
        this.collectionID = collectionID;
    }

    public KeyringException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public KeyringException(final Throwable cause) {
        super(cause);
    }

    public String getCollectionID() {
        return this.collectionID;
    }

    /**
     * Format the exception message to include the suffix "CollectionID: <ID>" if the collection ID is not empty.
     * Otherwise returns the original message.
     *
     * @param message      the exception message.
     * @param collectionID the collection ID to append.
     * @return the formatted message.
     */
    public static String formatExceptionMsg(String message, String collectionID) {
        if (isEmpty(message)) {
            message = "";
        }

        if (isEmpty(collectionID)) {
            return message;
        }

        return format("{0}: CollectionID - {1}", message, collectionID);
    }
}
