package com.github.onsdigital.zebedee.util.versioning;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Version;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;
import com.github.onsdigital.zebedee.session.model.Session;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.util.versioning.VersionNotFoundException.versionsNotFoundException;

public class VersionsServiceImpl implements VersionsService {

    static final String VERSION_URI = "/previous/";
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

    /**
     * Return the version number of a uri if the input value is a valid version uri. A valid version uri is a non
     * null, non empty string containing the substring matching the regex "/previous/v\\d+".
     * Example: "/a/b/c/previous/v1/data.json" will return 1
     *
     * @param uri the input proccess.
     * @return the version number an int if the input is valid. Returns the a default of -1 for all invalid inputs.
     */
    @Override
    public int getVersionNumberFromURI(String uri) {
        Integer version = -1;
        Optional<String> vOpt = getVersionNameFromURI(uri);

        if (vOpt.isPresent()) {
            version = Integer.valueOf(vOpt.get().replaceFirst("v", ""));
        }

        return version;
    }

    @Override
    public Comparator<Version> versionComparator() {
        return (Version v1, Version v2) -> {
            Integer foo = getVersionNumberFromURI(v1.getUri().toString());
            Integer bar = getVersionNumberFromURI(v2.getUri().toString());
            return foo.compareTo(bar);
        };
    }

    @Override
    public boolean isVersionDir(File file) {
        return file != null && file.isDirectory() && VALID_VERSION_DIR_PATTERN.matcher(file.getName()).matches();
    }


    @Override
    public void verifyCollectionDatasets(ZebedeeReader cmsReader, Collection collection,
                                         CollectionReader reader, Session session)
            throws ZebedeeException, IOException, VersionNotFoundException {

        List<Dataset> datasets = getDatasetsInCollection(reader);

        if (datasets != null && !datasets.isEmpty()) {

            List<MissingVersion> missingVersions = getMissingDatasetVersions(cmsReader, collection, session, datasets);

            if (!missingVersions.isEmpty()) {
                throw versionsNotFoundException(missingVersions);
            }
        }
    }

    List<Dataset> getDatasetsInCollection(CollectionReader reader) throws ZebedeeException, IOException {
        List<String> filtered = reader.getReviewed()
                .listUris()
                .stream()
                .filter(uri -> StringUtils.isNotEmpty(uri) && uri.contains("/datasets/") && isDataJson(uri) && !isVersionedURI(uri))
                .collect(Collectors.toList());

        List<Dataset> datasets = new ArrayList<>();

        for (String uri : filtered) {
            Path parent = Paths.get(uri).getParent();
            Page page = reader.getReviewed().getContent(parent.toString());

            if (PageType.dataset.equals(page.getType())) {
                datasets.add((Dataset) page);
            }
        }

        return datasets;
    }

    /**
     * Get a {@link List} of {@link MissingVersion} if any {@link Dataset} {@link Version} can not be found.
     *
     * @param cmsReader  a {@link ZebedeeReader} to read content from the collection and published content directories.
     * @param collection the collection to check.
     * @param session    the user {@link Session} making the request.
     * @param datasets   the datasets to check.
     * @return a {@link List} of {@link MissingVersion} of any missing dataset versions.
     * @throws ZebedeeException unexpected error checking version.
     * @throws IOException      unexpected error checking version.
     */
    List<MissingVersion> getMissingDatasetVersions(ZebedeeReader cmsReader, Collection collection, Session session,
                                                   List<Dataset> datasets) throws ZebedeeException, IOException {
        List<MissingVersion> missingVersions = new ArrayList<>();

        for (Dataset dataset : datasets) {
            missingVersions.addAll(getMissingVersions(cmsReader, collection, session, dataset));
        }

        return missingVersions;
    }

    /**
     * Identify any missing dataset versions. Uses the {@link Dataset#versions} to determined what versions should
     * exist.
     *
     * @param cmsReader  a {@link ZebedeeReader} to read content from the collection and published content directories.
     * @param collection the collection to check.
     * @param session    the user {@link Session} making the request.
     * @param dataset    the {@link Dataset} to check.
     * @return a {@link List} of {@link MissingVersion} for the missing dataset version.
     * @throws ZebedeeException unexpected error checking version.
     * @throws IOException      unexpected error checking version.
     */
    List<MissingVersion> getMissingVersions(ZebedeeReader cmsReader, Collection collection, Session session, Dataset dataset)
            throws ZebedeeException, IOException {

        List<MissingVersion> missingVersions = new ArrayList<>();

        for (Version version : dataset.getVersions()) {

            if (!versionExists(cmsReader, collection, session, version)) {
                missingVersions.add(new MissingVersion(dataset, version));
            }
        }

        return missingVersions;
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

    boolean isDataJson(String uri) {
        return StringUtils.isNotEmpty(uri) && uri.endsWith("data.json");
    }

    @Override
    public boolean isVersionOf(String target, String input) {
        if (!target.endsWith("/current")) {
            return false;
        }

        String regex = target +"/previous/v\\d+/.+";
        return Pattern.compile(regex).matcher(input).matches();
    }
}
