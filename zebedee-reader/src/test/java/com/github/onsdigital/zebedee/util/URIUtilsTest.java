package com.github.onsdigital.zebedee.util;

import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static org.junit.Assert.*;

public class URIUtilsTest {

    @Test
    public void removeLastSegmentWhenEndingOnEmpty(){
        String result = URIUtils.removeLastSegment("//");
        assertEquals("/",result);
    }

    @Test
    public void removeLastSegmentWhenEndingOnSlash(){
        String result = URIUtils.removeLastSegment("/segment1/segment2/segment3/");
        assertEquals("/segment1/segment2",result);
    }

    @Test
    public void removeLastSegment(){
        String result = URIUtils.removeLastSegment("/segment1/segment2/segment3");
        assertEquals("/segment1/segment2",result);
    }

    @Test
    public void getQueryParameterFromURLShouldFailBecauseParameterNamesAreCaseSensitiveTest() throws UnsupportedEncodingException{
        String url = "/file?URI=" + "/Economy/inflation/articles/research/topic/health/december2023/december2023/";
        String result = URIUtils.getQueryParameterFromURL(url, "Uri");
        assertEquals(null, result);
    }

    @Test
    public void getQueryParameterFromURLTest() throws UnsupportedEncodingException{
        String url = "/file?URI=" + "/Economy/inflation/articles/research/topic/health/december2023/december2023/";
        String result = URIUtils.getQueryParameterFromURL(url, "URI");
        assertEquals("/Economy/inflation/articles/research/topic/health/december2023/december2023/", result);
    }

    @Test
    public void isLastSegmentFileExtensionYes() {
        String url = "/economy/inflation/articles/research/topic/health/december2023/december2023.json";
        boolean result = URIUtils.isLastSegmentFileExtension(url);
        assertTrue(result);
    }

    @Test
    public void isLastSegmentFileExtensionNo() {
        String url = "/economy/inflation/articles/research/topic/health/december2023/december2023/";
        boolean result = URIUtils.isLastSegmentFileExtension(url);
        assertFalse(result);
    }

    @Test
    public void isLastSegmentFileExtensionNo2() {
        String url = "/economy/inflation/articles/research/topic/health/december2023/december2023";
        boolean result = URIUtils.isLastSegmentFileExtension(url);
        assertFalse(result);
    }

    @Test
    public void isLastSegmentFileExtensionNo3() {
        boolean result = URIUtils.isLastSegmentFileExtension("/");
        assertFalse(result);
    }

    @Test
    public void getLastSegments() {
        String url = "/economy/inflation/articles/research/topic/health/december2023";
        String result = URIUtils.getLastSegment(url);
        assertEquals("december2023", result);
    }

    @Test
    public void getLastSegmentsOfEmptyReturnsNull() {
        String url = "";
        String result = URIUtils.getLastSegment(url);
        assertNull(result);
    }

    @Test
    public void getLastSegmentsOfOneSlashReturnsNull() {
        String url = "/";
        String result = URIUtils.getLastSegment(url);
        assertNull(result);
    }

    @Test
    public void getLastSegmentsFileName() {
        String url = "/economy/inflation/articles/research/topic/health/december2023/december2023.json";
        String result = URIUtils.getLastSegment(url);
        assertEquals("december2023.json", result);
    }

    @Test
    public void getLastSegmentsFileName2() {
        String url = "december2023.json";
        String result = URIUtils.getLastSegment(url);
        assertEquals("december2023.json", result);
    }

    @Test
    public void getNSegmentsAfterSegment() {
        String url = "/economy/inflation/articles/research/topic/health/december2023";
        String result = URIUtils.getNSegmentsAfterSegmentInput(url, "/articles", 2);
        assertEquals("/economy/inflation/articles/research/topic", result);
    }

    @Test
    public void getNSegmentsAfterSegment10th() {
        String url = "/economy/inflation/articles/research/topic/health/december2023";
        String result = URIUtils.getNSegmentsAfterSegmentInput(url, "/articles", 10);
        assertEquals("/economy/inflation/articles/research/topic/health/december2023", result);
    }

    @Test
    public void getNSegmentsAfterSegmentIfNotExistsReturnSameURL() {
        String url = "/economy/inflation/bulletins/research/topic/health/december2023";
        String result = URIUtils.getNSegmentsAfterSegmentInput(url, "/articles", 2);
        assertEquals(url, result);
    }

    @Test
    public void getNSegmentsNEquals2() {
        String url = "/economy/inflation/bulletins/research/topic/health/december2023";
        String result = URIUtils.getNSegments(url, 2);
        assertEquals("/economy/inflation", result);
    }

    @Test
    public void getNSegmentsNlongerThenAvailable() {
        String url = "/economy/inflation/bulletins/research/topic/health/december2023";
        String result = URIUtils.getNSegments(url, 9);
        assertEquals("/economy/inflation/bulletins/research/topic/health/december2023", result);
    }

    @Test
    public void getNSegmentsForPassNEquals0() {
        String url = "/economy/inflation/bulletins/research/topic/health/december2023";
        String result = URIUtils.getNSegments(url, 0);
        assertEquals("", result);
    }

    @Test
    public void getNSegmentsForSlashOnly() {
        String url = "/";
        String result = URIUtils.getNSegments(url, 2);
        assertEquals("/", result);
    }

    @Test
    public void getNSegmentsForNegative() {
        String url = "/economy/inflation/bulletins/research/topic/health/december2023";
        String result = URIUtils.getNSegments(url, -2);
        assertEquals("", result);
    }
}
