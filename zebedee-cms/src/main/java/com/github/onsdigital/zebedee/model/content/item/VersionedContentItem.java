package com.github.onsdigital.zebedee.model.content.item;

import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.CollectionContentReader;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.util.URIUtils;

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

    private static final String VERSION_DIRECTORY = "previous";
    private static final String VERSION_PREFIX = "v";

    private final ContentWriter contentWriter;

    public VersionedContentItem(String uri, ContentWriter contentWriter) throws NotFoundException {
        super(uri);
        this.contentWriter = contentWriter;
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
     * @param contentRoot
     * @return
     */
    public ContentItemVersion createVersion(Path contentRoot, ContentReader contentReader) throws IOException, ZebedeeException {

        // create a new directory for the version. e.g. edition/previous/v1
        Path absolutePath = contentRoot.resolve(URIUtils.removeLeadingSlash(getUri().toString()));
        String versionIdentifier = createVersionIdentifier(absolutePath);
        String versionUri = String.format("%s/%s/%s", getUri(), getVersionDirectoryName(), versionIdentifier);

        copyFilesIntoVersionDirectory(contentRoot, versionUri, contentReader);

        return new ContentItemVersion(versionIdentifier, this, versionUri);
    }

    /**
     * Create a version from the given source path. The source path is typically the path to the published content, so
     * it cannot be assumed the current version is in the root path of this VersionedContentItem.
     *
     * @param contentReader
     * @return
     */
    public ContentItemVersion createVersion(ContentReader contentReader) throws IOException, ZebedeeException {

       return createVersion(contentReader.getRootFolder(), contentReader);
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
    private void copyFilesIntoVersionDirectory(Path contentRoot, String versionUri, ContentReader contentReader) throws IOException, ZebedeeException {

        // Iterate the files in the source directory.
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(contentRoot.resolve(removeLeadingSlash(getUri().toString())))) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) { // ignore directories
                    String uri = contentRoot.relativize(path).toString();
                    Resource source = contentReader.getResource(uri);

                    String filename = path.getFileName().toString();
                    Path versionPath = Paths.get(versionUri).resolve(filename);
                    try (InputStream inputStream = source.getData()){
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

        int version = 1;

        Path versionDirectory = versionSourcePath.resolve(getVersionDirectoryName());

        if (Files.exists(versionDirectory)) {
            version = versionDirectory.toFile().listFiles().length + 1;
        }

        return VERSION_PREFIX + version;
    }

    public boolean versionExists(Content content) {

        Path path = content.get(getUri().toString());
        if (path != null) {
            Path destinationPath = path.resolve(VersionedContentItem.getVersionDirectoryName());

            if (destinationPath != null && Files.exists(destinationPath) && destinationPath.toFile().listFiles().length > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean versionExists(CollectionContentReader reader) {

        Path pathToVersionsFolder = reader.getRootFolder().resolve(getUri().toString()).resolve(VersionedContentItem.getVersionDirectoryName());

        if (pathToVersionsFolder != null && Files.exists(pathToVersionsFolder) && pathToVersionsFolder.toFile().listFiles().length > 0) {
            return true;
        }

        return false;
    }
}
