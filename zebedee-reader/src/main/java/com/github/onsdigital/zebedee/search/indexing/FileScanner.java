package com.github.onsdigital.zebedee.search.indexing;

import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.util.PathUtils;
import com.github.onsdigital.zebedee.util.URIUtils;
import org.apache.commons.lang3.StringUtils;

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

    public FileScanner() {
        root = Paths.get(ReaderConfiguration.getConfiguration().getContentDir());
    }

    public List<String> scan() throws IOException {
        return scan(null);
    }


    public List<String> scan(String path) throws IOException {
        Path dir = root;
        if (StringUtils.isEmpty(path) == false) {
            dir = root.resolve(URIUtils.removeLeadingSlash(path));
        }
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
                    if(isNotPreviousVersion(path.getFileName().toString())) {
                        getFileNames(fileNames, path);
                    } else {
                        continue;//skip versions
                    }
                } else {
                    String fileName = path.toAbsolutePath().toString();
                    if (isDataFile(fileName)) {
                        fileNames.add(PathUtils.toRelativeUri(root, path.getParent()).toString());
                    }
                }
            }
        }

        return fileNames;
    }

    private static boolean isDataFile(String fileName) {
        return fileName.endsWith("data.json");
    }

    private static boolean isNotPreviousVersion(String fileName) {
        return !fileName.equals("previous");
    }
}