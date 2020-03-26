package com.github.onsdigital.zebedee.util.versioning;

import com.github.onsdigital.zebedee.model.Collection;

import java.util.List;
import java.util.stream.Collectors;

import static java.text.MessageFormat.format;

public class VersionNotFoundException extends Exception {

    static final String VERSION_NOT_FOUND_FMT = "Error verifying dataset(s): version(s) not found in either " +
            "collection or published content. Collection: {0}, uris: {1}";

    public VersionNotFoundException(final String message) {
        super(message);
    }

    public VersionNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public VersionNotFoundException(final Throwable cause) {
        super(cause);
    }

    public static VersionNotFoundException versionsNotFoundException(Collection collection,
                                                                     List<MissingVersion> missingVersions) {
        String details = missingVersions
                .stream()
                .map(i -> i.toString())
                .collect(Collectors.joining(","));

        String collectionId = collection != null ? collection.getId() : "";

        String message = format(VERSION_NOT_FOUND_FMT, collectionId, details);

        return new VersionNotFoundException(message);
    }
}
