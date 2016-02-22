package com.github.onsdigital.zebedee.model.csdb;

import com.github.davidcarboni.cryptolite.KeyExchange;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DownloadSection;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.model.*;
import com.github.onsdigital.zebedee.model.publishing.PublishNotification;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.util.EncryptionUtils;
import com.github.onsdigital.zebedee.util.Log;
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
    public static void processNotification(
            PrivateKey privateCsdbImportKey,
            String csdbIdentifier,
            DylanClient dylan,
            Collections collections,
            Map<String, SecretKey> keyCache
    ) throws IOException, ZebedeeException {
        executorService.submit(() -> processCsdb(privateCsdbImportKey, csdbIdentifier, dylan, collections, keyCache));
    }

    private static void processCsdb(PrivateKey privateCsdbImportKey, String csdbIdentifier, DylanClient dylan, Collections collections, Map<String, SecretKey> keyCache) {
        Log.print("Processing notification for CSDB %s", csdbIdentifier);

        Collection collection;
        try (InputStream csdbData = getDylanData(privateCsdbImportKey, csdbIdentifier, dylan)) {
            collection = addCsdbToCollectionWithCorrectDataset(csdbIdentifier, collections, keyCache, csdbData);

            if (collection != null) {

                Log.print("Collection %s found for CSDB %s.", collection.description.name, csdbIdentifier);

                if (collection.description.approvedStatus == true) {
                    preProcessCollection(collection);
                } else {
                    Log.print("The collection %s is not approved.", collection.description.name);
                }
            } else {
                Log.print("No collection found for CSDB file with ID %s", csdbIdentifier);
            }
        } catch (ZebedeeException | IOException e) {
            Log.print(e);
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
    public static void preProcessCollection(Collection collection) throws IOException, ZebedeeException {
        SecretKey collectionKey = Root.zebedee.keyringCache.schedulerCache.get(collection.description.id);
        CollectionReader collectionReader = new ZebedeeCollectionReader(collection, collectionKey);
        CollectionWriter collectionWriter = new ZebedeeCollectionWriter(collection, collectionKey);
        ContentReader publishedReader = new ContentReader(Root.zebedee.published.path);

        List<String> uriList;
        try {
            uriList = Collections.preprocessTimeseries(Root.zebedee, collection, collectionReader, collectionWriter, publishedReader);
        } catch (URISyntaxException e) {
            throw new BadRequestException("Brian could not process this collection");
        }

        new PublishNotification(collection, uriList).sendNotification(EventType.APPROVED);
    }

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
            SecretKey collectionKey = keyCache.get(collection.description.id);
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
                    Log.print(e);
                }
            }
        }
        return null;
    }

    private static boolean containsIgnoreCase(List<String> list, String toCompare){
        for (String item : list) {
            if (item.equalsIgnoreCase(toCompare));
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
}
