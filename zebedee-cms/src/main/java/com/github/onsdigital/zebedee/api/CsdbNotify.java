package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.cryptolite.Crypto;
import com.github.davidcarboni.cryptolite.KeyExchange;
import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.csdb.CsdbImporter;
import com.github.onsdigital.zebedee.model.csdb.DylanClient;
import com.github.onsdigital.zebedee.model.csdb.HttpDylanClient;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.PrivateKey;

@Api
public class CsdbNotify {

    // Hold a single instance of DylanClient.
    private static final DylanClient dylanClient = new HttpDylanClient(Configuration.getDylanUrl());

    /**
     * Notify zebedee that a new CSDB file is available
     *
     * @param request
     * @param response
     * @throws IOException
     * @throws ZebedeeException
     */
    @POST
    public void csdbNotify(HttpServletRequest request, HttpServletResponse response, String csdbId) throws IOException, ZebedeeException {

        System.out.println(String.format("\n\tReceived csdb file notification: filename='%s'.\n", csdbId));

        try {
            // get key from dylan
            String encryptedDylanKey = dylanClient.getEncryptedSecretKey(FilenameUtils.getBaseName(csdbId) + ".key");

            PrivateKey pk = Root.zebedee.applicationKeys.getPrivateKeyFromCache(CsdbImporter.APPLICATION_KEY_ID);

            // get the csdb data
            InputStream encryptedCsdbData = dylanClient.getEncryptedCsdbData(csdbId);
            dylanSays(encryptedCsdbData);

            // stream is consumed get it again.
            encryptedCsdbData = dylanClient.getEncryptedCsdbData(csdbId);

            // decrypt it using the private key
            SecretKey secretKey = new KeyExchange().decryptKey(encryptedDylanKey, pk);

            InputStream unencryptedDylan = new Crypto().decrypt(encryptedCsdbData, secretKey);
            dylanSays(unencryptedDylan);

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // TODO this needs to be wired into the collections publishing.
/*
        PrivateKey privateKey = Root.zebedee.applicationKeys.getPrivateKeyFromCache(CsdbImporter.APPLICATION_KEY_ID);

        CsdbImporter.processNotification(
                privateKey,
                csdbId,
                dylanClient,
                Root.zebedee.collections,
                Root.zebedee.keyringCache.schedulerCache);*/
    }

    private void dylanSays(InputStream in) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(in, writer);
        System.out.println(String.format("\nDylan says:\n%s", writer.toString()));
    }
}
