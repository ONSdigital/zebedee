package com.github.onsdigital.zebedee.service;

import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.regex.Pattern;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.warn;
import static java.text.MessageFormat.format;

/**
 * Utility class for {@link com.github.onsdigital.zebedee.model.ServiceAccount} token values.
 */
public class ServiceTokenUtils {

    private static final Pattern VALID_FILENAME_REGEX = Pattern.compile("^[a-zA-Z0-9]+$");

    static final String JSON_EXTENSION = ".json";
    static final int MIN_TOKEN_LENGTH = 16;
    static final String EMPTY_TOKEN_ERROR = "invalid service token value: empty";
    static final String INVALID_TOKEN_ERROR = "invalid service token value: tokens must match alphanumeric";
    static final String TOKEN_LENGTH_INVALID_ERROR = "invalid service token value: tokens must have length >= {0} characters";
    static final String SERVICE_PATH_EMPTY_ERROR = "service dir path required but was empty or null";

    /**
     * ServiceTokenValidator is a utility class containing only static methods.
     */
    private ServiceTokenUtils() {
    }

    /**
     * Validate the given token string conatins only alphanumeric characters and has a minimum length of 16 characters.
     * Throws a {@link RuntimeException} if the token is invalid.
     *
     * @param token the token string to validate.
     * @throws {@link RuntimeException} if the token is not valid.
     */
    public static void validateToken(String token) {
        if (StringUtils.isEmpty(token)) {
            throw new RuntimeException(EMPTY_TOKEN_ERROR);
        }

        if (!VALID_FILENAME_REGEX.matcher(token).matches()) {
            warn().data("token", token).log(EMPTY_TOKEN_ERROR);
            throw new RuntimeException(INVALID_TOKEN_ERROR);
        }

        if (token.length() < MIN_TOKEN_LENGTH) {
            throw new RuntimeException(format(TOKEN_LENGTH_INVALID_ERROR, MIN_TOKEN_LENGTH));
        }
    }

    /**
     * Get the {@link Path} to the service account file for the given token.
     *
     * @param servicesPath the {@link Path} of the service root directory.
     * @param token        the service account token.
     * @return a {@link Path} to the service account file.
     * @throws {@link RuntimeException} if there is a problem for getting the path.
     */
    public static Path getServiceTokenPath(Path servicesPath, String token) {
        if (servicesPath == null) {
            throw new RuntimeException(SERVICE_PATH_EMPTY_ERROR);
        }
        validateToken(token);
        return servicesPath.resolve(getTokenFilename(token));
    }

    static final String getTokenFilename(String token) {
        return token + JSON_EXTENSION;
    }

}
