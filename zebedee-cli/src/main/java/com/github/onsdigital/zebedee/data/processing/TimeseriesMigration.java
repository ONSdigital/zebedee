package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.TimeSeriesDataset;
import com.github.onsdigital.zebedee.data.processing.setup.DataIndexBuilder;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TimeseriesMigration {

    public static void main(String[] args) throws ZebedeeException, IOException, InterruptedException, URISyntaxException {
        migrateTimeseries(Paths.get("/Users/carlhembrough/dev/zebedee/zebedee/master"), Paths.get("/Users/carlhembrough/dev/zebedee/migration"));
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

        ContentReader contentReader = new FileSystemContentReader(source); // read dataset / timeseries content from master
        ContentWriter contentWriter = new ContentWriter(destination); // write output content to collection

        DataIndex dataIndex = DataIndexBuilder.buildDataIndex(contentReader); // build the dataindex of existing timeseries to determine location of output

        // find all dataset files on the site including versions.
        List<DataPublication> datasets = getAllDatasets(source, contentReader);

        // sort the datasets found by release date

//        for (TimeSeriesDataset dataset : datasets) {
//            System.out.println("Uri = " + dataset.getUri());
//            System.out.println("release date = " + dataset.getDescription().getReleaseDate());
//        }


        for (DataPublication dataPublication : datasets) {
            // If a file upload exists
            if (dataPublication.hasUpload()) {
                boolean saveTimeSeries = false;
                dataPublication.process(contentReader, contentReader, contentWriter, saveTimeSeries, dataIndex, new ArrayList<>());
            }
        }
    }

    private static void sortDatasetsByReleaseDate(List<DataPublication> datasets) {
        Comparator<DataPublication> byReleaseDate = (ds1, ds2) -> ds1.getDetails().datasetPage.getDescription().getReleaseDate()
                .compareTo(ds2.getDetails().datasetPage.getDescription().getReleaseDate());
        datasets.sort(byReleaseDate);
    }

    private static List<DataPublication> getAllDatasets(Path source, ContentReader contentReader) throws ZebedeeException, IOException {
        List<TimeseriesDatasetFiles> datasetDownloads = getTimeseriesDatasets(source);

        List<DataPublication> dataPublications = new ArrayList<>();

        for (TimeseriesDatasetFiles datasetDownload : datasetDownloads) {
            System.out.println("------------------------------------------------------");
            System.out.println("CsdbPath() = " + datasetDownload.getCsdbPath());
            System.out.println("DatasetPath() = " + datasetDownload.getDatasetPath());

            Page page = contentReader.getContent(datasetDownload.getRootPath().toString());

            if (page instanceof TimeSeriesDataset == false) {
                System.out.println("This is not a timeseries dataset.");
                continue;
            }

            TimeSeriesDataset timeseriesDatasetPage = (TimeSeriesDataset) page;

            //System.out.println("Uri = " + timeseriesDatasetPage.getUri());
            //System.out.println("release date = " + timeseriesDatasetPage.getDescription().getReleaseDate());

            // create custom instance of data publication. We want to reference versioned datasets but specify the landing page
            // relative to the current version.

            String datasetUri = datasetDownload.getRootPath().toString();

            if (VersionedContentItem.isVersionedUri(datasetUri)) {

            }

            String landingPageUri = "";
            String fileUri = "";
            String parentFolderUri = "";
            DatasetLandingPage landingPage = new DatasetLandingPage();
            TimeSeriesDataset datasetPage = new TimeSeriesDataset();

            DataPublicationDetails dataPublicationDetails = new DataPublicationDetails(datasetUri, landingPageUri, fileUri, parentFolderUri, landingPage, datasetPage);
            DataPublication newPublication = new DataPublication(dataPublicationDetails);
            dataPublications.add(newPublication);
        }

        sortDatasetsByReleaseDate(dataPublications);

        return dataPublications;
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
