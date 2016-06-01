package com.github.onsdigital.zebedee.model.approval.task.timeseries;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.approval.tasks.timeseries.TimeSeriesCompressor;
import com.github.onsdigital.zebedee.model.approval.tasks.timeseries.TimeseriesCompressionResult;
import com.github.onsdigital.zebedee.model.approval.tasks.timeseries.ZipFileVerifier;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static junit.framework.TestCase.assertTrue;

public class ZipFileVerifierTest {

    private TimeSeriesCompressor timeSeriesCompressor = new TimeSeriesCompressor();
    private ZipFileVerifier zipFileVerifier = new ZipFileVerifier();

    public static void createTimeseriesFile(Path timeseriesRoot) throws IOException {
        String timeseriesId = Random.id();
        Path timeseriesDirectory = timeseriesRoot.resolve(timeseriesId);
        Files.createDirectories(timeseriesDirectory);
        TimeSeries timeSeries = new TimeSeries();
        String serialised = ContentUtil.serialise(timeSeries);
        FileUtils.write(timeseriesDirectory.resolve("data.json").toFile(), serialised);
    }

    public static void createEmptyFile(Path timeseriesRoot) throws IOException {
        String timeseriesId = Random.id();
        Path timeseriesDirectory = timeseriesRoot.resolve(timeseriesId);
        Files.createDirectories(timeseriesDirectory);
        FileUtils.write(timeseriesDirectory.resolve("data.json").toFile(), "");
    }

    // If the number of json files in the zip matches the original number of json files in the timeseries directory
    // and we can deserialise the json then its a success
    @Test
    public void shouldReturnEmptyListForSuccessfulVerification() throws IOException, ZebedeeException {

        // Given a directory with timeseries and a zip file of the timeseries
        Path tempDirectory = Files.createTempDirectory(Random.id());
        Path timeseriesRoot = Files.createDirectories(tempDirectory.resolve("timeseries"));

        ContentReader reader = new FileSystemContentReader(tempDirectory);
        ContentWriter writer = new ContentWriter(tempDirectory);

        createTimeseriesFile(timeseriesRoot);
        List<TimeseriesCompressionResult> zipFiles = timeSeriesCompressor.compressFiles(reader, writer, false);

        // when the zip file is verified.
        List<TimeseriesCompressionResult> failedVerifications = zipFileVerifier.verifyZipFiles(zipFiles, reader, reader, writer);
        assertTrue(failedVerifications.size() == 0);
    }

    // If there is an empty json file in the zip file then fail verification.
    @Test
    public void shouldReturnFailedVerificationsForEmptyJsonFile() throws IOException, ZebedeeException {

        Path tempDirectory = Files.createTempDirectory(Random.id());
        Path timeseriesRoot = Files.createDirectories(tempDirectory.resolve("timeseries"));

        ContentReader reader = new FileSystemContentReader(tempDirectory);
        ContentWriter writer = new ContentWriter(tempDirectory);

        createEmptyFile(timeseriesRoot);
        List<TimeseriesCompressionResult> zipFiles = timeSeriesCompressor.compressFiles(reader, writer, false);

        List<TimeseriesCompressionResult> failedVerifications = zipFileVerifier.verifyZipFiles(zipFiles, reader, reader, writer);
        assertTrue(failedVerifications.size() > 0);
    }

    // if there is an empty timeseries directory then the zip will be empty. Recognise there are no timeseries and do not fail verification.
    @Test
    public void shouldReturnSuccessfullyWhenTimeseriesDirectoryIsEmpty() throws IOException, ZebedeeException {

        // Given a directory with timeseries and a zip file of the timeseries
        Path tempDirectory = Files.createTempDirectory(Random.id());
        Files.createDirectories(tempDirectory.resolve("timeseries"));

        ContentReader reader = new FileSystemContentReader(tempDirectory);
        ContentWriter writer = new ContentWriter(tempDirectory);

        List<TimeseriesCompressionResult> zipFiles = timeSeriesCompressor.compressFiles(reader, writer, false);

        List<TimeseriesCompressionResult> failedVerifications = zipFileVerifier.verifyZipFiles(zipFiles, reader, reader, writer);
        assertTrue(failedVerifications.size() > 0);
    }

    // If a zip file has no files but there should be timeseries in there then fail verification
    @Test
    public void shouldReturnFailedVerificationsForEmptyZip() throws IOException, ZebedeeException {


        Path tempDirectory = Files.createTempDirectory(Random.id());
        Files.createDirectories(tempDirectory.resolve("timeseries"));

        ContentReader reader = new FileSystemContentReader(tempDirectory);
        ContentWriter writer = new ContentWriter(tempDirectory);

        List<TimeseriesCompressionResult> zipFiles = timeSeriesCompressor.compressFiles(reader, writer, false);
        zipFiles.get(0).numberOfFiles = 1;

        // when the zip file is verified.
        List<TimeseriesCompressionResult> failedVerifications = zipFileVerifier.verifyZipFiles(zipFiles, reader, reader, writer);
        assertTrue(failedVerifications.size() > 0);
    }
}
