package com.github.onsdigital.exceptions;

public class SessionsDecodeException extends RuntimeException{
    public SessionsDecodeException(String message) {
        this(message, null);
    }
    public SessionsDecodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
