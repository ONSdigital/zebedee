package com.github.onsdigital.zebedee.util.versioning;

public class VersionNotFoundException extends Exception {

    public VersionNotFoundException(final String message) {
        super(message);
    }

    public VersionNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public VersionNotFoundException(final Throwable cause) {
        super(cause);
    }
}
