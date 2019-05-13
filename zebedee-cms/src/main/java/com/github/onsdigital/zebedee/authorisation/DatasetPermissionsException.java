package com.github.onsdigital.zebedee.authorisation;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;

public class DatasetPermissionsException extends ZebedeeException {

    public DatasetPermissionsException(String message, int responseCode) {
        super(message, responseCode);
    }
}