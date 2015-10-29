package com.github.onsdigital.zebedee.model.content.item;

import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A single content item that can contain previous versions.
 * <p>
 * This class handles operations related to versioning content. When
 * a version is created the current version is copied into a directory under
 * the "previous" directory.
 */
public class VersionedContentItem extends ContentItem {

    private static final String VERSION_DIRECTORY = "previous";
    private static final String VERSION_PREFIX = "v";

    public VersionedContentItem(URI uri, Path path) throws NotFoundException {
        super(uri, path);
    }

    public static String getVersionDirectoryName() {
        return VERSION_DIRECTORY;
    }

    /**
     * Utility function to determine if a give uri is that of a previous version.
     * <p>
     * Versioned URI's will have /
     *
     * @param uri
     * @return
     */
    public static boolean isVersionedUri(String uri) {
        return uri.contains(String.format("/%s/%s", getVersionDirectoryName(), VERSION_PREFIX));
    }

    /**
     * Create a version from the given source path. The source path is typically the path to the published content, so
     * it cannot be assumed the current version is in the root path of this VersionedContentItem.
     *
     * @param versionSourcePath
     * @return
     */
    public ContentItemVersion createVersion(Path versionSourcePath) throws IOException, NotFoundException {

        // create a new directory for the version. e.g. edition/previous/v1
        String versionIdentifier = createVersionIdentifier(versionSourcePath);
        Path versionPath = getVersionDirectoryPath().resolve(versionIdentifier);
        Files.createDirectories(versionPath);

        URI versionUri = URI.create(String.format("%s/%s/%s", getUri(), getVersionDirectoryName(), versionIdentifier));

        copyFilesIntoVersionDirectory(versionSourcePath, versionPath);

        return new ContentItemVersion(versionIdentifier, versionPath, this, versionUri);
    }

    /**
     * Return true if a previous version exists
     *
     * @return
     */
    public boolean versionExists() {
        if (Files.exists(getVersionDirectoryPath())
                && getVersionDirectoryPath().toFile().listFiles().length > 0) {
            return true;
        }

        return false;
    }

    /**
     * Copy only the files (not directories) from the given source path, into the version path.
     *
     * @param versionSourcePath
     * @param versionPath
     * @throws IOException
     */
    private void copyFilesIntoVersionDirectory(Path versionSourcePath, Path versionPath) throws IOException {
        File destinationFile = versionPath.toFile();

        // Iterate the files in the source directory.
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(versionSourcePath)) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) { // ignore directories
                    FileUtils.copyFileToDirectory(path.toFile(), destinationFile);
                }
            }
        }
    }

    /**
     * Get the path of the versions folder for this content.
     *
     * @return
     */
    public Path getVersionDirectoryPath() {
        return this.getPath().resolve(getVersionDirectoryName());
    }

    /**
     * Determine the version identifier of the next version.
     *
     * @return
     * @param versionSourcePath
     */
    private String createVersionIdentifier(Path versionSourcePath) {

        int version = 1;

        Path versionDirectory = versionSourcePath.resolve(getVersionDirectoryName());

        if (Files.exists(versionDirectory)) {
            version = versionDirectory.toFile().listFiles().length + 1;
        }

        return VERSION_PREFIX + version;
    }
}
