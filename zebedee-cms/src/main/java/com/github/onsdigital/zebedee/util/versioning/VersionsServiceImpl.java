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

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;
import static java.text.MessageFormat.format;

public class VersionsServiceImpl implements VersionsService {

    static final String VERSION_URI = "/previous/";
    static final Pattern VERSION_DIR_PATTERN = Pattern.compile("/previous/v\\d+");
    static final Pattern VALID_VERSION_DIR_PATTERN = Pattern.compile("v\\d+");

    private static final String VERSION_NOT_FOUND = "error verifying collection legacy dataset - could not find " +
            "expected content {0} in either collection {1} or published content - please investigate and fix this " +
            "issue before continuing with approval";

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
            verifyDatasetVersions(cmsReader, collection, session, datasets);

            info().collectionID(collection)
                    .user(session)
                    .data("datasets",
                            datasets.stream()
                                    .map(ds -> ds.getDescription().getTitle())
                                    .collect(Collectors.toList()))
                    .log("collection legacy datasets verified successfully");
        }
    }

    List<Dataset> getDatasetsInCollection(CollectionReader reader) throws ZebedeeException, IOException {
        List<Dataset> datasets = new ArrayList<>();

        for (String uri : reader.getReviewed().listUris()) {

            if (isDataJson(uri) && !isVersionedURI(uri)) {

                Path parent = Paths.get(uri).getParent();

                Page page = reader.getReviewed().getContent(parent.toString());
                if (PageType.dataset.equals(page.getType())) {
                    datasets.add((Dataset) page);
                }
            }
        }

        return datasets;
    }

    void verifyDatasetVersions(ZebedeeReader cmsReader, Collection collection, Session session,
                               List<Dataset> datasets) throws ZebedeeException, IOException,
            VersionNotFoundException {
        for (Dataset ds : datasets) {
            info().collectionID(collection)
                    .data("dataset", ds.getDescription().getTitle())
                    .log("verifying collection dataset versions");

            for (Version version : ds.getVersions()) {
                String uri = version.getUri().toString();

                try {
                    cmsReader.getCollectionContent(collection.getId(), session.getId(), uri);
                } catch (NotFoundException ex) {
                    try {
                        cmsReader.getPublishedContent(uri);
                    } catch (NotFoundException ex1) {
                        throw new VersionNotFoundException(format(VERSION_NOT_FOUND, uri, collection.getId()));
                    }
                }
            }
        }
    }

    boolean isDataJson(String uri) {
        return StringUtils.isNotEmpty(uri) && uri.endsWith("data.json");
    }
}
