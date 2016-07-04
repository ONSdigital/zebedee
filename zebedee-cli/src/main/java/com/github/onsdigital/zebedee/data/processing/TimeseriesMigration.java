package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.TimeSeriesDataset;
import com.github.onsdigital.zebedee.data.processing.setup.DataIndexBuilder;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TimeseriesMigration {

    public static void main(String[] args) throws ZebedeeException, IOException, InterruptedException, URISyntaxException {
        migrateTimeseries(Paths.get("/Users/carlhembrough/dev/zebedee/zebedee/masterlive"), Paths.get("/Users/carlhembrough/dev/zebedee/migration"));
    }

    public static void migrateTimeseries(String[] args) throws InterruptedException, ZebedeeException, IOException, URISyntaxException {
        // args[1] - source data directory
        // args[2] - destination directory to save the updated timeseries (can be a collection or master)

        Path source = Paths.get(args[1]);
        Path destination = Paths.get(args[2]);

        migrateTimeseries(source, destination);
    }

    private static void migrateTimeseries(Path source, Path destination) throws ZebedeeException, IOException, InterruptedException, URISyntaxException {

        // - build data index from existing timeseries structure
        // - find all datasets on the site so we can re-run them to generate timeseries
        // - order by date
        // - create a collection for the whole migration
        // - run the timeseries generator for each version of the dataset into the collection
        // - replace the old timeseries files with timeseries landing pages

        // - merge the collection files into master on publishing when deployment happens

        // - take one web server at a time, copy from publishing to the web server + deploy latest code

        ContentReader publishedContentReader = new FileSystemContentReader(source); // read dataset / timeseries content from master

        ContentReader destinationContentReader = new FileSystemContentReader(destination);
        ContentWriter destinationContentWriter = new ContentWriter(destination); // write output content to collection

        System.out.println("Building data index...");
        DataIndex dataIndex = DataIndexBuilder.buildDataIndex(publishedContentReader); // build the dataindex of existing timeseries to determine location of output

        // find all dataset files on the site including versions.
        List<TimeseriesDatasetFiles> datasetDownloads = getTimeseriesDatasets(source);

        // load the pages
        List<TimeseriesDatasetContainer> datasetContainers = loadPageObjectsForDatasets(publishedContentReader, datasetDownloads);

        // sort by release date.
        sortDatasetsByReleaseDate(datasetContainers);

        System.out.println("Number of datasets: " + datasetContainers.size());

        int count = 1;

        // iterate each dataset found
        for (TimeseriesDatasetContainer datasetContainer : datasetContainers) {

            System.out.println("Processing dataset : " + count + " of " + datasetContainers.size());
            count++;

            TimeSeriesDataset dataset = datasetContainer.timeSeriesDataset;
            TimeseriesDatasetFiles datasetFiles = datasetContainer.timeseriesDatasetFiles;

            System.out.println("------------------------------------------------------");
            System.out.println("dataset.getUri() = " + dataset.getUri());
            System.out.println("dataset.getDescription().getReleaseDate() = " + dataset.getDescription().getReleaseDate());

            String datasetUri = dataset.getUri().toString();
            System.out.println("datasetUri = " + datasetUri);

            // if processing a versioned dataset, determine the root URL of the dataset.
            if (VersionedContentItem.isVersionedUri(datasetUri)) {
                datasetUri = VersionedContentItem.resolveBaseUri(datasetUri);
                System.out.println("VERSIONED datasetUri = " + datasetUri);
            }

            try {

                // attempt to read the existing dataset if it exists
                destinationContentReader.getContent(datasetUri);

                System.out.println("--- dataset already exists - creating version");

                // create a version of the existing dataset.
                VersionedContentItem versionedContentItem = new VersionedContentItem(datasetUri);
                versionedContentItem.createVersion(destinationContentReader, destinationContentWriter);

                // copy the dataset and CSDB from the version directory to the destination.
                copyDatasetJsonToDestination(destinationContentWriter, dataset, datasetUri);
                copyCsdbFileToLocation(publishedContentReader, destinationContentWriter, datasetFiles, datasetUri);

            } catch (NotFoundException e) {

                // if the dataset does not alreadt exist in the destination, just move it from source into place.
                System.out.println("--- dataset does not exist - creating copy...");
                copyDatasetJsonToDestination(destinationContentWriter, dataset, datasetUri);
                copyCsdbFileToLocation(publishedContentReader, destinationContentWriter, datasetFiles, datasetUri);
            }

            // create the data publication object to pass into the data processor.
            DataPublication dataPublication = new DataPublication(publishedContentReader, destinationContentReader, datasetUri);

            // process the dataset as it would normally be.
            if (dataPublication.hasUpload()) {
                boolean saveTimeSeries = true;
                System.out.println("--- Processing dataset... ");
                dataPublication.process(publishedContentReader, destinationContentReader, destinationContentWriter, saveTimeSeries, dataIndex, new ArrayList<>());
            }
        }
    }

    private static void copyCsdbFileToLocation(ContentReader publishedContentReader, ContentWriter destinationContentWriter, TimeseriesDatasetFiles datasetFiles, String datasetUri) throws IOException, ZebedeeException {
        // copy csdb file to new location
        String csdbSource = datasetFiles.getCsdbPath().toString();
        String csdbDestination = datasetUri + "/" + datasetFiles.getCsdbPath().getFileName().toString();

        System.out.println("Copying CSDB file from " + csdbSource + " to " + csdbDestination);

        try (InputStream inputStream = publishedContentReader.getResource(csdbSource).getData()) {
            destinationContentWriter.write(inputStream, csdbDestination);
        }
    }

    private static void copyDatasetJsonToDestination(ContentWriter destinationContentWriter, TimeSeriesDataset dataset, String datasetUri) throws IOException, BadRequestException {
        // clear downloads
        dataset.setDownloads(new ArrayList<>());
        // clear versions
        dataset.setVersions(new ArrayList<>());

        // write json to destination
        String datasetPath = datasetUri + "/data.json";
        System.out.println("Writing dataset file to " + datasetPath);
        destinationContentWriter.writeObject(dataset, datasetPath);
    }

    private static List<TimeseriesDatasetContainer> loadPageObjectsForDatasets(ContentReader publishedContentReader, List<TimeseriesDatasetFiles> datasetDownloads) throws ZebedeeException, IOException {
        List<TimeseriesDatasetContainer> datasetContainers = new ArrayList<>();

        for (TimeseriesDatasetFiles TimeseriesDatasetFile : datasetDownloads) {
            System.out.println("------------------------loadPageObjectsForDatasets------------------------------");
            System.out.println("CsdbPath() = " + TimeseriesDatasetFile.getCsdbPath());
            System.out.println("DatasetPath() = " + TimeseriesDatasetFile.getDatasetPath());

            Page page = publishedContentReader.getContent(TimeseriesDatasetFile.getRootPath().toString());

            if (page instanceof TimeSeriesDataset == false) {
                System.out.println("This is not a timeseries dataset.");
                continue;
            }

            TimeSeriesDataset timeseriesDatasetPage = (TimeSeriesDataset) page;

            TimeseriesDatasetContainer timeseriesDatasetContainer = new TimeseriesDatasetContainer();
            timeseriesDatasetContainer.timeSeriesDataset = timeseriesDatasetPage;
            timeseriesDatasetContainer.timeseriesDatasetFiles = TimeseriesDatasetFile;
            datasetContainers.add(timeseriesDatasetContainer);
            System.out.println("Uri = " + timeseriesDatasetPage.getUri());
            System.out.println("release date = " + timeseriesDatasetPage.getDescription().getReleaseDate());

        }
        return datasetContainers;
    }

    private static void sortDatasetsByReleaseDate(List<TimeseriesDatasetContainer> datasets) {
        Comparator<TimeseriesDatasetContainer> byReleaseDate = (ds1, ds2) -> ds1.timeSeriesDataset.getDescription().getReleaseDate()
                .compareTo(ds2.timeSeriesDataset.getDescription().getReleaseDate());
        datasets.sort(byReleaseDate);
    }

    /**
     * find all CSDB files to determine the timeseries datasets.
     *
     * @param source
     * @return
     */
    private static List<TimeseriesDatasetFiles> getTimeseriesDatasets(Path source) {
        CsdbFinder csdbFinder = new CsdbFinder();
        boolean includeVersions = true;
        csdbFinder.find(source, includeVersions);
        List<TimeseriesDatasetFiles> datasetDownloads = new ArrayList<>();
        for (String uri : csdbFinder.uris) {
            datasetDownloads.add(new TimeseriesDatasetFiles(Paths.get(uri)));
        }
        return datasetDownloads;
    }
}
