package com.github.onsdigital.zebedee.model.encryption;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.json.Keyring;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Path;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class ApplicationKeysTest {

    private static final String APPLICATION_NAME = "example-application";
    private ApplicationKeys applicationKeys;
    private Path path;

    @Before
    public void setUp() throws Exception {
        path = Files.createTempDir().toPath().resolve("application-keys");
        applicationKeys = new ApplicationKeys(path);
    }

    @Test
    public void shouldGenerateNewKey() throws IOException {

        // Given an instance of application keys

        // When a new key is generated for an application
        SecretKey secretKey = applicationKeys.generateNewKey(APPLICATION_NAME);

        // Then the key returned is not null
        assertNotNull(secretKey);

        // The key is available from the application keys instance, and is cached.
        assertTrue(applicationKeys.isPublicKeyCached(APPLICATION_NAME));
        assertTrue(applicationKeys.isPrivateKeyCached(APPLICATION_NAME));
        assertTrue(applicationKeys.containsKey(APPLICATION_NAME));
        assertNotNull(applicationKeys.getPublicKey(APPLICATION_NAME));
        assertNotNull(applicationKeys.getPrivateKeyFromCache(APPLICATION_NAME));
    }

    @Test
    public void shouldPopulateCacheFromUserKeyring() throws IOException {
        // Given an existing instance of application keys

        // And a keyring populated with an application key.
        SecretKey secretKey = applicationKeys.generateNewKey(APPLICATION_NAME);
        String password = Random.password(8);
        Keyring keyring = Keyring.generate(password);
        keyring.put(APPLICATION_NAME, secretKey);

        // When the populateCacheFromUserKeyring method is called on a new instance (so its not already cached)
        ApplicationKeys newInstance = new ApplicationKeys(path);
        newInstance.populateCacheFromUserKeyring(keyring);

        // The new instance contains the key provided by the keyring.
        assertNotNull(newInstance.getPrivateKeyFromCache(APPLICATION_NAME));
    }
}
