package com.github.onsdigital.zebedee.util;

import static com.github.onsdigital.zebedee.configuration.Configuration.getDatasetWhitelist;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import com.github.onsdigital.zebedee.configuration.Configuration;

@RunWith(MockitoJUnitRunner.class)
public class DatasetWhitelistCheckerTest {
    private MockedStatic<Configuration> mockConfiguration;

    // @Before
    // public void setUp() {
    //     mockConfiguration = mockStatic(Configuration.class);
    //     String whiteLst = "drsi,mm23,mm22,ppi,dataset1,pusf,a01,x09,cla01,pn2,mgdp,diop,ios1,mret,mq10";
    //     when(Configuration.getDatasetWhitelist()).thenReturn(whiteLst);
    // }

    @Test
    public void testIsWhitelistedWithValidFilename() {
        assertTrue(DatasetWhitelistChecker.isWhitelisted("upload-mm22.csv"));
    }

    @Test
    public void testIsWhitelistedWithInvalidFilename() {
        assertFalse(DatasetWhitelistChecker.isWhitelisted("upload-unknown.csv"));
    }

    @Test
    public void testIsWhitelistedWithoutUploadPrefix() {
        assertTrue(DatasetWhitelistChecker.isWhitelisted("mm22.csv"));
    }

    @Test
    public void testIsWhitelistedWithoutExtension() {
        assertTrue(DatasetWhitelistChecker.isWhitelisted("upload-mm22"));
    }

    @Test
    public void testIsWhitelistedWithDifferentExtension() {
        assertTrue(DatasetWhitelistChecker.isWhitelisted("upload-mm22.xlsx"));
    }
}
