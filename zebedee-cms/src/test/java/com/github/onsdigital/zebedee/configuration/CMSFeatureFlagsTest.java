package com.github.onsdigital.zebedee.configuration;

import static com.github.onsdigital.zebedee.configuration.CMSFeatureFlags.cmsFeatureFlags;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;


public class CMSFeatureFlagsTest {

    @After
    public void tearDown() {
        System.clearProperty(CMSFeatureFlags.ENABLE_REDIRECT_API);
    }

    @Test
    public void shouldGetDefaultIfNotSet() {
        // Given a set of feature flags
        System.clearProperty(CMSFeatureFlags.ENABLE_REDIRECT_API);
        CMSFeatureFlags.reset();
        CMSFeatureFlags cmsFeatureFlags = cmsFeatureFlags();

        // When redirect feature flag is requested
        boolean isRedirectAPIEnabled = cmsFeatureFlags.isRedirectAPIEnabled();

        // Then the default of false is returned
        assertFalse(isRedirectAPIEnabled);
    }

    @Test
    public void shouldGetRedirectFlagIfSet() {
        // Given an env var set to true
        System.setProperty(CMSFeatureFlags.ENABLE_REDIRECT_API, "true");
        CMSFeatureFlags.reset();
        CMSFeatureFlags cmsFeatureFlags = cmsFeatureFlags();

        // When redirect feature flag is requested
        boolean isRedirectAPIEnabled = cmsFeatureFlags.isRedirectAPIEnabled();

        // Then the default of true is returned
        assertTrue(isRedirectAPIEnabled);
    }
}
