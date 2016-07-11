package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DownloadSection;
import com.github.onsdigital.zebedee.data.importing.TimeseriesUpdateCommand;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.reader.CompositeContentReader;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class DataPublication {
    public static final String DEFAULT_DATASET_ID = "data";
    public static final int MAX_SECONDS = 60;
    DataLink dataLink = new DataLinkBrian();
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
     * Get the dataset id from a file
     *
     * Filename should be of the form [datasetId].csdb or upload.[datasetId].csv
     *
     * @param uri the uri of the data upload
     * @return
     */
    static String getDatasetIdFromFile(String uri) {
        String filename = Paths.get(uri).getFileName().toString().trim().toLowerCase();
        if (filename.endsWith(".csdb")) {
            return filename.substring(0, filename.length() - ".csdb".length());
        } else if (filename.startsWith("upload-") && filename.endsWith(".csv")) {
            if (filename.equalsIgnoreCase("upload.csv")) {
                return DEFAULT_DATASET_ID;
            } else {
                return filename.substring("upload-".length(), filename.length() - ".csv".length());
            }
        }
        return DEFAULT_DATASET_ID;
    }

    /**
     * Get details of files that act as inputs to this publication
     *
     * @return a DataPublicationDetails
     */
    public DataPublicationDetails getDetails() {
        return details;
    }

    public boolean hasUpload() {
        return (details.fileUri != null && details.fileUri != "");
    }

    /**
     * Process a specified collection
     * @param publishedContentReader
     * @param reviewedContentReader
     * @param reviewedContentWriter
     * @param saveTimeSeries
     * @param dataIndex
     * @param updateCommands - a set of commands to update the timeseries metadata.
     * @throws IOException
     * @throws ZebedeeException
     * @throws URISyntaxException
     */
    public void process(
            ContentReader publishedContentReader,
            ContentReader reviewedContentReader,
            ContentWriter reviewedContentWriter,
            boolean saveTimeSeries,
            DataIndex dataIndex,
            List<TimeseriesUpdateCommand> updateCommands
    ) throws IOException, ZebedeeException, URISyntaxException {
        // Wait for dataIndex to complete progress
        dataIndex.pauseUntilComplete(MAX_SECONDS);

        // check this landingpage has a datasetId and generate if necessary
        checkLandingPageDatasetId(reviewedContentWriter);

        // send the file to brian to create timeseries.
        this.serieses = callDataLink(reviewedContentReader, details.fileUri);

        CompositeContentReader compositeContentReader = new CompositeContentReader(reviewedContentReader, publishedContentReader);

        // Process each timeseries returned from Brian
        for(TimeSeries series: serieses) {

            // see if there is an update command for this timeseries.
            Optional<TimeseriesUpdateCommand> command = updateCommands.stream()
                    .filter(updateCommand -> updateCommand.cdid.equalsIgnoreCase(series.getCdid()))
                    .findFirst();

            // Build new timeseries
            DataProcessor processor = new DataProcessor();
            processor.processTimeseries(compositeContentReader, details, series, dataIndex, command);

            // Save files
            if (saveTimeSeries) {
                DataWriter writer = new DataWriter(reviewedContentWriter, reviewedContentReader, publishedContentReader);
                writer.versionAndSave(processor, details);
            }

            // Retain the result to be added to any generated spreadsheet
            results.add(processor.timeSeries);
        }

        // Generate data files
        DataFileGenerator generator = new DataFileGenerator(reviewedContentWriter);
        List<DownloadSection> downloadSections = generator.generateDataDownloads(this.details, this.results);
        downloadSections.add(newDownloadSection("csdb", details.fileUri));

        // Write the dataset page
        writeDatasetPage(reviewedContentWriter, details, downloadSections);
    }


    /**
     * Process a specified collection
     *
     * @param publishedContentReader
     * @param reviewedContentReader
     * @param reviewedContentWriter
     *@param updateCommands @throws IOException
     * @throws ZebedeeException
     */
    public void process(ContentReader publishedContentReader, ContentReader reviewedContentReader, ContentWriter reviewedContentWriter, DataIndex dataIndex, List<TimeseriesUpdateCommand> updateCommands) throws IOException, ZebedeeException, URISyntaxException {
        process(publishedContentReader, reviewedContentReader, reviewedContentWriter, true, dataIndex, updateCommands);
    }

        /**
         * Check the landingpage datasetId and update if necessary
         *
         * @param contentWriter
         * @throws IOException
         * @throws BadRequestException
         */
    void checkLandingPageDatasetId(ContentWriter contentWriter) throws IOException, BadRequestException {

        String currentId = details.landingPage.getDescription().getDatasetId();

        // only update if we have no current datasetId
        if (currentId == null || currentId.trim().length() == 0) {

            details.landingPage.getDescription().setDatasetId(getDatasetIdFromFile(details.fileUri));
            contentWriter.writeObject(details.landingPage, details.landingPage.getUri().toString() + "/data.json");
        }
    }

    /**
     * Write the timeseries_dataset page with the required downloads
     *
     * @param contentWriter a contentwriter for the collection
     * @param details details that contain the original timeseries
     * @param downloads the downloads section that needs to be updated
     */
    private void writeDatasetPage(ContentWriter contentWriter, DataPublicationDetails details, List<DownloadSection> downloads) throws IOException, BadRequestException {
        details.datasetPage.setDownloads(downloads);
        contentWriter.writeObject(details.datasetPage, details.datasetPage.getUri().toString() + "/data.json");
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

    /**
     * Convenience method to generate a download section in one line
     *
     * @param title the title for the download
     * @param file the filename of the download
     * @return
     */
    private DownloadSection newDownloadSection(String title, String file) {
        DownloadSection section = new DownloadSection();
        String filename = Paths.get(file).getFileName().toString();

        section.setTitle(title);
        section.setFile(filename);
        return section;
    }

    public void setDataLink(DataLink dataLink) {
        this.dataLink = dataLink;
    }
}
