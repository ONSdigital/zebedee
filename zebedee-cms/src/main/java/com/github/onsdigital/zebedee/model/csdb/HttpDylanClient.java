package com.github.onsdigital.zebedee.model.csdb;

import com.github.davidcarboni.httpino.Endpoint;
import com.github.davidcarboni.httpino.Host;
import com.github.davidcarboni.httpino.Http;
import com.github.davidcarboni.httpino.Response;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client to send requests to Dylan
 */
public class HttpDylanClient implements DylanClient {

    private final Host dylanHost;

    /**
     * Create a new instance of DylanClient for the given url.
     *
     * @param dylanUrl - The url of the dylan host.
     */
    public HttpDylanClient(String dylanUrl) {
        dylanHost = new Host(dylanUrl);
    }

    /**
     * Get the encrypted secret key from Dylan. The key will have been encrypted using the public key
     * provided by Zebedee - so must be decrypted using the private key held by zebedee.
     *
     * @return
     * @throws IOException
     */
    @Override
    public String getEncryptedSecretKey(String keyName) throws IOException {
        try (Http http = new Http()) {
            Endpoint endpoint = new Endpoint(dylanHost, "key").setParameter("name", keyName);
            Response<String> response = http.getJson(endpoint, String.class);
            return response.body;
        }
    }

    /**
     * Get the encrypted CSDB data from Dylan. The data will be encrypted using the secret key provided from dylan.
     * The getEncryptedSecretKey() method should be used to get the key to unlock this data.
     *
     * @param csdbIdentifier
     * @return
     * @throws IOException
     */
    @Override
    public InputStream getEncryptedCsdbData(String csdbIdentifier) throws IOException {
        try (Http http = new Http()) {
            Response<Path> response = http.getFile(new Endpoint(dylanHost, "file").setParameter("name", csdbIdentifier));
            Path path = response.body;
            return Files.newInputStream(path);
        }
    }
}
