package com.github.onsdigital.zebedee.data;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.data.processing.DataPublication;
import com.github.onsdigital.zebedee.data.processing.DataPublicationFinder;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionContentReader;
import com.github.onsdigital.zebedee.model.CollectionContentWriter;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.util.Log;
import com.github.onsdigital.zebedee.util.ZipUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

public class DataPublisherReloaded {
    public boolean doNotCompress = false;
    public DataPublisherReloaded(boolean doNotCompress) {
        this.doNotCompress = doNotCompress;
    }
    public DataPublisherReloaded() {
        this(false);
    }

    /**
     * Run the preprocess routine that processes csdb uploads with the option of skipping timeseries filesaves
     *
     * @param publishedContentReader reader for the master content
     * @param reviewedContentReader reader for this publications collection content
     * @param collectionContentWriter reader for this publications collection content
     * @param collection the collection being processed
     * @param saveTimeSeries the option to skip saving the individual timeseries
     * @throws IOException
     * @throws ZebedeeException
     * @throws URISyntaxException
     */
    public List<String> preprocessCollection(ContentReader publishedContentReader, ContentReader reviewedContentReader, ContentWriter collectionContentWriter, Collection collection, boolean saveTimeSeries, DataIndex dataIndex) throws IOException, ZebedeeException, URISyntaxException {

        // Find all files that need data preprocessing
        List<DataPublication> dataPublications = new DataPublicationFinder().findPublications(publishedContentReader, reviewedContentReader, collection);

        // For each file in this collection
        for(DataPublication dataPublication: dataPublications) {
            // If a file upload exists
            if (dataPublication.hasUpload())
                dataPublication.process(publishedContentReader, reviewedContentReader, collectionContentWriter, saveTimeSeries, dataIndex);
        }

        // Get the list of uris in reviewed
        List<String> uris = reviewedContentReader.listUris();

        // Run compression
        if (!doNotCompress)
            compressFiles(reviewedContentReader, collectionContentWriter, collection);

        return uris;
    }

    /**
     * Run the preprocess routine that processes csdb uploads
     *
     * @param publishedContentReader reader for the master content
     * @param reviewedContentReader reader for this publications collection content
     * @param collectionContentWriter reader for this publications collection content
     * @param collection the collection being processed
     * @throws IOException
     * @throws ZebedeeException
     * @throws URISyntaxException
     */
    public List<String> preprocessCollection(ContentReader publishedContentReader, CollectionContentReader reviewedContentReader, CollectionContentWriter collectionContentWriter, Collection collection, DataIndex dataIndex) throws IOException, ZebedeeException, URISyntaxException
    {
        return preprocessCollection(publishedContentReader, reviewedContentReader, collectionContentWriter, collection, true, dataIndex);
    }

    /**
     * Compress timeseries
     *
     * Uses content
     *
     * @param contentReader
     * @param contentWriter
     * @throws BadRequestException
     * @throws IOException
     */
    private void compressFiles(ContentReader contentReader, ContentWriter contentWriter, Collection collection) throws ZebedeeException, IOException {
        Log.print("Compressing time series directories...");

        List<Path> timeSeriesDirectories = contentReader.listTimeSeriesDirectories();

        for (Path timeSeriesDirectory : timeSeriesDirectories) {
            Log.print("Compressing time series directory %s", timeSeriesDirectory.toString());
            String saveUri = contentReader.getRootFolder().relativize(timeSeriesDirectory).toString() + "-to-publish.zip";

            if (!collection.description.isEncrypted) {
                try (OutputStream outputStream = contentWriter.getOutputStream(saveUri)) {
                    ZipUtils.zipFolder(timeSeriesDirectory.toFile(), outputStream);
                }
            } else {
                ZipUtils.zipFolderWithEncryption(contentReader, contentWriter, timeSeriesDirectory.toFile().toString(), saveUri);
            }
            Log.print("Deleting directory after compression %s", timeSeriesDirectory);
            FileUtils.deleteDirectory(timeSeriesDirectory.toFile());
        }
    }

}
