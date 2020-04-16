package com.github.onsdigital.zebedee.util.versioning;

import java.util.List;
import java.util.stream.Collectors;

import static java.text.MessageFormat.format;

public class VersionNotFoundException extends Exception {

    static final String VERSION_NOT_FOUND_FMT = "Dataset versions could not be found. Try deleting and re-uploading " +
            "the lastest version of each dataset: {0}";

    public VersionNotFoundException(final String message) {
        super(message);
    }

    public VersionNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public VersionNotFoundException(final Throwable cause) {
        super(cause);
    }

    public static VersionNotFoundException versionsNotFoundException(List<MissingVersion> missingVersions) {
        String details = missingVersions
                .stream()
                .map(i -> i.getCurrentURI())
                .distinct()
                .collect(Collectors.joining(","));

        String message = format(VERSION_NOT_FOUND_FMT, details);

        return new VersionNotFoundException(message);
    }
}
