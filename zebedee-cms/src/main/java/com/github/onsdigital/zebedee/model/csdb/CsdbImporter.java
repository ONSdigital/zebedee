package com.github.onsdigital.zebedee.model.csdb;

import com.github.davidcarboni.cryptolite.KeyExchange;
import com.github.onsdigital.zebedee.util.EncryptionUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;

public class CsdbImporter {

    public static final String APPLICATION_KEY_ID = "csdb-import";


    public static void processNotification(
            PrivateKey privateCsdbImportKey,
            String csdbIdentifier,
            DylanClient dylan
    ) throws IOException {

        try (InputStream csdbData = getDylanData(privateCsdbImportKey, csdbIdentifier, dylan)) {

            // determine where to put it.
            //     zebedee.collections.getCollectionForCsdbId
            //     collection.getDataSetForCsdbId

            // add it into the collection encrypted.


            // if its approved - add a task to a queue to delete existing timeseries and regenerate
        }

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
