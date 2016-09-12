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
import java.util.List;

public class DatasetVersionHistory extends SimpleFileVisitor<Path> {

    List<Path> datasetFiles = new ArrayList<>();
    Path root;

    public static void main(String[] args) {
        List<Path> paths = findDatasetsWithMissingVersionHistory(Paths.get("/Users/carlhembrough/dev/zebedee/zebedee/masterlive"));

//        for (Path path : paths) {
//            System.out.println(path);
//        }

        System.out.println("paths.size() = " + paths.size());

    }

    public static void findDatasetsWithMissingVersionHistory(String[] args) {
        // args[1] - source data directory.

        Path source = Paths.get(args[1]);

        findDatasetsWithMissingVersionHistory(source)
                .forEach(System.out::println);
    }

    private static List<Path> findDatasetsWithMissingVersionHistory(Path source) {
        List<Path> datasets = new DatasetVersionHistory().findDatasets(source);
        List<Path> versionedDatasets = filterDatasetsWithoutVersions(datasets);

        ContentReader publishedContentReader = new FileSystemContentReader(source); // read dataset / timeseries content from master

        List<Path> datasetsToFix = new ArrayList<>();

        for (Path versionedDataset : versionedDatasets) {
            String uri = "/" + source.relativize(versionedDataset).toString();
            uri = uri.substring(0, uri.length() - "/data.json".length()); // trim data.json off the end of the uri when using the reader.
            try {
                Page content = publishedContentReader.getContent(uri);

                if (content.getType() == PageType.dataset) {

                    Dataset dataset = (Dataset) content;

                    if (dataset.getVersions() == null || dataset.getVersions().size() == 0) {

                        System.out.println("uri = " + uri);

                        datasetsToFix.add(versionedDataset);
                    }
                }

            } catch (ZebedeeException | IOException e) {
                e.printStackTrace();
            }
        }


        return datasetsToFix;

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
