package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.cryptolite.KeyExchange;
import com.github.onsdigital.zebedee.util.EncryptionUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;

public class CsdbImporter {

    public static final String APPLICATION_KEY_ID = "csdb-import";

    public static void processNotification(PrivateKey privateCsdbImportKey, String csdbIdentifier) throws IOException {

        // todo get key from dylan
        String dylanKey = "go get it from dylan";

        // decrypt it using the private key
        SecretKey secretKey = new KeyExchange().decryptKey(dylanKey, privateCsdbImportKey);

        // todo get the csdb data
        InputStream csdbData = null;

        // decrypt it using the retrieved key.
        InputStream inputStream = EncryptionUtils.encryptionInputStream(csdbData, secretKey);

        // determine where to put it.
        //     zebedee.collections.getCollectionForCsdbId
        //     collection.getDataSetForCsdbId

        // add it into the collection encrypted.


        // if its approved - add a task to a queue to delete existing timeseries and regenerate
    }
}
