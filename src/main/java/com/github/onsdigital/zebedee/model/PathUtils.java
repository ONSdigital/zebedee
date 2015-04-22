package com.github.onsdigital.zebedee.model;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class PathUtils {
    static final int MAX_LENGTH = 255;

    public static String toFilename(String string) {
        StringBuilder filename = new StringBuilder();

        // Strip dodgy characters:
        for (char c : string.toCharArray()) {
            if (c == '.' || Character.isJavaIdentifierPart(c)) {
                filename.append(c);
            }
        }

        // Ensure the String is a sensible length:
        return StringUtils.lowerCase(StringUtils.abbreviateMiddle(filename.toString(), "_",
                MAX_LENGTH));
    }

    /**
     * Creates a new file at the given path, including any required parent directories.
     *
     * @param path The path of the file to be created.
     * @throws IOException If a filesystem error occurs.
     */
    static void create(Path path) throws IOException {

        Files.createDirectories(path.getParent());
        Files.createFile(path);
    }

    /**
     * Convenience method for copying content between two paths.
     *
     * @param source      The source {@link Path}.
     * @param destination The destination {@link Path}.
     * @throws IOException If a filesystem error occurs.
     */
    static void copy(Path source, Path destination) throws IOException {

        createParentFolders(destination);
        doCopy(source, destination);
    }

    /**
     * Convenience method for moving content from one {@link Path} to another,
     * regardless of whether the destination {@link Path} exists. If the
     * destination exists, this method performs a copy-
     *
     * @param source      The source {@link Path}.
     * @param destination The destination {@link Path}.
     * @throws IOException If a filesystem error occurs.
     */
    public static void move(Path source, Path destination) throws IOException {

        createParentFolders(destination);
        if (!Files.exists(destination)) {

            // Move
            Files.move(source, destination);

        } else {

            // Copy-then-delete
            doCopy(source, destination);
            Files.delete(source);
        }
    }

    /**
     * Given a path to a file or directory, move all the files in the directory
     * to the destination directory. If a file is given, The parent directory
     * of the file is used.
     *
     * @param source
     * @param destination
     */
    public static void moveFilesInDirectory(Path source, Path destination) throws IOException {

        Path sourceDirectory = Files.isDirectory(source) ? source : source.getParent();
        Path destinationDirectory = Files.isDirectory(destination) ? destination : destination.getParent();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDirectory)) {
            for (Path entry : stream) {
                if (!Files.isDirectory(entry)) {
                    FileUtils.moveFileToDirectory(entry.toFile(), destinationDirectory.toFile(), true);
                }
            }
        }
    }

    /**
     * Given a path to a file or directory, copy all the files in the directory
     * to the destination directory. If a file is given, The parent directory
     * of the file is used.
     *
     * @param source
     * @param destination
     */
    public static void copyFilesInDirectory(Path source, Path destination) throws IOException {

        Path sourceDirectory = Files.isDirectory(source) ? source : source.getParent();
        Path destinationDirectory = Files.isDirectory(destination) ? destination : destination.getParent();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDirectory)) {
            for (Path entry : stream) {
                if (!Files.isDirectory(entry)) {
                    FileUtils.copyFileToDirectory(entry.toFile(), destinationDirectory.toFile(), true);
                }
            }
        }
    }

    /**
     * Given a path to a file or directory, delete all the files in the directory.
     * If a file is given, The parent directory of the file is used.
     *
     * @param target
     */
    public static void deleteFilesInDirectory(Path target) throws IOException {

        Path sourceDirectory = Files.isDirectory(target) ? target : target.getParent();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDirectory)) {
            for (Path entry : stream) {
                if (!Files.isDirectory(entry)) {
                    Files.delete(entry);
                }
            }
        }
    }

    private static void doCopy(Path source, Path destination)
            throws IOException {
        try (InputStream input = Files.newInputStream(source);
             OutputStream output = Files.newOutputStream(destination)) {
            IOUtils.copy(input, output);
        }
    }

    private static void createParentFolders(Path path) throws IOException {

        // Create any necessary parent folders:
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
    }
}
