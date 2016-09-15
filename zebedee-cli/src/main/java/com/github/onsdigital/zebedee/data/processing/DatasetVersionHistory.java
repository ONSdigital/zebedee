package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

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

                    System.out.println("");
                    System.out.println("uri = " + uri);

// check the previous versions for missing history
                    Path versionDirectory = datasetPath.resolve(VersionedContentItem.getVersionDirectoryName());

                    File[] files = new File(versionDirectory.toString()).listFiles();
                    Arrays.sort(files);
                    for (File file : files) {

                        Path path = file.toPath();

                        String versionUri = getUriFromPath(source, path);

                        System.out.print(":" + path.getFileName() + " ");

                        try {
                            Dataset datasetVersion = (Dataset) publishedContentReader.getContent(versionUri);

                            if (!datasetVersion.getUri().toString().endsWith("v1")) {

                                if (datasetVersion.getVersions() != null) {
                                    //System.out.println("number of versions in history: " + datasetVersion.getVersions().size());

                                    int version = Integer.parseInt(path.getFileName().toString().replace("v", ""));
                                    int expectedVersion = datasetVersion.getVersions().size() + 1;

                                    if (expectedVersion != version) {
                                        datasetsToFix.add(datasetPath);
                                        System.out.println("***** unexpected number of versions in " + version + " was expecting " + expectedVersion);
                                    }

                                    // if we have too many versions, then remove some
                                    if (expectedVersion > version) {
                                        expectedVersion--;
                                        while (expectedVersion >= version) {
                                            System.out.println("##### Removing version entry from v" + version + " removing entry v" + expectedVersion);
                                            expectedVersion--;
                                        }
                                    } else if (expectedVersion < version) {
                                        System.out.println("#### Need to repopulate some entries here");
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

// check the current version for missing history
                    if (dataset.getVersions() == null || dataset.getVersions().size() == 0) {
                        datasetsToFix.add(datasetPath);
                        System.out.println("****** Current version has empty versions array");
                    } else {

                        //System.out.println("Size of current version history: " + dataset.getVersions().size());

                        String lastVersionIdentifier = VersionedContentItem.getLastVersionIdentifier(datasetPath);
                        int lastVersion = Integer.parseInt(lastVersionIdentifier.replace("v", ""));
                        int expectedVersion = dataset.getVersions().size();

                        if (expectedVersion != lastVersion) {
                            datasetsToFix.add(datasetPath);
                            System.out.println("***** unexpected number of versions for current version " + lastVersionIdentifier + " was expecting " + expectedVersion);
                        }

                        // if we have too many versions, then remove some
                        if (expectedVersion > lastVersion) {
                            expectedVersion--;
                            while (expectedVersion >= lastVersion) {
                                System.out.println("##### Removing version entry from current version removing entry v" + expectedVersion);
                                expectedVersion--;
                            }
                        } else if (expectedVersion < lastVersion) {
                            System.out.println("##### Need to repopulate some entries for current version");
                        }


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
