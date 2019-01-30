package com.github.onsdigital.zebedee.model.csdb;

import com.github.davidcarboni.cryptolite.KeyExchange;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DownloadSection;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ApprovalStatus;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter;
import com.github.onsdigital.zebedee.model.approval.ApproveTask;
import com.github.onsdigital.zebedee.model.approval.tasks.timeseries.TimeSeriesCompressionTask;
import com.github.onsdigital.zebedee.model.publishing.PublishNotification;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.util.EncryptionUtils;
import org.apache.commons.io.FilenameUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * Handles a notification when a new CSDB file is available to zebedee.
 * This class handles the retrieval of the CSDB data and saving it into the premade collection.
 */
public class CsdbImporter {

    // The key that is used to store the public / private keys
    public static final String APPLICATION_KEY_ID = "csdb-import";

    // Single threaded pool to process a single CSDB at a time.
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * Save the CSDB file into the collection that it should be inserted into.
     *
     * @param csdbIdentifier
     * @param collections
     * @param keyCache
     * @param csdbData
     * @return
     * @throws IOException
     * @throws BadRequestException
     * @throws UnauthorizedException
     * @throws NotFoundException
     */
    private static Collection addCsdbToCollectionWithCorrectDataset(
            String csdbIdentifier,
            Collections collections,
            Map<String, SecretKey> keyCache,
            InputStream csdbData
    ) throws IOException, BadRequestException, UnauthorizedException, NotFoundException {
        for (Collection collection : collections.list()) {
            SecretKey collectionKey = keyCache.get(collection.getDescription().getId());
            CollectionReader collectionReader = new ZebedeeCollectionReader(collection, collectionKey);
            Path csdbFileUri = getCsdbPathFromCollection(csdbIdentifier, collectionReader);
            if (csdbFileUri != null) {
                CollectionWriter collectionWriter = new ZebedeeCollectionWriter(collection, collectionKey);
                collectionWriter.getReviewed().write(csdbData, csdbFileUri.toString());
                return collection;
            }
        }
        return null; // if no collection is found.
    }

    /**
     * Given a CSDB file identifier and a collection, find the path the CSDB should be added to. If the CSDB file
     * does not belong to the collection then null is returned.
     *
     * @param csdbIdentifier
     * @param collectionReader
     * @return
     * @throws IOException
     */
    static Path getCsdbPathFromCollection(String csdbIdentifier, CollectionReader collectionReader) throws IOException {
        List<String> uris = collectionReader.getReviewed().listUris();

        // for each uri in the collection
        for (String uri : uris) {
            // deserialise only the uris that are datasets
            if (uri.contains("/datasets/")) {
                try {

                    if (FilenameUtils.getExtension(uri).equals("json")) {
                        try (Resource resource = collectionReader.getReviewed().getResource(uri)) {
                            Page page = ContentUtil.deserialiseContent(resource.getData());

                            if (page.getType().equals(PageType.timeseries_dataset)) {

                                String filename = csdbIdentifier + ".csdb";
                                Dataset datasetPage = (Dataset) page;

                                for (DownloadSection downloadSection : datasetPage.getDownloads()) {
                                    if ((downloadSection.getCdids() != null && containsIgnoreCase(downloadSection.getCdids(), csdbIdentifier))
                                            || (downloadSection.getFile() != null && downloadSection.getFile().equalsIgnoreCase(filename))) {
                                        return Paths.get(uri).getParent().resolve(filename);
                                    }
                                }
                            }
                        }
                    }
                } catch (ZebedeeException e) {
                    logError(e, "Error while getting CSDB path from collection")
                            .addParameter("CSDBIdentifier", csdbIdentifier).log();
                }
            }
        }
        return null;
    }

    private static boolean containsIgnoreCase(List<String> list, String toCompare) {
        for (String item : list) {
            if (item.equalsIgnoreCase(toCompare)) ;
            return true;
        }
        return false;
    }

