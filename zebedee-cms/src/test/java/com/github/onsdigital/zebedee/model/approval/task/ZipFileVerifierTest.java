package com.github.onsdigital.zebedee.model.approval.task;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.approval.tasks.TimeSeriesCompressor;
import com.github.onsdigital.zebedee.model.approval.tasks.TimeseriesCompressionResult;
import com.github.onsdigital.zebedee.model.approval.tasks.ZipFileVerifier;
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

    @Test
    public void shouldReturnEmptyListForSuccessfulVerification() throws IOException, ZebedeeException {

        // Given a directory with timeseries and a zip file of the timeseries
        Path tempDirectory = Files.createTempDirectory(Random.id());
        Path timeseriesRoot = Files.createDirectories(tempDirectory.resolve("timeseries"));

        ContentReader reader = new FileSystemContentReader(tempDirectory);
        ContentWriter writer = new ContentWriter(tempDirectory);

        createTimeseriesFile(timeseriesRoot);
        List<TimeseriesCompressionResult> zipFiles = TimeSeriesCompressor.compressFiles(reader, writer, false);

        // when the zip file is verified.
        List<TimeseriesCompressionResult> failedVerifications = ZipFileVerifier.verifyZipFiles(zipFiles, reader, reader, writer);
        assertTrue(failedVerifications.size() == 0);
    }

    @Test
    public void shouldReturnFailedVerificationsForEmptyJsonFile() throws IOException, ZebedeeException {

        // Given a directory with timeseries and a zip file of the timeseries
        Path tempDirectory = Files.createTempDirectory(Random.id());
        Path timeseriesRoot = Files.createDirectories(tempDirectory.resolve("timeseries"));

        ContentReader reader = new FileSystemContentReader(tempDirectory);
        ContentWriter writer = new ContentWriter(tempDirectory);

        createEmptyFile(timeseriesRoot);
        List<TimeseriesCompressionResult> zipFiles = TimeSeriesCompressor.compressFiles(reader, writer, false);

        // when the zip file is verified.
        List<TimeseriesCompressionResult> failedVerifications = ZipFileVerifier.verifyZipFiles(zipFiles, reader, reader, writer);
        assertTrue(failedVerifications.size() > 0);
    }

    @Test
    public void shouldReturnFailedVerificationsForEmptyZip() throws IOException, ZebedeeException {

        // Given a directory with timeseries and a zip file of the timeseries
        Path tempDirectory = Files.createTempDirectory(Random.id());
        Files.createDirectories(tempDirectory.resolve("timeseries"));

        ContentReader reader = new FileSystemContentReader(tempDirectory);
        ContentWriter writer = new ContentWriter(tempDirectory);

        List<TimeseriesCompressionResult> zipFiles = TimeSeriesCompressor.compressFiles(reader, writer, false);

        // when the zip file is verified.
        List<TimeseriesCompressionResult> failedVerifications = ZipFileVerifier.verifyZipFiles(zipFiles, reader, reader, writer);
        assertTrue(failedVerifications.size() > 0);
    }
}
