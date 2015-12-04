package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.util.URIUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * Common functions for reading application data from Http request objects.
 */
public class RequestUtils {

    public static final String TOKEN_HEADER = "X-Florence-Token";

    /**
     * Get the zebedee session id from the given HttpServletRequest.
     * The session ID will be retrieved from the {@value #TOKEN_HEADER} header.
     *
     * @param request
     * @return
     */
    public static String getSessionId(HttpServletRequest request) {
        return request.getHeader(TOKEN_HEADER);
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
}
