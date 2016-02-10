package com.github.onsdigital.zebedee.model.csdb;

import com.github.davidcarboni.cryptolite.Keys;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;

import static org.junit.Assert.assertEquals;

public class CsdbImporterTest {

    @Test
    public void shouldGetCsdbDataFromDylan() throws IOException {

        // Given a dummy Dylan client and a new key pair
        KeyPair keyPair = Keys.newKeyPair();
        DummyDylanClient dylanClient = new DummyDylanClient(keyPair.getPublic());

        // When we call the getDylanData method.
        InputStream inputStream = CsdbImporter.getDylanData(keyPair.getPrivate(), "csdbId", dylanClient);

        String result = IOUtils.toString(inputStream);
        inputStream.close();

        assertEquals(dylanClient.testData, result);
    }
}
