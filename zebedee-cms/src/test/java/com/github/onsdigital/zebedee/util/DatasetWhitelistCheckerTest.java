package com.github.onsdigital.zebedee.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;

@RunWith(MockitoJUnitRunner.class)
public class DatasetWhitelistCheckerTest {

    @Mock
    private CollectionReader collectionReader;

    @Mock
    private ContentReader reviewedContentReader;

    @InjectMocks
    private DatasetWhitelistChecker checker;
    
    @Before
    public void setUp() {
        when(collectionReader.getReviewed()).thenReturn(reviewedContentReader);
    }

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
    public void testIsWhitelistedWithppistatistics() {
        assertTrue(DatasetWhitelistChecker.isWhitelisted("ppistatistics.xlsx"));
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

    @Test
    public void testIsURIWhitelisted_WithWhitelistedURI() {
        List<String> uris = Arrays.asList("/path/to/upload-mm22.csv");
        when(reviewedContentReader.listUris()).thenReturn(uris);

        assertTrue("Expected URI to be whitelisted", DatasetWhitelistChecker.isURIWhitelisted(collectionReader));
    }

    @Test
    public void testIsURIWhitelisted_WithNonWhitelistedURI() {
        List<String> uris = Arrays.asList("/visualisations/dvc3069/Occupation%2520graphics%2520for%2520data%2520vis.xlsx");
        when(reviewedContentReader.listUris()).thenReturn(uris);

        assertFalse("Expected URI to not be whitelisted", DatasetWhitelistChecker.isURIWhitelisted(collectionReader));
    }
}
