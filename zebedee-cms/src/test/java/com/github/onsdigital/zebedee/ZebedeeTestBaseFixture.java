package com.github.onsdigital.zebedee;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.MockitoAnnotations;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * {@link Deprecated} Please do not use this any more.
 */
@Deprecated
public abstract class ZebedeeTestBaseFixture {

    protected Zebedee zebedee;
    protected Builder builder;

    @BeforeClass
    public static void setUpKeyringEnvVars() throws Exception {
        System.setProperty("KEYRING_SECRET_KEY", createCollectionKeyStoreKey());
        System.setProperty("KEYRING_INIT_VECTOR", createCollectionKeyStoreIV());
    }

    @AfterClass
    public static void tearDownKeyringEnvVars() {
        System.clearProperty("KEYRING_SECRET_KEY");
        System.clearProperty("KEYRING_INIT_VECTOR");
    }

    @Before
    public void init() throws Exception {
        MockitoAnnotations.openMocks(this);

        builder = new Builder();
        zebedee = builder.getZebedee();

        setUp();
    }

    private static String createCollectionKeyStoreKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        SecretKey secretKey = keyGen.generateKey();
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    private static String createCollectionKeyStoreIV() {
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        return Base64.getEncoder().encodeToString(new IvParameterSpec(iv).getIV());
    }


    public abstract void setUp() throws Exception;

    @After
    public void tearDown() throws Exception {
        builder.delete();
    }

    @AfterClass
    public static void cleanUp() {
        TestUtils.clearReaderConfig();
    }
}
