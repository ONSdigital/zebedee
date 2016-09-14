package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatasetVersionHistory extends SimpleFileVisitor<Path> {

    List<Path> datasetFiles = new ArrayList<>();
    Path root;

    public static void main(String[] args) {
        Set<Path> paths = findDatasetsWithMissingVersionHistory(Paths.get("/Users/carlhembrough/dev/zebedee/zebedee/masterlive"));

//        for (Path path : paths) {
//            System.out.println(path);
//        }

        System.out.println("paths.size() = " + paths.size());

    }

    public static void findDatasetsWithMissingVersionHistory(String[] args) {
        // args[1] - source data directory.

        Path source = Paths.get(args[1]);

        Set<Path> paths = findDatasetsWithMissingVersionHistory(source);

        System.out.println("paths.size() = " + paths.size());
    }

    private static Set<Path> findDatasetsWithMissingVersionHistory(Path source) {
        List<Path> datasets = new DatasetVersionHistory().findDatasets(source);
        List<Path> versionedDatasets = filterDatasetsWithoutVersions(datasets);

        ContentReader publishedContentReader = new FileSystemContentReader(source); // read dataset / timeseries content from master

        Set<Path> datasetsToFix = new HashSet<>();

        for (Path datasetJsonPath : versionedDatasets) {

            Path datasetPath = datasetJsonPath.getParent();
            String uri = getUriFromPath(source, datasetPath);
            try {

                // Read the data.json of the dataset and ensure its a csv dataset (not a timeseries dataset.)
                Page content = publishedContentReader.getContent(uri);
                if (content.getType() == PageType.dataset) {
                    Dataset dataset = (Dataset) content;

// check the previous versions for missing history
                    Path versionDirectory = datasetPath.resolve(VersionedContentItem.getVersionDirectoryName());
                    Files.list(versionDirectory).forEach(path -> {
                        String versionUri = getUriFromPath(source, path);
                        try {
                            Dataset datasetVersion = (Dataset) publishedContentReader.getContent(versionUri);

                            //System.out.println("Checking version: " + versionUri);

                            if (!datasetVersion.getUri().toString().endsWith("v1")) {

                                if (datasetVersion.getVersions() != null) {
                                    //System.out.println("number of versions in history: " + datasetVersion.getVersions().size());

                                    String versionFilename = path.getFileName().toString();
                                    String expectedFilename = "v" + (datasetVersion.getVersions().size() + 1);

                                    if (!expectedFilename.equals(versionFilename)) {
                                        datasetsToFix.add(datasetPath);
                                        System.out.println("uri = " + uri);
                                        System.out.println("***** unexpected number of versions in " + versionFilename);
                                    }

                                } else {
                                    datasetsToFix.add(datasetPath);
                                    System.out.println("uri = " + uri);
                                    System.out.println("***** no version history in previous version to use" + datasetVersion.getVersions().size());
                                }
                            } else {
                                //System.out.println("V1 Will not have a previous version");
                            }
                        } catch (ZebedeeException | IOException e) {
                            e.printStackTrace();
                        }
                    });

// check the current version for missing history
                    if (dataset.getVersions() == null || dataset.getVersions().size() == 0) {
                        datasetsToFix.add(datasetPath);
                        System.out.println("uri = " + uri);
                        System.out.println("****** Current version has empty versions array");
                    } else {

                        //System.out.println("Size of current version history: " + dataset.getVersions().size());

                        String lastVersionIdentifier = VersionedContentItem.getLastVersionIdentifier(datasetPath);
                        String expectedFilename = "v" + (dataset.getVersions().size() + 1);

                        if (!expectedFilename.equals(lastVersionIdentifier)) {
                            datasetsToFix.add(datasetPath);
                            System.out.println("uri = " + uri);
                            System.out.println("***** unexpected number of versions for current version ");
                        }

                        // ---------------------------------------------------------------------------------
                        // read the versions array from the most recent version

//                        String lastVersionIdentifier = VersionedContentItem.getLastVersionIdentifier(datasetPath);
//                        Path lastVersionPath = datasetPath.resolve("previous").resolve(lastVersionIdentifier);
//                        String lastVersionUri = getUriFromPath(source, lastVersionPath);
//                        Dataset lastVersion = (Dataset) publishedContentReader.getContent(lastVersionUri);
//
//                        System.out.println("lastVersionUri = " + lastVersionUri);
//
//                        if (lastVersion.getVersions() != null && lastVersion.getVersions().size() > 0) {
//                            List<Version> newVersionsList = lastVersion.getVersions();
//
//                            for (Version version : newVersionsList) {
//                                System.out.println("versionUri = " + version.getUri());
//
//                                Dataset versionPage = (Dataset) publishedContentReader.getContent(version.getUri().toString());
//
//                                if ((versionPage.getVersions() == null || versionPage.getVersions().size() == 0)) {
//                                    if (!version.getUri().toString().endsWith("v1")) {
//                                        System.out.println("********* versions list should not be empty in this previous version");
//
//                                        // - populate the versions list from the previous version, adding this version
//
//                                    } else {
//                                        System.out.println("V1 Will not have a previous version");
//                                    }
//                                } else {
//                                    System.out.println("versionPage.getVersions().size() = " + versionPage.getVersions().size());
//                                }
//                            }
//                        } else {
//                            System.out.println("***** no version history in previous version to use");
//                        }
                        // add another version for the last version

                    }


                }

            } catch (ZebedeeException | IOException e) {
                e.printStackTrace();
            }
        }


        return datasetsToFix;

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
