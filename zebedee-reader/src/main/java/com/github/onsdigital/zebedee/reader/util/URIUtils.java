package com.github.onsdigital.zebedee.reader.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by bren on 31/07/15.
 */
public class URIUtils {

    /**
     * @param uri The URI of the item. If url starts with forwards slash removes the forward slash
     * @return
     */
    public static String removeForwardSlash(String uri) {
        if (StringUtils.startsWith(uri, "/")) {
            return StringUtils.substring(uri, 1);
        }
        return uri;
    }
}
