package com.github.onsdigital.zebedee.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.removeEnd;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
     * <p>
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
        // getSegments will return [] empty array for passing in one slash "/"
        String[] segments = getSegments(uri);
        if (ArrayUtils.isEmpty(segments)) {
            return null;
        }

        return segments[segments.length - 1];
    }

    public static String getQueryParameterFromURL(String url, String queryParam) {
        try {
            String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8.toString());
            List<NameValuePair> params = URLEncodedUtils.parse(new URI(decoded), StandardCharsets.UTF_8);
            Optional<NameValuePair> result = params.stream().filter(param -> param.getName().equals(queryParam)).findFirst();
            if (result.isPresent()){
                return result.get().getValue();
            }
        } catch (UnsupportedEncodingException | URISyntaxException e) {
            throw new RuntimeException("Error retrieving query parameter from path", e);
        }
        return null;
    }

    public static boolean isLastSegmentFileExtension(String path) {
        if ("".equals(path)){
            return false;
        }
        String lastSegment = getLastSegment(path);
        if (lastSegment == null){
            return false;
        }
        return lastSegment.contains(".");
    }

    public static boolean hasQueryParams(String path) {
        return path.contains("=");
    }

    public static String removeLastSegment(String uri) {
        String result = removeEnd(removeTrailingSlash(uri), FORWARD_SLASH + getLastSegment(uri));
        return isEmpty(result) ? "/" : result;
    }

    public static String getNSegmentsAfterSegmentInput(String uri, String segmentInput, int n) {
        if (!uri.contains(segmentInput)) {
            return uri;
        }
        int start = uri.indexOf(segmentInput);
        String everythingAfterSegment = uri.substring(start + segmentInput.length());
        return uri.substring(0, start + segmentInput.length()) + getNSegments(removeLeadingSlash(everythingAfterSegment), n);
    }

    public static String getNSegments(String uri, int n) {
        StringBuilder result = new StringBuilder();
        String[] parts = removeLeadingSlash(uri).split("/");
        int minimumOfSegments = Math.min(parts.length, n);
        for (int i = 0; i < minimumOfSegments; i++) {
            if (parts[i] != null) {
                result.append("/").append(parts[i]);
            } else {
                break;
            }
        }
        return result.toString();
    }
}
