package com.github.onsdigital.zebedee.search.indexing;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Searches the file system
 */
public class ScanFileSystem {

    /**
     * Iterates through the file system from a specified root directory and
     * stores the file names
     *
     * @param fileNames a List to store results in
     * @param dir       the root directory to start searching from
     * @return the list with file names
     * @throws IOException if any file io operations fail
     */
    public static List<String> getFileNames(List<String> fileNames, Path dir)
            throws IOException {

        if (fileNames == null || dir == null) {
            throw new IllegalArgumentException(
                    "List of fileNames and Path dir cannot be null");
        }

        // java 7 try-with-resources automatically closes streams after use
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (path.toFile().isDirectory())
                    getFileNames(fileNames, path);
                else {
                    String fileName = path.toAbsolutePath().toString();

                    if (isRequiredForIndex(fileName)) {
                        fileNames.add(fileName);
                    }
                }
            }
        }

        return fileNames;
    }

    /**
     * @param files collection to store the files in
     * @param dir   the root directory to start searching from
     * @return collection of located files
     * @throws IOException if file lookup fails
     */
    public static List<File> getFiles(List<File> files, Path dir, String type)
            throws IOException {

        if (files == null || dir == null) {
            throw new IllegalArgumentException(
                    "List of fileNames and Path dir cannot be null");
        }

        // java 7 try-with-resources automatically closes streams after use
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (path.toFile().isDirectory()) {
                    getFiles(files, path, type);
                } else {
                    File file = path.toFile();
                    if (isCollectionItem(file.getAbsolutePath(), type)) {
                        files.add(file);
                    }
                }
            }
        }

        return files;
    }

    private static boolean isRequiredForIndex(String fileName) {
        return isDataFile(fileName) && isNotRelease(fileName) && isNotPreviousVersion(fileName);
    }

    private static boolean isCollectionItem(String fileName, String type) {
        return isDataFile(fileName) && isCollectionItemType(fileName, type) && isNotPreviousVersion(fileName);
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

    private static boolean isCollectionItemType(String fileName, String type) {
        return fileName.contains(type);
    }
}
