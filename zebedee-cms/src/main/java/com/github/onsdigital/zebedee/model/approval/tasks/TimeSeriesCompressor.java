package com.github.onsdigital.zebedee.model.approval.tasks;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.util.ZipUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

public class TimeSeriesCompressor {

    /**
     * Find each time series directory in a collection and create a zip file for it.
     * <p>
     * Uses content
     *
     * @param contentReader
     * @param contentWriter
     * @throws BadRequestException
     * @throws IOException
     */
    public static List<TimeseriesCompressionResult> compressFiles(ContentReader contentReader, ContentWriter contentWriter, boolean isEncrypted) throws ZebedeeException, IOException {

        List<Path> timeSeriesDirectories = contentReader.listTimeSeriesDirectories();
        List<TimeseriesCompressionResult> results = new ArrayList<>();

        for (Path timeSeriesDirectory : timeSeriesDirectories) {

            String saveUri = contentReader.getRootFolder().relativize(timeSeriesDirectory).toString() + "-to-publish.zip";
            int filesAdded = compressFile(contentReader, contentWriter, isEncrypted, timeSeriesDirectory, saveUri);
            results.add(new TimeseriesCompressionResult(saveUri, filesAdded));

            //Log.print("Deleting directory after compression %s", timeSeriesDirectory);
            //FileUtils.deleteDirectory(timeSeriesDirectory.toFile());
        }

        return results;
    }

    public static int compressFile(ContentReader contentReader, ContentWriter contentWriter, boolean isEncrypted, Path timeSeriesDirectory, String saveUri) throws IOException, ZebedeeException {
        logInfo("Compressing time series directory").addParameter("directory", timeSeriesDirectory.toString()).log();
        if (!isEncrypted) {
            try (OutputStream outputStream = contentWriter.getOutputStream(saveUri)) {
                return ZipUtils.zipFolder(timeSeriesDirectory.toFile(), outputStream,
                        url -> VersionedContentItem.isVersionedUri(url));
            }
        } else {
            return ZipUtils.zipFolderWithEncryption(contentReader, contentWriter, timeSeriesDirectory.toFile().toString(), saveUri,
                    url -> VersionedContentItem.isVersionedUri(url));
        }
    }
}
