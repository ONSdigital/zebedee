package com.github.onsdigital.zebedee.permissions.cmd;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;

public class PermissionsException extends ZebedeeException {

    public PermissionsException(String message, int responseCode) {
        super(message, responseCode);
    }
}
