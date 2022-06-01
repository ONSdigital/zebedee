package org.springframework.http;

import org.springframework.util.InvalidMimeTypeException;

public class InvalidMediaTypeException extends RuntimeException {
    public InvalidMediaTypeException(String mediaType, String message) {
        super(message + ": " +mediaType);
    }

    public InvalidMediaTypeException(InvalidMimeTypeException ex) {
        super(ex);
    }
}
