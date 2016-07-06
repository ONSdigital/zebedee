package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.TimeSeriesDataset;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Version;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DataPublicationDetails {
    public String datasetUri;
    public String landingPageUri;
    public String fileUri;
    public String parentFolderUri;

    public DatasetLandingPage landingPage;
    public TimeSeriesDataset datasetPage;

    /**
     * Initialise data publication details by backtracking from the TimeSeriesDataset
     *
     * assumes we are going with the /../../parent/datasets/landingpage/timeseriesdataset
     * database schema
     *
     * @param publishedReader
     * @param collectionReader
     * @param datasetPageUri
     * @throws ZebedeeException
     * @throws IOException
     */
    public DataPublicationDetails(ContentReader publishedReader, ContentReader collectionReader, String datasetPageUri) throws ZebedeeException, IOException {
        this.datasetUri = datasetPageUri;
        this.landingPageUri = Paths.get(this.datasetUri).getParent().toString();
        this.parentFolderUri = Paths.get(this.datasetUri).getParent().getParent().getParent().toString();

        this.datasetPage = (TimeSeriesDataset) collectionReader.getContent(this.datasetUri);

        try {
            this.landingPage = (DatasetLandingPage) collectionReader.getContent(this.landingPageUri);
        } catch (NotFoundException e) {
            this.landingPage = (DatasetLandingPage) publishedReader.getContent(this.landingPageUri);
        }

        this.fileUri = findFileUri(collectionReader, datasetPageUri);
    }

    public String getTimeseriesFolder() {
        return parentFolderUri + "/timeseries";
    }

    public Version getLastDatasetVersion() {
        if (datasetPage.getVersions() == null || datasetPage.getVersions().size() == 0) {
            return null;
        } else {
            return datasetPage.getVersions().get(datasetPage.getVersions().size() - 1);
        }
    }

    /**
     * Find the first viable data upload file in a folder
     *
     * @param collectionReader
     * @param datasetPageUri
     * @return
     */
    String findFileUri(ContentReader collectionReader, String datasetPageUri) throws BadRequestException {

        try (DirectoryStream<Path> ds = collectionReader.getDirectoryStream(datasetPageUri)) {
            for (Path p : ds) {
                if (isAnUploadFile(p)) {
                    return "/" + collectionReader.getRootFolder().relativize(p).toString();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Check if name adhere's to either of the file upload conventions
     *
     * [datasetid].csdb
     * upload.[datasetId].csv
     *
     * @param path
     * @return
     */
    boolean isAnUploadFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".csdb")) {
            return true;
        } else if (fileName.startsWith("upload-") && fileName.endsWith(".csv")) {
            return true;
        }
        return false;
    }
}
