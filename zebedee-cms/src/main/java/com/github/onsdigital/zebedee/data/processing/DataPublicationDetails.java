package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.TimeSeriesDataset;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;
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
     * @param publishedContentReader
     * @param reviewedContentReader
     * @param datasetPageUri
     * @throws ZebedeeException
     * @throws IOException
     */
    public DataPublicationDetails(ContentReader publishedContentReader, ContentReader reviewedContentReader, String datasetPageUri) throws ZebedeeException, IOException {
        this.datasetUri = datasetPageUri;
        this.landingPageUri = Paths.get(this.datasetUri).getParent().toString();
        this.parentFolderUri = Paths.get(this.datasetUri).getParent().getParent().getParent().toString();

        this.datasetPage = (TimeSeriesDataset) reviewedContentReader.getContent(this.datasetUri);

        try {
            this.landingPage = (DatasetLandingPage) reviewedContentReader.getContent(this.landingPageUri);
        } catch (NotFoundException e) {
            this.landingPage = (DatasetLandingPage) publishedContentReader.getContent(this.landingPageUri);
        }
    }

    public String getTimeseriesFolder() {
        return parentFolderUri + "/timeseries";
    }

    public String getDatasetCorrectionsNotice() {
            if (datasetPage.getVersions() == null || datasetPage.getVersions().size() == 0) {
                return "";
            } else {
                return datasetPage.getVersions().get(datasetPage.getVersions().size() - 1).getCorrectionNotice();
            }
    }
}
