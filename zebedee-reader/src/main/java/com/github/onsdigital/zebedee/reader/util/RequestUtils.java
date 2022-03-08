package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;
import com.github.onsdigital.zebedee.reader.api.ReadRequestHandler;
import com.github.onsdigital.zebedee.util.URIUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

import static com.github.onsdigital.zebedee.reader.util.ReaderRequestUtils.getRequestedLanguage;

/**
 * Common functions for reading application data from Http request objects.
 */
public final class RequestUtils {

    public static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTH_HEADER = "Authorization";
    public static final String FLORENCE_TOKEN_HEADER = "X-Florence-Token";

    private RequestUtils() {
        throw new IllegalStateException("Instantiation of a utility class is not allowed");
    }

    /**
     * Get the zebedee session id from the given HttpServletRequest.
     * The session ID will be retrieved from the auth headers.
     *
     * @param request the http request object
     * @return the access token/session ID or null if it was not provided
     */
    public static String getSessionId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // TODO: Simplify after migration from X-Florence-Token to Authorization header is complete
        String florenceHeader = request.getHeader(FLORENCE_TOKEN_HEADER);
        String authHeader = request.getHeader(AUTH_HEADER);
        String sessionId = null;
        // If the Authorization header contains a '.' then it is a JWT session token rather than a service token
        if (StringUtils.isNotBlank(authHeader) && authHeader.contains(".")) {
            sessionId = removeBearerPrefixIfPresent(authHeader);
        } else if (StringUtils.isNotBlank(florenceHeader)) {
            sessionId = removeBearerPrefixIfPresent(florenceHeader);
        }

        return sessionId;
    }

    /**
     * Get the zebedee collection id from the given HttpServletRequest.
     * By default tries to read collection id from cookies named collection. If not found falls back to reading from uri.
     * @param request
     * @return
     */
    public static String getCollectionId(HttpServletRequest request) {
        return URIUtils.getPathSegment(request.getRequestURI(), 2);
    }

    /**
     * Common function to read file URI from a request and get the file as a Resource instance.
     *
     * @param request
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    public static Resource getResource(HttpServletRequest request) throws ZebedeeException, IOException {
        String uri = request.getParameter("uri");

        // Requested path
        if (StringUtils.isBlank(uri)) {
            throw new BadRequestException("Please provide a URI");
        }

        return new ReadRequestHandler(getRequestedLanguage(request)).findResource(request);
    }

    /**
     * Helper method to create instances of ZebedeeReader from a request.
     * @param request
     * @return
     */
    public static ZebedeeReader getZebedeeReader(HttpServletRequest request) {
        return new ZebedeeReader(getRequestedLanguage(request));
    }

    public static Optional<String> getURIParameter(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        String uri = request.getParameter("uri");
        if (StringUtils.isEmpty(uri)) {
            return Optional.empty();
        }
       return Optional.ofNullable(uri);
    }

    /**
     * Removes the Bearer prefix from the Authorization header value if presents.
     *
     * @param accessToken the token value from the Authorization header
     * @return the access token with the prefix removed
     */
    public static String removeBearerPrefixIfPresent(String accessToken) {
        if (StringUtils.isEmpty(accessToken)) {
            return accessToken;
        }

        if (accessToken.startsWith(BEARER_PREFIX)) {
            accessToken = accessToken.replaceFirst(BEARER_PREFIX, "");
        }
        return accessToken;
    }
}
