package com.github.onsdigital.zebedee.util;

import java.net.URI;
import java.nio.file.Path;

/**
 * Created by bren on 10/09/15.
 */
public class PathUtils {
    public static URI toRelativeUri(Path root, Path child) {
        return URI.create("/" + URIUtils.removeTrailingSlash(root.toUri().relativize(child.toUri()).getPath()));
    }
}