    /**
     * Co-ordinate the key management to decrypt data provided by dylan.
     *
     * @param privateCsdbImportKey
     * @param csdbIdentifier
     * @param dylan
     * @return
     * @throws IOException
     */
    static InputStream getDylanData(PrivateKey privateCsdbImportKey, String csdbIdentifier, DylanClient dylan) throws IOException {
        // get key from dylan
        String dylanKey = dylan.getEncryptedSecretKey(csdbIdentifier);

        // decrypt it using the private key
        SecretKey secretKey = new KeyExchange().decryptKey(dylanKey, privateCsdbImportKey);

        // get the csdb data
        InputStream csdbData = dylan.getEncryptedCsdbData(csdbIdentifier);

        // decrypt it using the retrieved key.
        InputStream inputStream = EncryptionUtils.encryptionInputStream(csdbData, secretKey);

        return inputStream;
    }

    /**
     * When processing the notification, add a task to a single threaded executor to process the CSDB.
     * This ensures only one CSDB file is processed at a time.
     *
     * @param privateCsdbImportKey
     * @param csdbIdentifier
     * @param dylan
     * @param collections
     * @param keyCache
     * @throws IOException
     * @throws ZebedeeException
     */
    public void processNotification(
            PrivateKey privateCsdbImportKey,
            String csdbIdentifier,
            DylanClient dylan,
            Collections collections,
            Map<String, SecretKey> keyCache
    ) throws IOException, ZebedeeException {
        executorService.submit(() -> processCsdb(privateCsdbImportKey, csdbIdentifier, dylan, collections, keyCache));
    }

    private void processCsdb(PrivateKey privateCsdbImportKey, String csdbIdentifier, DylanClient dylan, Collections collections, Map<String, SecretKey> keyCache) {
        logInfo("Processing CSDB notification").addParameter("CSDBIdentifier", csdbIdentifier).log();

        Collection collection;
        try (InputStream csdbData = getDylanData(privateCsdbImportKey, csdbIdentifier, dylan)) {
            collection = addCsdbToCollectionWithCorrectDataset(csdbIdentifier, collections, keyCache, csdbData);

            if (collection != null) {

                logInfo("Found collection found for CSDB identifier")
                        .addParameter("collectionName", collection.getDescription().getName())
                        .addParameter("CSDBIdentifier", csdbIdentifier).log();

                if (collection.description.approvalStatus == ApprovalStatus.COMPLETE) {
                    preProcessCollection(collection);
                } else {
                    logInfo("Collection for CSDB identifier is not approved")
                            .addParameter("collectionName", collection.getDescription().getName())
                            .addParameter("CSDBIdentifier", csdbIdentifier).log();
                }
            } else {
                logInfo("No collection found for CSDB identifier")
                        .addParameter("CSDBIdentifier", csdbIdentifier).log();
            }
        } catch (ZebedeeException | IOException | URISyntaxException e) {
            logError(e, "Error while processing CSDB file notification")
                    .addParameter("CSDBIdentifier", csdbIdentifier).log();
        }
    }

    /**
     * Once a CSDB file has been inserted into a collection, the timeseries files must be regenerated.
     * Generate the files and then publish the updated uri list to babbage for cache notification.
     *
     * @param collection
     * @throws IOException
     * @throws ZebedeeException
     */
    public void preProcessCollection(Collection collection) throws IOException, ZebedeeException, URISyntaxException {
        SecretKey collectionKey = Root.zebedee.getKeyringCache().schedulerCache.get(collection.getDescription().getId());
        CollectionReader collectionReader = new ZebedeeCollectionReader(collection, collectionKey);
        CollectionWriter collectionWriter = new ZebedeeCollectionWriter(collection, collectionKey);
        ContentReader publishedReader = new FileSystemContentReader(Root.zebedee.getPublished().path);
        DataIndex dataIndex = Root.zebedee.getDataIndex();

        ApproveTask.generateTimeseries(collection, publishedReader, collectionReader, collectionWriter, dataIndex);
        PublishNotification publishNotification = ApproveTask.createPublishNotification(collectionReader, collection);
        compressZipFiles(collection, collectionReader, collectionWriter);

        // Send a notification to the website with the publish date for caching.
        publishNotification.sendNotification(EventType.APPROVED);

    }

    private void compressZipFiles(Collection collection, CollectionReader collectionReader, CollectionWriter collectionWriter) throws ZebedeeException, IOException {
        TimeSeriesCompressionTask timeSeriesCompressionTask = new TimeSeriesCompressionTask();
        timeSeriesCompressionTask.compressTimeseries(collection, collectionReader, collectionWriter);
    }
}
