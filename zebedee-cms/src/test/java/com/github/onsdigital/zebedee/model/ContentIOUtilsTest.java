package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.data.framework.DataPagesGenerator;
import com.github.onsdigital.zebedee.data.framework.DataPagesSet;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class ContentIOUtilsTest {

    @Rule
    public TemporaryFolder rootDir = new TemporaryFolder();

    private Path sourcePath;
    private Path targetPath;

    private DataPagesSet dataPagesSet;

    /**
     * Setup generates directory structure and test content
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {

        // Create necessary directory structure
        rootDir.create();
        Path rootPath = Paths.get(rootDir.getRoot().getPath());
        sourcePath = Files.createDirectory(rootPath.resolve("source"));
        targetPath = Files.createDirectory(rootPath.resolve("target"));

        // I'm using DataPagesGenerator to speed up generation but this could use any pages
        DataPagesGenerator generator = new DataPagesGenerator();
        dataPagesSet = generator.generateDataPagesSet("dataprocessor", "published", 2015, 2, "");
        writePage(sourcePath, dataPagesSet.datasetLandingPage, dataPagesSet.datasetLandingPage.getUri().toString());
        writePage(sourcePath, dataPagesSet.timeSeriesDataset, dataPagesSet.timeSeriesDataset.getUri().toString());

        for (TimeSeries timeSeries : dataPagesSet.timeSeriesList) {
            writePage(sourcePath, timeSeries, timeSeries.getUri().toString());
        }
    }

    @Test
    public void copy_withoutDirectorySpecified_shouldCopyAll() throws IOException, ZebedeeException {
        // Given
        // a reader pointing to some content and a writer pointing to an empty directory
        ContentWriter writer = new ContentWriter(targetPath);
        ContentReader reader = new FileSystemContentReader(sourcePath);
        String landingPage = dataPagesSet.datasetLandingPage.getUri().toString() + "/data.json";
        String datasetPage = dataPagesSet.timeSeriesDataset.getUri().toString() + "/data.json";
        String timeSeriesPage = dataPagesSet.timeSeriesList.get(0).getUri().toString() + "/data.json";

        // When
        // we run the copy
        ContentIOUtils.copy(reader, writer);

        // Then
        // we expect all content to have been copied
        assertTrue(Files.exists(uriResolve(targetPath, landingPage)));
        assertTrue(Files.exists(uriResolve(targetPath, datasetPage)));
        assertTrue(Files.exists(uriResolve(targetPath, timeSeriesPage)));
    }

    private Path uriResolve(Path root, String uri) {
        if (uri.startsWith("/")) {
            return root.resolve(uri.substring(1, uri.length()));
        } else {
            return root.resolve(uri);
        }
    }

    /**
     * Write a page object to the specified content path.
     *
     * @param contentPath the content root path to write to
     * @param page        any zebedee page
     * @param uri         the page uri
     * @throws IOException
     * @throws BadRequestException
     */
    private void writePage(Path contentPath, Page page, String uri) throws IOException, BadRequestException {
        String path = uri;
        if (path.startsWith("/"))
            path = path.substring(1);
        ContentWriter writer = new ContentWriter(contentPath);

        writer.writeObject(page, path + "/data.json");
    }
}