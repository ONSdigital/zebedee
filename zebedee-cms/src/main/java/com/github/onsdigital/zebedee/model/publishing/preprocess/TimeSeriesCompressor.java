package com.github.onsdigital.zebedee.model.publishing.preprocess;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.util.ZipUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

public class TimeSeriesCompressor {

    /**
     * Compress timeseries
     * <p>
     * Uses content
     *
     * @param contentReader
     * @param contentWriter
     * @throws BadRequestException
     * @throws IOException
     */
    public static void compressFiles(ContentReader contentReader, ContentWriter contentWriter, Collection collection) throws ZebedeeException, IOException {
        logInfo("Compressing time series directories").collectionName(collection).log();

        List<Path> timeSeriesDirectories = contentReader.listTimeSeriesDirectories();

        for (Path timeSeriesDirectory : timeSeriesDirectories) {
            logInfo("Compressing time series directory").addParameter("directory", timeSeriesDirectory.toString()).log();
            String saveUri = contentReader.getRootFolder().relativize(timeSeriesDirectory).toString() + "-to-publish.zip";

            if (!collection.description.isEncrypted) {
                try (OutputStream outputStream = contentWriter.getOutputStream(saveUri)) {
                    ZipUtils.zipFolder(timeSeriesDirectory.toFile(), outputStream,
                            url -> VersionedContentItem.isVersionedUri(url));
                }
            } else {
                ZipUtils.zipFolderWithEncryption(contentReader, contentWriter, timeSeriesDirectory.toFile().toString(), saveUri,
                        url -> VersionedContentItem.isVersionedUri(url));
            }
            //Log.print("Deleting directory after compression %s", timeSeriesDirectory);
            //FileUtils.deleteDirectory(timeSeriesDirectory.toFile());
        }
    }

    //    /**
//     * Zip up timeseries to be transferred by the train
//     *
//     * @param collection the collection being published
//     * @throws IOException
//     */
//    public static void compressTimeseries(Zebedee zebedee, Collection collection) throws IOException {
//        Log.print("Compressing time series directories...");
//        List<Path> timeSeriesDirectories = collection.reviewed.listTimeSeriesDirectories();
//        for (Path timeSeriesDirectory : timeSeriesDirectories) {
//
//            Log.print("Compressing time series directory %s", timeSeriesDirectory.toString());
//            if (collection.description.isEncrypted) {
//                ZipUtils.zipFolderWithEncryption(
//                        timeSeriesDirectory.toFile(),
//                        new File(timeSeriesDirectory.toString() + "-to-publish.zip"),
//                        zebedee.keyringCache.schedulerCache.get(collection.description.id),
//                        url -> VersionedContentItem.isVersionedUri(url));
//            } else {
//                ZipUtils.zipFolder(
//                        timeSeriesDirectory.toFile(),
//                        new File(timeSeriesDirectory.toString() + "-to-publish.zip"),
//                        url -> VersionedContentItem.isVersionedUri(url));
//            }
//
//            Log.print("Deleting directory after compression %s", timeSeriesDirectory);
//            FileUtils.deleteDirectory(timeSeriesDirectory.toFile());
//        }
//    }

}
