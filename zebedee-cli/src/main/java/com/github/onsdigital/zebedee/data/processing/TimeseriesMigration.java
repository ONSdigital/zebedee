package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.TimeSeriesDataset;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TimeseriesMigration {

    public static void main(String[] args) throws ZebedeeException, IOException, InterruptedException {
        migrateTimeseries(Paths.get("/Users/carlhembrough/dev/zebedee/zebedee/master"), Paths.get("/Users/carlhembrough/dev/zebedee/zebedee/master"));
    }

    public static void migrateTimeseries(String[] args) throws InterruptedException, ZebedeeException, IOException {
        // args[1] - source data directory
        // args[2] - destination directory to save the updated timeseries (can be a collection or master)

        Path source = Paths.get(args[1]);
        Path destination = Paths.get(args[2]);

        migrateTimeseries(source, destination);
    }

    private static void migrateTimeseries(Path source, Path destination) throws ZebedeeException, IOException, InterruptedException {

        ContentReader contentReader = new FileSystemContentReader(source); // read dataset / timeseries content from master
        ContentWriter contentWriter = new ContentWriter(destination); // write output content to collection

//        DataIndex dataIndex = DataIndexBuilder.buildDataIndex(contentReader); // build the dataindex of existing timeseries to determine location of output

        // find all CSDB files on the site and determine the dataset they belong to.
        List<TimeseriesDatasetFiles> datasetDownloads = getTimeseriesDatasets(source);

        List<TimeSeriesDataset> datasets = new ArrayList<>();

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

            datasets.add(timeseriesDatasetPage);
        }

        Comparator<TimeSeriesDataset> byReleaseDate = (ds1, ds2) -> ds1.getDescription().getReleaseDate()
                .compareTo(ds2.getDescription().getReleaseDate());
        datasets.sort(byReleaseDate);

        for (TimeSeriesDataset dataset : datasets) {
            System.out.println("Uri = " + dataset.getUri());
            System.out.println("release date = " + dataset.getDescription().getReleaseDate());
        }


        // get current location of the timeseries via the data index

        // create the new timeseries-data json under the directory of the dataset id

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
