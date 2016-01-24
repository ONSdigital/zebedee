package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DownloadSection;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class DataPublication {
    private DataLink dataLink = new DataLinkBrian();
    private DataPublicationDetails details = null;
    private TimeSerieses serieses = null;
    private TimeSerieses results = new TimeSerieses();

    /**
     * Setup a a new Data publication
     *  @param publishedContentReader a reader for already published content
     * @param reviewedContentReader a reader for the content being approved
     * @param datasetPageUri the root Timeseries dataset page
     */
    public DataPublication(ContentReader publishedContentReader, ContentReader reviewedContentReader, String datasetPageUri) throws ZebedeeException, IOException {
        // Setup the publication by backtracking from the dataset
        details = new DataPublicationDetails(publishedContentReader, reviewedContentReader, datasetPageUri);
    }

    /**
     * Get details of files that act as inputs to this publication
     *
     * @return a DataPublicationDetails
     */
    public DataPublicationDetails getDetails() {
        return details;
    }

    /**
     * Process a specified collection
     *
     * @param publishedContentReader
     * @param reviewedContentReader
     * @param reviewedContentWriter
     * @throws IOException
     * @throws ZebedeeException
     */
    public void process(ContentReader publishedContentReader, ContentReader reviewedContentReader, ContentWriter reviewedContentWriter) throws IOException, ZebedeeException, URISyntaxException {

        // send the file for processing
        this.serieses = callDataLink(reviewedContentReader, details.fileUri);

        // Process each returned timeseries
        for(TimeSeries series: serieses) {
            // Build new timeseries
            DataProcessor processor = new DataProcessor();
            processor.processTimeseries(publishedContentReader, details, series);

            // Save files
            DataWriter writer = new DataWriter(reviewedContentWriter, reviewedContentReader, publishedContentReader);
            writer.versionAndSave(processor, details);

            // Retain the result to be added to any generated spreadsheet
            results.add(processor.timeSeries);
        }


        // Generate data files
        DataFileGenerator generator = new DataFileGenerator(reviewedContentWriter);
        List<DownloadSection> downloadSections = generator.generateDataDownloads(this.details, this.results);

        // Link downloadable files to the TimeSeriesDataset page
        downloadSections.add(newDownloadSection("csdb", details.fileUri));
        details.datasetPage.setDownloads(downloadSections);
        reviewedContentWriter.writeObject(details.datasetPage, details.datasetPage.getUri().toString() + "/data.json");
    }

    /**
     * Call Brian with the appropriate file
     *
     * @param collectionReader reader to pick the appropriate file
     * @throws IOException
     * @throws ZebedeeException
     */
    private TimeSerieses callDataLink(ContentReader collectionReader, String fileUri) throws IOException, ZebedeeException {
        TimeSerieses results = null;
        if (fileUri.toLowerCase().endsWith("csdb")) {
            results = dataLink.callCSDBProcessor(details.fileUri, collectionReader);
        } else {
            results = dataLink.callCSVProcessor(details.fileUri, collectionReader);
        }
        return results;
    }

    private DownloadSection newDownloadSection(String title, String file) {
        DownloadSection section = new DownloadSection();
        section.setTitle(title);
        section.setFile(file);
        return section;
    }

    public void setDataLink(DataLink dataLink) {
        this.dataLink = dataLink;
    }
}
