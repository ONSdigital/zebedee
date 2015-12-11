package com.github.onsdigital.zebedee.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.removeEnd;

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
            return removeEnd(uri, FORWARD_SLASH);
        }
        return uri;
    }


    /**
     * Resolves nth path segment, if n is bigger than number of segments or a negative value returns null.
     * Segment numbers are 1 based
     * <p>
     * e.g. ("/data/economy/yadayada", 2) = "economy"
     *
     * Note this method only works for absolute uris. Relative uris will not work as expected.
     *
     * @param uri
     * @param n
     * @return Returns nth path segment.
     */

    /*Method splits uri by forward slash(/), thats why relative uris would need segment number based on 0. Only use for absolute uris   */
    public static String getPathSegment(String uri, int n) {
        if (isEmpty(uri) || n < 0) {
            return null;
        }
        String[] segments = getSegments(uri);
        if (n >= segments.length) {
            return null;
        }
        return segments[n];
    }

    public static String[] getSegments(String uri) {
        return uri.split("/");
    }


    public static String getLastSegment(String uri) {
        if (isEmpty(uri)) {
            return null;
        }
        String[] segments = getSegments(uri);
        if (ArrayUtils.isEmpty(segments)) {
            return null;
        }

        return segments[segments.length - 1];
    }

    public static String removeLastSegment(String uri) {
        if (isEmpty(uri)) {
            return null;
        }
        String result =  removeEnd(removeTrailingSlash(uri), FORWARD_SLASH + getLastSegment(uri));
        return isEmpty(result) ? "/" : result;
    }
}
