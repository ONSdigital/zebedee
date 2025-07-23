package com.github.onsdigital.zebedee;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.onsdigital.zebedee.configuration.CMSFeatureFlags;
import com.github.onsdigital.zebedee.service.NoOpRedirectService;
import com.github.onsdigital.zebedee.service.RedirectService;
import com.github.onsdigital.zebedee.service.RedirectServiceImpl;

public class ZebedeeConfigurationTest extends ZebedeeTestBaseFixture {

    Path expectedPath;
    
    @Before
    public void setUp() throws Exception {
        expectedPath = builder.parent;
    }

    @After
    public void tearDown() throws Exception {
        System.clearProperty("REDIRECT_API_URL");
    }

    @Test
    public void shouldGetRedirectServiceWhenEnabled() throws IOException {
        // Given env flag is set to true
        System.setProperty(CMSFeatureFlags.ENABLE_REDIRECT_API, "true");
        CMSFeatureFlags.reset();

        ZebedeeConfiguration zebedeeConfig = new ZebedeeConfiguration(expectedPath);

        // When RedirectService is requested
        RedirectService redirectService = zebedeeConfig.getRedirectService();

        // Then it is a full implementation
        assertEquals(RedirectServiceImpl.class, redirectService.getClass());
    }

    @Test
    public void shouldGetNoOpRedirectServiceWhenDisabled() throws IOException {
        // Given env flag is set to false
        System.setProperty(CMSFeatureFlags.ENABLE_REDIRECT_API, "false");
        CMSFeatureFlags.reset();

        ZebedeeConfiguration zebedeeConfig = new ZebedeeConfiguration(expectedPath);

        // When RedirectService is requested
        RedirectService redirectService = zebedeeConfig.getRedirectService();

        // Then it is a no op implementation
        assertEquals(NoOpRedirectService.class, redirectService.getClass());
    }

    @Test
    public void shouldThrowErrorIfRedirectURIInvalid() {
        // Given env flag is set to true
        System.setProperty(CMSFeatureFlags.ENABLE_REDIRECT_API, "true");
        CMSFeatureFlags.reset();
        // And the API URL flag is set to an invalid value
        System.setProperty("REDIRECT_API_URL", ".;,!^&*");

        // When the configuration is initialised
        RuntimeException ex = assertThrows(RuntimeException.class, () -> new ZebedeeConfiguration(expectedPath));

        // Then a runtime exception should be thrown
        assertEquals("Illegal character in path at index 4: .;,!^&*", ex.getCause().getMessage());
    }
}
