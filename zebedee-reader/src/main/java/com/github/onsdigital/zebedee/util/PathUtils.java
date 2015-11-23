package com.github.onsdigital.zebedee.util;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by bren on 10/09/15.
 */
public class PathUtils {
    public static URI toRelativeUri(Path root, Path child) {

        // Handle spaces in filenames. An exception is thrown if the URI is created with spaces, so we encode them.
        if (child.toString().contains(" ")) {
            child = Paths.get(child.toString().replace(" ", "%20"));
        }

        return URI.create("/" + URIUtils.removeTrailingSlash(root.toUri().relativize(child.toUri()).getPath()));
    }
}
