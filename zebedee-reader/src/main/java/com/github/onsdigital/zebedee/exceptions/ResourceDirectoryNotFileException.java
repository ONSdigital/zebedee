   package com.github.onsdigital.zebedee.exceptions;

/**
 * Exception type for cases where the requested resource is a direcory not a file. Allows us to separate this from
 * other {@link BadRequestException} scenarios.
 */
public class ResourceDirectoryNotFileException extends BadRequestException {

    public ResourceDirectoryNotFileException(String message) {
        super(message);
    }
}
