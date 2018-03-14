package com.github.onsdigital.zebedee.authorisation;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;

public class UserIdentityException extends ZebedeeException {

    public UserIdentityException(String message, int responseCode) {
        super(message, responseCode);
    }

    public int getResponseCode() {
        return super.statusCode;
    }
}
