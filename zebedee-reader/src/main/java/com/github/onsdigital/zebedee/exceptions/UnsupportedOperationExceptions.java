package com.github.onsdigital.zebedee.exceptions;


import java.io.IOException;

/**
 * Created by Ann W on 13/12/2021.
 * to support the migration to JWT
 */

public class UnsupportedOperationExceptions extends IOException {

    public UnsupportedOperationExceptions(String message) {
        super(message);
    }

}
