package com.github.onsdigital.zebedee.model.csdb;

import com.github.davidcarboni.cryptolite.KeyExchange;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DownloadSection;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.*;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.util.EncryptionUtils;
import com.github.onsdigital.zebedee.util.Log;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.List;
import java.util.Map;

public class CsdbImporter {

    public static final String APPLICATION_KEY_ID = "csdb-import";

    public static void processNotification(
            PrivateKey privateCsdbImportKey,
            String csdbIdentifier,
            DylanClient dylan,
            Collections collections,
            Map<String, SecretKey> keyCache
    ) throws IOException, NotFoundException, BadRequestException, UnauthorizedException {

        try (InputStream csdbData = getDylanData(privateCsdbImportKey, csdbIdentifier, dylan)) {
            processCollections(csdbIdentifier, collections, keyCache, csdbData);
        }
    }

    private static void processCollections(String csdbIdentifier, Collections collections, Map<String, SecretKey> keyCache, InputStream csdbData) throws IOException, BadRequestException, UnauthorizedException, NotFoundException {
        for (Collection collection : collections.list()) {
            ProcessCollection(csdbIdentifier, keyCache, csdbData, collection);
        }
    }

    private static void ProcessCollection(String csdbIdentifier, Map<String, SecretKey> keyCache, InputStream csdbData, Collection collection) throws BadRequestException, IOException, UnauthorizedException, NotFoundException {
        SecretKey collectionKey = keyCache.get(collection.description.id);
        CollectionReader collectionReader = new ZebedeeCollectionReader(collection, collectionKey);

        Path csdbFileUri = findCsdbUri(csdbIdentifier, collectionReader);

        if (csdbFileUri != null) {

            CollectionWriter collectionWriter = new ZebedeeCollectionWriter(collection, collectionKey);

            collectionWriter.getReviewed().write(csdbData, csdbFileUri.toString());

            // todo if its approved - add a task to a queue to generate
            if (collection.description.approvedStatus == true) {

            } else {
                Log.print("The collection %s is not approved.", collection.description.name);
            }
        }
    }

    /**
     * For a given collection return the CSDB URI if the dataset is found.
     *
     * @param csdbIdentifier
     * @param collectionReader
     * @return
     * @throws IOException
     */
    private static Path findCsdbUri(String csdbIdentifier, CollectionReader collectionReader) throws IOException {
        List<String> uris = collectionReader.getReviewed().listUris();

        // for each uri in the collection
        for (String uri : uris) {

            // deserialise only the uris that are datasets
            if (uri.contains("/datasets/")) {
                try {
                    Page page = collectionReader.getReviewed().getContent(uri);

                    // if the page is a landing page then check the CSDB ID and put the csdb in the right place
                    if (page.getType().equals(PageType.timeseries_dataset)) {

                        String filename = csdbIdentifier + ".csdb";
                        Dataset datasetPage = (Dataset) page;

                        for (DownloadSection downloadSection : datasetPage.getDownloads()) {
                            if (downloadSection.getFile().equals(filename)) {
                                // work out what the URI of the CSDB file should be from the URI of the dataset page it belongs to.
                                return Paths.get(datasetPage.getUri().toString()).resolve(filename);
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
        String dylanKey = dylan.getEncryptedSecretKey();

        // decrypt it using the private key
        SecretKey secretKey = new KeyExchange().decryptKey(dylanKey, privateCsdbImportKey);

        // get the csdb data
        InputStream csdbData = dylan.getEncryptedCsdbData(csdbIdentifier);

        // decrypt it using the retrieved key.
        InputStream inputStream = EncryptionUtils.encryptionInputStream(csdbData, secretKey);

        return inputStream;
    }
}
