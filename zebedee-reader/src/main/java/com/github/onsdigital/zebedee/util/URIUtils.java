package com.github.onsdigital.zebedee.util;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;

/**
 * Created by bren on 31/07/15.
 */
public class URIUtils {

    private static final String FORWARD_SLASH = "/";

    /**
     * @param uri The URI of the item. If url starts with forwards slash removes the forward slash
     * @return
     */
    public static String removeLeadingSlash(String uri) {
        if (StringUtils.startsWith(uri, FORWARD_SLASH)) {
            return StringUtils.removeStart(uri, FORWARD_SLASH);
        }
        return uri;
    }


    /**
     * @param uri The URI of the item. If url ends with forwards slash removes the forward slash
     * @return
     */
    public static String removeTrailingSlash(String uri) {
        if (StringUtils.endsWith(uri, FORWARD_SLASH)) {
            return StringUtils.removeEnd(uri, FORWARD_SLASH);
        }
        return uri;
    }


    /**
     * Resolves nth path segment, if n is bigger than number of segments or a negative value returns null.
     * Segment numbers are 0 based
     * <p>
     * e.g. ("/data/economy/yadayada", 1) = "economy"
     *
     * @param uri
     * @param n
     * @return Returns nth path segment.
     */
    public static String getPathSegment(String uri, int n) {
        if (StringUtils.isEmpty(uri) || n < 0) {
            return null;
        }
        String[] segments = URI.create(uri).toString().split("/");
        if (n >= segments.length) {
            return null;
        }
        return segments[n];
    }
}
