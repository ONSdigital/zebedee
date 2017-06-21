package com.github.onsdigital.zebedee.model.content.item;

import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.util.URIUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.onsdigital.zebedee.util.URIUtils.removeLeadingSlash;

/**
 * A single content item that can contain previous versions.
 * <p>
 * This class handles operations related to versioning content. When
 * a version is created the current version is copied into a directory under
 * the "previous" directory.
 */
public class VersionedContentItem extends ContentItem {

    static final String VERSION_DIRECTORY = "previous";
    static final String VERSION_PREFIX = "v";

    public VersionedContentItem(String uri) throws NotFoundException {
        super(uri);
    }

    public static String getVersionDirectoryName() {
        return VERSION_DIRECTORY;
    }

    public static String getVersionUri(String uri, int versionNumber) {
        return uri + "/" + VERSION_DIRECTORY + "/" + VERSION_PREFIX + versionNumber;
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
     * Given a versioned uri, remove the version related part of the uri and return the result.
     *
     * @param uri
     * @return
     */
    public static String resolveBaseUri(String uri) {
        if (!isVersionedUri(uri))
            return uri;

        Path path = Paths.get(uri);

        // if there is no file extension, just jump up two levels. i.e /economy/gdp/previous/v1/ -> /economy/gdp
        if (FilenameUtils.getExtension(uri).length() == 0) {
            return path.getParent().getParent().toString();
        }

        // if there is a file extension, we are refering specifically to a file, so add that filename onto the end of the base
        // i.e /economy/gdp/previous/v1/data.json -> /economy/gdp/data.json
        Path fileName = path.getFileName();
        Path basePath = path.getParent().getParent().getParent().resolve(fileName);
        return basePath.toString();
    }

    private static int getNextVersionNumber(Path versionSourcePath) {
        int version = 1;

        Path versionDirectory = versionSourcePath.resolve(getVersionDirectoryName());

        if (Files.exists(versionDirectory)) {
            version = versionDirectory.toFile().listFiles().length + 1;
        }

        String versionIdentifier = VERSION_PREFIX + version;

        while (Files.exists(versionDirectory.resolve(versionIdentifier))) {
            version++;
            versionIdentifier = VERSION_PREFIX + version;
        }
        return version;
    }

    /**
     * Return the identifier of the most recently added version.
     *
     * @param datasetPath
     * @return
     */
    public static String getLastVersionIdentifier(Path datasetPath) {
        int version = getNextVersionNumber(datasetPath);
        return VERSION_PREFIX + (version - 1);
    }

    /**
     * Create a version from the given source path. The source path is typically the path to the published content, so
     * it cannot be assumed the current version is in the root path of this VersionedContentItem.
     *
     * @param contentRoot
     * @return
     */
    public ContentItemVersion createVersion(Path contentRoot, ContentReader contentReader, ContentWriter contentWriter) throws IOException, ZebedeeException {

        // create a new directory for the version. e.g. edition/previous/v1
        Path absolutePath = contentRoot.resolve(URIUtils.removeLeadingSlash(getUri()));
        String versionIdentifier = createVersionIdentifier(absolutePath);
        String versionUri = String.format("%s/%s/%s", getUri(), getVersionDirectoryName(), versionIdentifier);

        copyFilesIntoVersionDirectory(contentRoot, versionUri, contentReader, contentWriter);

        return new ContentItemVersion(versionIdentifier, this, versionUri);
    }

    /**
     * Create a version from the given source path. The source path is typically the path to the published content, so
     * it cannot be assumed the current version is in the root path of this VersionedContentItem.
     *
     * @param contentReader
     * @return
     */
    public ContentItemVersion createVersion(ContentReader contentReader, ContentWriter contentWriter) throws IOException, ZebedeeException {

        return createVersion(contentReader.getRootFolder(), contentReader, contentWriter);
    }

    /**
     * Copy only the files (not directories) from the given reader for the given uri.
     *
     * @param contentRoot
     * @param versionUri
     * @param contentReader
     * @throws IOException
     * @throws ZebedeeException
     */
    private void copyFilesIntoVersionDirectory(Path contentRoot, String versionUri, ContentReader contentReader,
                                               ContentWriter contentWriter) throws IOException, ZebedeeException {
        // Iterate the files in the source directory.
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(contentRoot.resolve(removeLeadingSlash(getUri())))) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) { // ignore directories
                    String uri = contentRoot.relativize(path).toString();

                    try (
                            Resource source = contentReader.getResource(uri);
                            InputStream inputStream = source.getData()
                    ) {
                        String filename = path.getFileName().toString();
                        Path versionPath = Paths.get(versionUri).resolve(filename);
                        contentWriter.write(inputStream, versionPath.toString());
                    }
                }
            }
        }
    }

    /**
     * Determine the version identifier of the next version.
     *
     * @param versionSourcePath
     * @return
     */
    private String createVersionIdentifier(Path versionSourcePath) {

        int version = getNextVersionNumber(versionSourcePath);
        String versionIdentifier = VERSION_PREFIX + version;
        return versionIdentifier;
    }

    public boolean versionExists(Content content) {

        Path path = content.get(getUri());
        if (path != null) {
            Path destinationPath = path.resolve(VersionedContentItem.getVersionDirectoryName());

            if (destinationPath != null && Files.exists(destinationPath) && destinationPath.toFile().listFiles().length > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean versionExists(ContentReader reader) {

        Path pathToVersionsFolder = reader.getRootFolder().resolve(getUri()).resolve(VersionedContentItem.getVersionDirectoryName());

        if (pathToVersionsFolder != null && Files.exists(pathToVersionsFolder) && pathToVersionsFolder.toFile().listFiles().length > 0) {
            return true;
        }

        return false;
    }
}
