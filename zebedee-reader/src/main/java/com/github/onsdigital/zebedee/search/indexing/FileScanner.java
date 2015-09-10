package com.github.onsdigital.zebedee.search.indexing;

import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.util.PathUtils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Searches the file system
 */
public class FileScanner {

    private Path root;

    public List<String> scan() throws IOException {
        Path dir = Paths.get(ReaderConfiguration.getConfiguration().getContentDir());
        this.root = dir;
        List<String> fileNames = new ArrayList<>();
        return getFileNames(fileNames, dir);
    }

    /**
     * Iterates through the file system from a specified root directory and
     * stores the file names
     *
     * @param fileNames a List to store results in
     * @param dir       the root directory to start searching from
     * @return the list with file names
     * @throws IOException if any file io operations fail
     */
    private List<String> getFileNames(List<String> fileNames, Path dir)
            throws IOException {

        if (fileNames == null || dir == null) {
            throw new IllegalArgumentException(
                    "List of fileNames and Path dir cannot be null");
        }

        // java 7 try-with-resources automatically closes streams after use
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (path.toFile().isDirectory()) {
                    getFileNames(fileNames, path);
                } else {
                    String fileName = path.toAbsolutePath().toString();
                    if (isRequiredForIndex(fileName)) {
                        fileNames.add(PathUtils.toRelativeUri(root, path.getParent()).toString());
                    }
                }
            }
        }

        return fileNames;
    }

    private static boolean isRequiredForIndex(String fileName) {
        return isDataFile(fileName) && isNotRelease(fileName) && isNotPreviousVersion(fileName);
    }

    private static boolean isDataFile(String fileName) {
        return fileName.endsWith("data.json");
    }

    private static boolean isNotRelease(String fileName) {
        return !fileName.contains("releases");
    }

    private static boolean isNotPreviousVersion(String fileName) {
        return !fileName.contains("versions");
    }
}