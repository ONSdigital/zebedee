package com.github.onsdigital.zebedee.reader.util;

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
}
