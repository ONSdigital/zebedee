package com.github.onsdigital.zebedee.service;

import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.regex.Pattern;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.warn;

/**
 * Utility class for {@link com.github.onsdigital.zebedee.model.ServiceAccount} token values.
 */
public class ServiceTokenUtils {

    /**
     * A valid service account token must contain >= 16 alphanumeric characters.
     */
    static final Pattern VALID_FILENAME_REGEX = Pattern.compile("^[a-zA-Z0-9]{16,}$");

    static final String BEARER_PREFIX_UC = "Bearer";
    static final String JSON_EXTENSION = ".json";

    /**
     * ServiceTokenValidator is a utility class containing only static methods.
     */
    private ServiceTokenUtils() {
    }

    /**
     * Validate the given token string conatins only alphanumeric characters and has a minimum length of 16 characters.
     *
     * @param token the token value to validate.
     * @return true if valid false otherwise.
     */
    public static boolean isValidServiceToken(String token) {
        boolean isValid = false;

        if (matches(token)) {
            isValid = true;
            info().log("valid service token");
        } else {
            warn().log("invalid service token value: service tokens must contain 16 or more alphanumerica only characters");
        }

        return isValid;
    }

    /**
     * Determined if a value is a valid service account token. Valid tokens contain only alphanumeric characters and
     * must have a length of >= 16 characters.
     *
     * @param token the value to check.
     * @return true if valid token value false otherwise.
     */
    private static boolean matches(String token) {
        boolean isMatch = false;

        if (StringUtils.isEmpty(token)) {
            warn().log("invalid service token value: empty or null");
        } else {
            isMatch = VALID_FILENAME_REGEX.matcher(token).matches();
        }
        return isMatch;
    }

    /**
     * Validate if a service authorization header value is valid. A valid service cannot be empty and must start with
     * the prefix 'Bearer ' (case sensitive).
     *
     * @param serviceAuthHeader the value to validate.
     * @return true if the value is a valid service auth header false otherwise.
     */
    public static boolean isValidServiceAuthorizationHeader(String serviceAuthHeader) {
        boolean isValid = true;

        if (StringUtils.isEmpty(serviceAuthHeader)) {
            warn().log("invalid service authorization header value is null or empty");
            isValid = false;

        } else if (!serviceAuthHeader.startsWith(BEARER_PREFIX_UC)) {
            warn().log("invalid service authorization header value not prefixed with Bearer (case sensitive)");
            return false;

        } else {
            info().log("service authorization header valid");
        }

        return isValid;
    }

    /**
     * Removes the Bearer prefix from a service authorization header returning a service account token. Returns the
     * input value if its empty or does not contain the 'Bearer ' prefix.
     *
     * @param rawHeader the service authorization header to process.
     * @return the service account token if input valid otherwise the input value.
     */
    public static String extractServiceAccountTokenFromAuthHeader(String rawHeader) {
        String serviceToken = rawHeader;

        if (StringUtils.isEmpty(rawHeader)) {
            warn().log("cannot remove Bearer prefix from null or empty service authorization header value");
        } else {
            serviceToken = rawHeader.replaceFirst(BEARER_PREFIX_UC, "").trim();
            info().log("bearer prefix removed from service auth header");
        }
        return serviceToken;
    }


    /**
     * Get the {@link Path} to the service account file for the given token.
     *
     * @param servicesPath the {@link Path} of the service root directory.
     * @param token        the service account token.
     * @return a {@link Path} to the service account file.
     * @throws {@link IllegalArgumentException} if the services path is null.
     */
    public static Path getServiceAccountPath(Path servicesPath, String token) {
        if (servicesPath == null) {
            throw new IllegalArgumentException("service dir path required but was empty or null");
        }
        return servicesPath.resolve(getTokenFilename(token));
    }

    static final String getTokenFilename(String token) {
        return token + JSON_EXTENSION;
    }

}
