package com.github.onsdigital.zebedee.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class DatasetWhitelistCheckerTest {
    @Test
    public void testIsWhitelistedWithValidFilename() {
        assertTrue(DatasetWhitelistChecker.isWhitelisted("upload-mm22.csv"));
    }

    @Test
    public void testIsWhitelistedWithEmptyFilename() {
        assertFalse(DatasetWhitelistChecker.isWhitelisted(""));
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
