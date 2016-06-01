package com.github.onsdigital.zebedee.model.approval.tasks.timeseries;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.util.SlackNotification;

import java.io.IOException;
import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * This class handles the process of compressing and verifying timeseries, including retry attempts.
 */
public class TimeSeriesCompressionTask {

    protected TimeSeriesCompressor timeSeriesCompressor;
    protected ZipFileVerifier zipFileVerifier;

    /**
     * Initialise a new instance of TimeSeriesCompressionTask
     *
     * @param timeSeriesCompressor
     * @param zipFileVerifier
     */
    public TimeSeriesCompressionTask(TimeSeriesCompressor timeSeriesCompressor, ZipFileVerifier zipFileVerifier) {
        this.timeSeriesCompressor = timeSeriesCompressor;
        this.zipFileVerifier = zipFileVerifier;
    }

    public TimeSeriesCompressionTask() {
        this.timeSeriesCompressor = new TimeSeriesCompressor();
        this.zipFileVerifier = new ZipFileVerifier();
    }

    /**
     * Compresses timeseries directories and verifies that the zip files are not corrupt.
     *
     * @param collection
     * @param collectionReader
     * @param collectionWriter
     * @throws ZebedeeException
     * @throws IOException
     */
    public void compressTimeseries(Collection collection, CollectionReader collectionReader, CollectionWriter collectionWriter) throws ZebedeeException, IOException {
        logInfo("Compressing time series directories").collectionName(collection).log();
        List<TimeseriesCompressionResult> zipFiles = timeSeriesCompressor.compressFiles(collectionReader.getReviewed(), collectionWriter.getReviewed(), collection.description.isEncrypted);

        logInfo("Verifying " + zipFiles.size() + " time series zip files").collectionName(collection).log();
        List<TimeseriesCompressionResult> failedZipFiles = zipFileVerifier.verifyZipFiles(
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
}
