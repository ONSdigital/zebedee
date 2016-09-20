package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Version;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class DatasetVersionHistory extends SimpleFileVisitor<Path> {

    List<Path> datasetFiles = new ArrayList<>();
    Path root;

    public static void findDatasetsWithMissingVersionHistory(String[] args) {
        // args[1] - source data directory.

        Path source = Paths.get(args[1]);
        Path destination = Paths.get(args[2]);

        findDatasetsWithMissingVersionHistory(source, destination);
    }

    private static void findDatasetsWithMissingVersionHistory(Path source, Path destination) {
        List<Path> datasets = new DatasetVersionHistory().findDatasets(source);
        List<Path> versionedDatasets = filterDatasetsWithoutVersions(datasets);

        ContentReader publishedContentReader = new FileSystemContentReader(source); // read dataset / timeseries content from master
        ContentWriter collectionWriter = new ContentWriter(destination);
        ContentReader collectionReader = new FileSystemContentReader(destination);

        Set<Path> datasetsToFix = new HashSet<>();

        for (Path datasetJsonPath : versionedDatasets) {

            Path datasetPath = datasetJsonPath.getParent();
            String uri = getUriFromPath(source, datasetPath);
            try {

                // Read the data.json of the dataset and ensure its a csv dataset (not a timeseries dataset.)
                Page content = publishedContentReader.getContent(uri);
                if (content.getType() == PageType.dataset
                        && uri.startsWith("/employmentandlabourmarket")) {

                    Path versionDirectory = datasetPath.resolve(VersionedContentItem.getVersionDirectoryName());
                    File[] files = getOrderedFileList(versionDirectory);

                    for (File file : files) {
                        fixPreviousVersionDataset(
                                source,
                                publishedContentReader,
                                collectionReader,
                                collectionWriter,
                                datasetsToFix,
                                datasetPath,
                                uri,
                                versionDirectory,
                                file);
                    }

                    fixCurrentVersion(source, publishedContentReader, collectionReader, collectionWriter, datasetPath, uri, versionDirectory);

                }
            } catch (ZebedeeException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void fixCurrentVersion(Path source, ContentReader publishedContentReader, ContentReader collectionReader, ContentWriter collectionWriter, Path datasetPath, String uri, Path versionDirectory) throws IOException, ZebedeeException {

        Dataset dataset = getDataset(publishedContentReader, collectionReader, uri);

        // check the current version for missing history
        if (dataset.getVersions() == null || dataset.getVersions().size() == 0) {
            System.out.println("uri = " + uri);
            System.out.println("****** Current version has empty versions array");
        } else {

            //System.out.println("Size of current version history: " + dataset.getVersions().size());

            String lastVersionIdentifier = VersionedContentItem.getLastVersionIdentifier(datasetPath);
            int lastVersion = Integer.parseInt(lastVersionIdentifier.replace("v", ""));
            int expectedVersion = dataset.getVersions().size();

            if (expectedVersion != lastVersion) {
                System.out.println("uri = " + uri);
                System.out.println("***** unexpected number of versions for current version " + lastVersionIdentifier + " was expecting " + expectedVersion);
            }

            // if we have too many versions, then remove some
            if (expectedVersion > lastVersion) {
                expectedVersion--;
                while (expectedVersion >= lastVersion) {
                    System.out.println("##### Removing version entry from current version removing entry v" + expectedVersion);
                    expectedVersion--;
                    dataset.getVersions().remove(dataset.getVersions().size() - 1);
                    collectionWriter.writeObject(dataset, uri + "/data.json");
                }
            } else if (expectedVersion < lastVersion) {
                System.out.println("##### Need to repopulate some entries for current version");

                String previousVersionUri = getUriFromPath(source, versionDirectory.resolve(lastVersionIdentifier));
                Dataset previousDatasetVersion = getDataset(publishedContentReader, collectionReader, previousVersionUri);
                populateMissingDatasetData(dataset, previousVersionUri, previousDatasetVersion, new DateTime(2016, 8, 17, 8, 30).toDate());
                collectionWriter.writeObject(dataset, uri + "/data.json");
            }
        }
    }

    private static File[] getOrderedFileList(Path versionDirectory) {
        File[] files = new File(versionDirectory.toString()).listFiles();
        Arrays.sort(files, (o1, o2) -> {

            int o1version = Integer.parseInt(o1.getName().toString().replace("v", ""));
            int o2version = Integer.parseInt(o2.getName().toString().replace("v", ""));

            return Integer.compare(o1version, o2version);
        });
        return files;
    }

    private static void fixPreviousVersionDataset(Path source, ContentReader publishedContentReader, ContentReader collectionReader, ContentWriter collectionWriter, Set<Path> datasetsToFix, Path datasetPath, String uri, Path versionDirectory, File file) {
        Path path = file.toPath();
        String versionUri = getUriFromPath(source, path);


        try {

            Dataset datasetVersion = getDataset(publishedContentReader, collectionReader, versionUri);

            if (!datasetVersion.getUri().toString().endsWith("v1")) {

                if (datasetVersion.getVersions() != null) {
                    //System.out.println("number of versions in history: " + datasetVersion.getVersions().size());

                    int version = Integer.parseInt(path.getFileName().toString().replace("v", ""));
                    int expectedVersion = datasetVersion.getVersions().size() + 1;

                    if (expectedVersion != version) {
                        System.out.println("uri = " + uri);
                        datasetsToFix.add(datasetPath);
                        System.out.println("***** unexpected number of versions in " + version + " was expecting " + expectedVersion);
                    }

                    // if we have too many versions, then remove some
                    if (expectedVersion > version) {
                        expectedVersion--;
                        while (expectedVersion >= version) {
                            System.out.println("##### Removing version entry from v" + version + " removing entry v" + expectedVersion);
                            expectedVersion--;
                            datasetVersion.getVersions().remove(datasetVersion.getVersions().size() - 1);
                            collectionWriter.writeObject(datasetVersion, versionUri + "/data.json");
                        }
                    } else if (expectedVersion < version) {
                        System.out.println("#### Need to repopulate some entries here");


                        try {
                            // read the previous version to copy entries from
                            String previousVersionUri = getUriFromPath(source, versionDirectory.resolve("v" + (version - 1)));

                            Dataset previousDatasetVersion = getDataset(publishedContentReader, collectionReader, previousVersionUri);

                            populateMissingDatasetData(datasetVersion, previousVersionUri, previousDatasetVersion, new DateTime(2016, 7, 20, 8, 30).toDate());

                            collectionWriter.writeObject(datasetVersion, versionUri + "/data.json");

                        } catch (ZebedeeException | IOException e) {
                            e.printStackTrace();
                        }

                    }

                } else {
                    datasetsToFix.add(datasetPath);
                    System.out.println("***** no version history in previous version to use" + datasetVersion.getVersions().size());
                }
            } else {
                //System.out.println("V1 Will not have a previous version");
            }

        } catch (ZebedeeException | IOException e) {
            e.printStackTrace();
        }
    }

    private static Dataset getDataset(ContentReader publishedContentReader, ContentReader collectionReader, String versionUri) throws IOException, ZebedeeException {
        Dataset datasetVersion;
        try {
            datasetVersion = (Dataset) collectionReader.getContent(versionUri);
        } catch (ZebedeeException e) {
            datasetVersion = (Dataset) publishedContentReader.getContent(versionUri);
        }
        return datasetVersion;
    }

    private static void populateMissingDatasetData(Dataset datasetVersion, String previousVersionUri, Dataset previousDatasetVersion, Date updateDate) {
        datasetVersion.setVersions(previousDatasetVersion.getVersions());
        Version missingVersion = new Version();
        missingVersion.setUri(URI.create(previousVersionUri));
        missingVersion.setUpdateDate(updateDate);
        missingVersion.setCorrectionNotice("");
        missingVersion.setLabel("");
        datasetVersion.getVersions().add(missingVersion);

        if (StringUtils.isEmpty(datasetVersion.getDescription().getTitle()))
            datasetVersion.getDescription().setTitle(previousDatasetVersion.getDescription().getTitle());

        if (StringUtils.isEmpty(datasetVersion.getDescription().getSummary()))
            datasetVersion.getDescription().setSummary(previousDatasetVersion.getDescription().getSummary());

        if (StringUtils.isEmpty(datasetVersion.getDescription().getMetaDescription()))
            datasetVersion.getDescription().setMetaDescription(previousDatasetVersion.getDescription().getMetaDescription());

        if (datasetVersion.getDescription().getContact() == null)
            datasetVersion.getDescription().setContact(previousDatasetVersion.getDescription().getContact());
    }

    private static String getUriFromPath(Path source, Path datasetPath) {
        String uri = "/" + source.relativize(datasetPath).toString();
        if (uri.endsWith("/data.json"))
            uri = uri.substring(0, uri.length() - "/data.json".length()); // trim data.json off the end of the uri when using the reader.
        return uri;
    }

    private static List<Path> filterDatasetsWithoutVersions(List<Path> datasets) {
        List<Path> versionedDatasets = new ArrayList<>();
        for (Path dataset : datasets) {

            // check if the dataset has previous directory

            Path datasetDirectory = dataset.getParent();
            Path previousDirectory = datasetDirectory.resolve("previous");

            if (Files.exists(previousDirectory)) {
                versionedDatasets.add(dataset);
            }
        }
        return versionedDatasets;
    }

    public List<Path> findDatasets(Path root) {
        this.root = root;

        try {
            Files.walkFileTree(root, this);
        } catch (NoSuchFileException e) {
            return new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this.datasetFiles;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attr) throws IOException {
        // Get the uri
        String uri = "/" + root.relativize(path).toString();

        // Check json files in timeseries directories (excluding versions)
        if (uri.endsWith("data.json")
                && uri.toString().contains("/datasets/")
                && uri.toString().contains("/current/")
                && !VersionedContentItem.isVersionedUri(uri)) {
            this.datasetFiles.add(path);
        }
        return FileVisitResult.CONTINUE;
    }
}
