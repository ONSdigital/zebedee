package com.github.onsdigital.zebedee;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.configuration.CMSFeatureFlags;
import com.github.onsdigital.zebedee.service.NoOpRedirectService;
import com.github.onsdigital.zebedee.service.RedirectService;
import com.github.onsdigital.zebedee.service.RedirectServiceImpl;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ZebedeeConfigurationTest {

    private Path rootPath;
    
    @Before
    public void setUp() throws Exception {
        rootPath = Files.createTempDirectory(Random.id());
        System.setProperty("KEYRING_SECRET_KEY", createCollectionKeyStoreKey());
        System.setProperty("KEYRING_INIT_VECTOR", createCollectionKeyStoreIV());
        System.setProperty("JWT_VERIFIER_KEY_ID","abc");
        System.setProperty("JWT_VERIFIER_PUBLIC_KEY", createJWTVerificationPublicKey());
    }

    @After
    public void tearDown() throws Exception {
        System.clearProperty(CMSFeatureFlags.ENABLE_REDIRECT_API);
        CMSFeatureFlags.reset();
        FileUtils.deleteDirectory(rootPath.toFile());
    }

    @Test
    public void getRedirectService_shouldGetRedirectServiceWhenEnabled() throws IOException {
        // Given env flag is set to true
        System.setProperty(CMSFeatureFlags.ENABLE_REDIRECT_API, "true");
        CMSFeatureFlags.reset();

        ZebedeeConfiguration zebedeeConfig = new ZebedeeConfiguration(rootPath);

        // When RedirectService is requested
        RedirectService redirectService = zebedeeConfig.getRedirectService();

        // Then it is a full implementation
        assertEquals(RedirectServiceImpl.class, redirectService.getClass());
    }

    @Test
    public void getRedirectService_shouldGetNoOpRedirectServiceWhenDisabled() throws IOException {
        // Given env flag is set to false
        System.setProperty(CMSFeatureFlags.ENABLE_REDIRECT_API, "false");
        CMSFeatureFlags.reset();

        ZebedeeConfiguration zebedeeConfig = new ZebedeeConfiguration(rootPath);

        // When RedirectService is requested
        RedirectService redirectService = zebedeeConfig.getRedirectService();

        // Then it is a no op implementation
        assertEquals(NoOpRedirectService.class, redirectService.getClass());
    }

    @Test
    public void constructor_shouldThrowErrorIfRedirectURIInvalid() {
        // Given env flag is set to true
        System.setProperty(CMSFeatureFlags.ENABLE_REDIRECT_API, "true");
        CMSFeatureFlags.reset();
        // And the API URL flag is set to an invalid value
        System.setProperty("REDIRECT_API_URL", ".;,!^&*");

        // When the configuration is initialised
        RuntimeException ex = assertThrows(RuntimeException.class, () -> new ZebedeeConfiguration(rootPath));

        // Then a runtime exception should be thrown
        assertEquals("Illegal character in path at index 4: .;,!^&*", ex.getCause().getMessage());
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

    private static String createJWTVerificationPublicKey() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        PublicKey publicKey = kpg.generateKeyPair().getPublic();
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
}
