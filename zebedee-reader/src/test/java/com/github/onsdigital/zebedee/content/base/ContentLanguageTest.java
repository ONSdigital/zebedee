package com.github.onsdigital.zebedee.content.base;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.junit.Test;

public class ContentLanguageTest {

    @Test
    public void testGetByIdEnglish() {
        assertEquals(Optional.of(ContentLanguage.ENGLISH), ContentLanguage.getById("en"));
    }

    @Test
    public void testGetByIdWelsh() {
        assertEquals(Optional.of(ContentLanguage.WELSH), ContentLanguage.getById("cy"));
    }

    @Test
    public void testGetByIdUnknown() {
        assertEquals(Optional.empty(), ContentLanguage.getById("sc"));
    }

    @Test
    public void testGetByIdEmpty() {
        assertEquals(Optional.empty(), ContentLanguage.getById(""));
    }

    @Test
    public void testGetByIdNull() {
        assertEquals(Optional.empty(), ContentLanguage.getById(null));
    }

    @Test
    public void testEnglishId() {
        assertEquals("en", ContentLanguage.ENGLISH.getId());
    }

    @Test
    public void testWelshId() {
        assertEquals("cy", ContentLanguage.WELSH.getId());
    }

    @Test
    public void testEnglishFileSuffix() {
        assertEquals("", ContentLanguage.ENGLISH.getFileSuffix());
    }

    @Test
    public void testWelshFileSuffix() {
        assertEquals("_cy", ContentLanguage.WELSH.getFileSuffix());
    }

    @Test
    public void testEnglishDataFilename() {
        assertEquals("data.json", ContentLanguage.ENGLISH.getDataFileName());
    }

    @Test
    public void testWelshDataFilename() {
        assertEquals("data_cy.json", ContentLanguage.WELSH.getDataFileName());
    }

    @Test
    public void testEnglishToString() {
        assertEquals("en", ContentLanguage.ENGLISH.toString());
    }

    @Test
    public void testWelshToString() {
        assertEquals("cy", ContentLanguage.WELSH.toString());
    }
}
