package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.zebedee.util.PathUtils;
import org.junit.Test;

import java.net.URI;
import java.nio.file.Paths;

import static junit.framework.TestCase.assertEquals;

public class PathUtilsTest {

    @Test
    public void toRelativeUri() {
        String path = "root/other/path";
        final URI relativeUri = PathUtils.toRelativeUri(Paths.get("root"), Paths.get(path));

        assertEquals("/other/path", relativeUri.toString());
    }

    @Test
    public void toRelativeUriShouldSupportPathsWithSpaces() {
        String path = "root/other/path/somefile (1).xls";
        final URI relativeUri = PathUtils.toRelativeUri(Paths.get("root"), Paths.get(path));

        assertEquals("/other/path/somefile%20(1).xls", relativeUri.toString());
    }
}
