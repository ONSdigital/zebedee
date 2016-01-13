package com.github.onsdigital.zebedee.util;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class JsonUtilsTest {
    @Test
    public void isValidJsonShouldReturnFalseWhenNotCompleteJson() {
        String json = "{ 'key'='value', "; // no closing curly brace to close the json object
        InputStream inputStream = new ByteArrayInputStream(json.getBytes());
        assertFalse(JsonUtils.isValidJson(inputStream));
    }

    @Test
    public void isValidJsonShouldReturnTrueForValidJson() {
        String json = "{ 'key'='value' } "; // no closing curly brace to close the json object
        InputStream inputStream = new ByteArrayInputStream(json.getBytes());
        assertTrue(JsonUtils.isValidJson(inputStream));
    }
}
