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

    @Test
    public void testIsWhitelistedWithx09() {
        assertTrue(DatasetWhitelistChecker.isWhitelisted("x09jul2025.xlsx"));
    }

    @Test
    public void testIsWhitelistedWithx09WithFalseDate() {
        assertFalse(DatasetWhitelistChecker.isWhitelisted("x09jul2025AVX.xlsx"));
    }

    @Test
    public void testIsWhitelistedWithx09WithFalseDateWithUpload() {
        assertTrue(DatasetWhitelistChecker.isWhitelisted("upload-x09jul2025.xlsx"));
    }

    @Test
    public void testIsWhitelistedWithdataset1() {
        assertTrue(DatasetWhitelistChecker.isWhitelisted("dataset1.xlsx"));
    }

    @Test
    public void testIsWhitelistedWitha01() {
        assertTrue(DatasetWhitelistChecker.isWhitelisted("a01jul2025.xlsx"));
    }

    @Test
    public void testIsWhitelistedWithmm22withExtraChar() {
        assertFalse(DatasetWhitelistChecker.isWhitelisted("mm22ABC.csv"));
    }

    @Test
    public void testIsWhitelistedWithrtisa() {
        assertTrue(DatasetWhitelistChecker.isWhitelisted("rtisa.csv"));
    }

}
