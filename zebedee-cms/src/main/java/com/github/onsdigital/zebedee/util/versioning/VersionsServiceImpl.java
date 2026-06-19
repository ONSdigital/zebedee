package com.github.onsdigital.zebedee.util.versioning;

import com.github.onsdigital.zebedee.content.page.statistics.dataset.Version;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;
import com.github.onsdigital.zebedee.session.model.Session;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class VersionsServiceImpl implements VersionsService {

    static final String VERSION_URI = "/previous/";
    static final String VERSION_FILE_REGEX = "/previous/v\\d+/.+";
    static final Pattern VERSION_DIR_PATTERN = Pattern.compile("/previous/v\\d+");
    static final Pattern VALID_VERSION_DIR_PATTERN = Pattern.compile("v\\d+");

    /**
     * Determined if the input value is a version uri. Return true if input not null, not empty and contains
     * substring matching the regex "/previous/v\\d+"
     *
     * @param uri the value to test.
     * @return Return true if input not null, not empty and contains substring matching the regex "/previous/v\\d+"
     * otherwise return false
     */
    @Override
    public boolean isVersionedURI(String uri) {
        return StringUtils.isNotEmpty(uri) && VERSION_DIR_PATTERN.matcher(uri).find();
    }

    /**
     * Return an optional containing the the version directory name from the URI if it exists otherwise return an
     * empty {@link Optional}. Example: Given uri "/a/b/c/previous/v1/data.json" returns "v1".
     *
     * @param uri the taxonomy uri to get the version from.
     * @return an {@link Optional} with the uri version string if it exists and the input is a valid version uri.
     * Otherwise return an empyt optional.
     */
    @Override
    public Optional<String> getVersionNameFromURI(String uri) {
        Optional<String> result = Optional.empty();

        if (isVersionedURI(uri)) {
            Matcher matcher = VERSION_DIR_PATTERN.matcher(uri);

            if (matcher.find()) {
                result = Optional.of(matcher.group(0).replace(VERSION_URI, ""));
            }
        }

        return result;
    }

    @Override
    public boolean isVersionDir(File file) {
        return file != null && file.isDirectory() && VALID_VERSION_DIR_PATTERN.matcher(file.getName()).matches();
    }

    /**
     * Check if the content version exists.
     *
     * @param cmsReader  a {@link ZebedeeReader} to read content from the collection and published content directories.
     * @param collection the collection to check.
     * @param session    the user {@link Session} making the request.
     * @param version    the {@link Version} to find.
     * @return true if the content version exists in either the collection or the published content.
     * @throws ZebedeeException unexpected error checking version.
     * @throws IOException      unexpected error checking version.
     */
    boolean versionExists(ZebedeeReader cmsReader, Collection collection, Session session, Version version)
            throws ZebedeeException, IOException {
        boolean exists = true;
        String uri = version.getUri().toString();

        try {
            cmsReader.getCollectionContent(collection.getId(), session.getId(), uri);
        } catch (NotFoundException ex) {
            try {
                cmsReader.getPublishedContent(uri);
            } catch (NotFoundException ex1) {
                exists = false;
            }
        }

        return exists;
    }
}
