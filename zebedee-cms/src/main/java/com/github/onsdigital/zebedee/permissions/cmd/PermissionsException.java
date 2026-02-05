package com.github.onsdigital.zebedee.permissions.cmd;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;

import static org.apache.hc.core5.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.hc.core5.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.hc.core5.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.hc.core5.http.HttpStatus.SC_UNAUTHORIZED;

/**
 * @deprecated in favour of the dp-permissions-api. Once all dataset related APIs have been updated to use the
 *             dp-authorisation v2 library and JWT sessions are in use, this service will be removed.
 */
@Deprecated
public class PermissionsException extends ZebedeeException {

    private static final String ERROR_PREFIX = "CMD permissions request denied: ";

    public static PermissionsException datasetIDNotProvidedException() {
        return new PermissionsException(ERROR_PREFIX + "dataset ID required but was empty", SC_BAD_REQUEST);
    }

    public static PermissionsException sessionNotProvidedException() {
        return new PermissionsException(ERROR_PREFIX + "session required but empty", SC_BAD_REQUEST);
    }

    public static PermissionsException internalServerErrorException() {
        return new PermissionsException(ERROR_PREFIX + "internal server error", SC_INTERNAL_SERVER_ERROR);
    }

    public static PermissionsException sessionNotFoundException() {
        return new PermissionsException(ERROR_PREFIX + "session not found", SC_UNAUTHORIZED);
    }

    public static PermissionsException collectionIDNotProvidedException() {
        return new PermissionsException(ERROR_PREFIX + "collection ID required but was empty", SC_BAD_REQUEST);
    }

    public static PermissionsException collectionNotFoundException() {
        return new PermissionsException(ERROR_PREFIX + "collection not found", SC_NOT_FOUND);
    }

    public static PermissionsException serviceAccountNotFoundException() {
        return new PermissionsException(ERROR_PREFIX + "service account not found", SC_UNAUTHORIZED);
    }

    public static PermissionsException serviceTokenNotProvidedException() {
        return new PermissionsException(ERROR_PREFIX + "service token required but none provided", SC_BAD_REQUEST);
    }

    public static PermissionsException invalidPermissionsRequestException() {
        return new PermissionsException(ERROR_PREFIX + "cannot process request as required fields missing",
                SC_BAD_REQUEST);
    }

    public PermissionsException(String message, int responseCode) {
        super(message, responseCode);
    }
}
