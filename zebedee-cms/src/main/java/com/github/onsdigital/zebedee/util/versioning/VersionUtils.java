package com.github.onsdigital.zebedee.util.versioning;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Version;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
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

public class VersionUtils {

    static final String VERSION_URI = "/previous/v";
    static final String VERSION_PREFIX = "v";
    static final Pattern VERSION_DIR_PATTERN = Pattern.compile("/previous/v\\d+");
    static final Pattern VALID_VERSION_DIR_PATTERN = Pattern.compile("v\\d+");

    private static final String VERSION_NOT_FOUND = "error verifying collection legacy dataset - could not find " +
            "expected content {0} in either collection {1} or published content - please investigate and fix this " +
            "issue before continuing with approval";

    private VersionUtils() {
        // only static methods
    }

    /**
     * Return an optional containing the the version directory name from the URI if it exists otherwise return an
     * empty {@link Optional}. Example: Given uri "/a/b/c/previous/v1/data.json" returns "v1".
     *
     * @param uri the taxonomy uri to get the version from.
     * @return an {@link Optional} with the uri version string if it exists and the input is a valid version uri.
     * Otherwise return an empyt optional.
     */
    public static Optional<String> getVersionFromURI(String uri) {
        Optional<String> result = Optional.empty();

        if (isVersionedUri(uri)) {
            Matcher matcher = VERSION_DIR_PATTERN.matcher(uri);

            if (matcher.find()) {
                result = Optional.of(matcher.group(0).replace(VERSION_URI, ""));
            }
        }

        return result;
    }

    public static boolean isVersionedUri(String uri) {
        return StringUtils.isNotEmpty(uri) && uri.contains(VERSION_URI);
    }


    public static int getVersionNumber(String uri) {
        Integer version = -1;
        Optional<String> vOpt = getVersionFromURI(uri);

        if (vOpt.isPresent()) {
            version = Integer.valueOf(vOpt.get().replaceFirst("v", ""));
        }

        return version;
    }

    public static Comparator<Version> versionComparator() {
        return (Version v1, Version v2) -> {
            Integer foo = getVersionNumber(v1.getUri().toString());
            Integer bar = getVersionNumber(v2.getUri().toString());
            return foo.compareTo(bar);
        };
    }

    public static boolean isVersionDir(File file) {
        return file.isDirectory() && VALID_VERSION_DIR_PATTERN.matcher(file.getName()).matches();
    }


    public static void verifyCollectionDatasets(Collection collection, CollectionReader reader, Session session)
            throws ZebedeeException, IOException, VersionNotFoundException {

        List<Dataset> datasets = getDatasetsInCollection(reader);

        if (datasets != null && !datasets.isEmpty()) {
            ZebedeeReader zebedeeReader = new ZebedeeReader(Root.zebedee.getPublished().path.toString(), null);
            verifyCollectionDatasets(zebedeeReader, collection, session, datasets);

            info().collectionID(collection)
                    .user(session)
                    .data("datasets",
                            datasets.stream()
                                    .map(ds -> ds.getDescription().getTitle())
                                    .collect(Collectors.toList()))
                    .log("collection legacy datasets verified successfully");
        }
    }

    static List<Dataset> getDatasetsInCollection(CollectionReader reader) throws ZebedeeException, IOException {
        List<Dataset> datasets = new ArrayList<>();

        for (String uri : reader.getReviewed().listUris()) {
            if (!uri.endsWith("data.json") || VersionedContentItem.isVersionedUri(uri)) {
                continue;
            }

            Path parent = Paths.get(uri).getParent();

            Page page = reader.getReviewed().getContent(parent.toString());
            if (PageType.dataset.equals(page.getType())) {
                datasets.add((Dataset) page);
            }
        }

        return datasets;
    }

    static void verifyCollectionDatasets(ZebedeeReader zebedeeReader, Collection collection,
                                         Session session, List<Dataset> datasets) throws ZebedeeException, IOException,
            VersionNotFoundException {
        for (Dataset ds : datasets) {
            info().collectionID(collection)
                    .data("dataset", ds.getDescription().getTitle())
                    .log("verifying collection dataset versions");

            for (Version version : ds.getVersions()) {
                String uri = version.getUri().toString();

                try {
                    zebedeeReader.getCollectionContent(collection.getId(), session.getId(), uri);
                } catch (NotFoundException ex) {
                    try {
                        zebedeeReader.getPublishedContent(uri);
                    } catch (NotFoundException ex1) {
                        throw new VersionNotFoundException(format(VERSION_NOT_FOUND, uri, collection.getId()));
                    }
                }
            }
        }
    }
}
