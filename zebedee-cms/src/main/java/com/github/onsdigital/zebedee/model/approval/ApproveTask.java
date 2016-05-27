package com.github.onsdigital.zebedee.model.approval;

import com.github.onsdigital.zebedee.data.DataPublisher;
import com.github.onsdigital.zebedee.data.importing.CsvTimeseriesUpdateImporter;
import com.github.onsdigital.zebedee.data.importing.TimeseriesUpdateCommand;
import com.github.onsdigital.zebedee.data.importing.TimeseriesUpdateImporter;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.Event;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.approval.tasks.*;
import com.github.onsdigital.zebedee.model.content.CompoundContentReader;
import com.github.onsdigital.zebedee.model.publishing.PublishNotification;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.service.BabbagePdfService;
import com.github.onsdigital.zebedee.util.ContentDetailUtil;
import com.github.onsdigital.zebedee.util.SlackNotification;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * Callable implementation for the approval process.
 */
public class ApproveTask implements Callable<Boolean> {

    private final Collection collection;
    private final Session session;
    private final CollectionReader collectionReader;
    private final CollectionWriter collectionWriter;
    private final ContentReader publishedReader;
    private final DataIndex dataIndex;

    public ApproveTask(
            Collection collection,
            Session session,
            CollectionReader collectionReader,
            CollectionWriter collectionWriter,
            ContentReader publishedReader,
            DataIndex dataIndex
    ) {
        this.collection = collection;
        this.session = session;
        this.collectionReader = collectionReader;
        this.collectionWriter = collectionWriter;
        this.publishedReader = publishedReader;
        this.dataIndex = dataIndex;
    }

    @Override
    public Boolean call() {

        try {

            List<ContentDetail> collectionContent = ContentDetailUtil.resolveDetails(collection.reviewed, collectionReader.getReviewed());

            populateReleasePage(collectionContent);
            List<String> uriList = generateTimeseries();
            compressZipFiles();
            generatePdfFiles(collectionContent);
            approveCollection();

            // Send a notification to the website with the publish date for caching.
            new PublishNotification(collection, uriList).sendNotification(EventType.APPROVED);

            return true;

        } catch (IOException | ZebedeeException | URISyntaxException e) {
            logError(e, "Exception approving collection").collectionName(collection).log();
            SlackNotification.alarm(String.format("Exception approving collection %s : %s", collection.description.name, e.getMessage()));
            return false;
        }
    }

    public void approveCollection() throws IOException {
        // set the approved state on the collection
        collection.description.approvedStatus = true;
        collection.description.AddEvent(new Event(new Date(), EventType.APPROVED, session.email));
        collection.save();
    }

    public void populateReleasePage(List<ContentDetail> collectionContent) throws IOException {
        // If the collection is associated with a release then populate the release page.
        ReleasePopulator.populateQuietly(collection, collectionReader, collectionWriter, collectionContent);
    }

    public void generatePdfFiles(List<ContentDetail> collectionContent) {
        CollectionPdfGenerator pdfGenerator = new CollectionPdfGenerator(new BabbagePdfService(session, collection));
        pdfGenerator.generatePdfsInCollection(collectionWriter, collectionContent);
    }

    public void compressZipFiles() throws ZebedeeException, IOException {
        logInfo("Compressing time series directories").collectionName(collection).log();
        List<TimeseriesCompressionResult> zipFiles = TimeSeriesCompressor.compressFiles(collectionReader.getReviewed(), collectionWriter.getReviewed(), collection.description.isEncrypted);

        logInfo("Verifying " + zipFiles.size() + " time series zip files").collectionName(collection).log();
        List<TimeseriesCompressionResult> failedZipFiles = ZipFileVerifier.verifyZipFiles(
                zipFiles,
                collectionReader.getReviewed(),
                collectionReader.getRoot(),
                collectionWriter.getRoot());

        for (TimeseriesCompressionResult failedZipFile : failedZipFiles) {
            String message = "Failed verification of time series zip file: " + failedZipFile.path;
            logInfo(message).collectionName(collection).log();
            SlackNotification.alarm(message + " in collection " + collection + ". Unlock and approve the collection again.");
        }
    }

    public List<String> generateTimeseries() throws IOException, ZebedeeException, URISyntaxException {

        // Import any time series update CSV file
        List<TimeseriesUpdateCommand> updateCommands = new ArrayList<>();
        if (collection.description.timeseriesImportFiles != null) {
            for (String importFile : collection.description.timeseriesImportFiles) {
                CompoundContentReader compoundContentReader = new CompoundContentReader(publishedReader);
                compoundContentReader.add(collectionReader.getReviewed());

                InputStream csvInput = collectionReader.getRoot().getResource(importFile).getData();

                // read the CSV and update the timeseries titles.
                TimeseriesUpdateImporter importer = new CsvTimeseriesUpdateImporter(csvInput);

                logInfo("Importing CSV file").addParameter("filename", importFile).log();
                updateCommands.addAll(importer.importData());
            }
        }

        // Generate time series if required.
        return new DataPublisher().preprocessCollection(
                publishedReader,
                collectionReader,
                collectionWriter.getReviewed(), true, dataIndex, updateCommands);
    }
}
