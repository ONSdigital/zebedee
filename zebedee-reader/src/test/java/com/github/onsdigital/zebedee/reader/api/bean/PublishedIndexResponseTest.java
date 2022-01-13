package com.github.onsdigital.zebedee.reader.api.bean;

import com.github.onsdigital.zebedee.search.indexing.Document;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class PublishedIndexResponseTest {

    static final String URI1 = "/uri/1";
    static final String URI2 = "/uri/2";
    static final int TOTAL_COUNT = 20;
    static final String TOTAL_COUNT_KEY = "total_count";

    @Test
    public void testSerialisation() {
        PublishedIndexResponse publishedIndexResponse = new PublishedIndexResponse();
        publishedIndexResponse.setTotalCount(TOTAL_COUNT);

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        String jsonString = gson.toJson(publishedIndexResponse);
        assertNotNull(jsonString);
        assertTrue(jsonString.contains("\""+TOTAL_COUNT_KEY+"\": "+TOTAL_COUNT));
    }

    @Test
    public void testAddDocument_null() {
        PublishedIndexResponse publishedIndexResponse = new PublishedIndexResponse();
        publishedIndexResponse.addDocuments(null);
        assertEquals(0, publishedIndexResponse.getCount());
        assertNotNull(publishedIndexResponse.getItems());
        assertEquals(0, publishedIndexResponse.getItems().size());
    }

    @Test
    public void testAddDocument_noDocuments() {
        PublishedIndexResponse publishedIndexResponse = new PublishedIndexResponse();
        publishedIndexResponse.addDocuments(listOfDocuments());
        assertEquals(0, publishedIndexResponse.getCount());
        assertNotNull(publishedIndexResponse.getItems());
        assertEquals(0, publishedIndexResponse.getItems().size());
    }

    @Test
    public void testAddDocument_oneDocument() {
        PublishedIndexResponse publishedIndexResponse = new PublishedIndexResponse();
        publishedIndexResponse.addDocuments(listOfDocuments(URI1));
        assertEquals(1, publishedIndexResponse.getCount());
        assertNotNull(publishedIndexResponse.getItems());
        assertEquals(1, publishedIndexResponse.getItems().size());
        assertNotNull(publishedIndexResponse.getItems().get(0));
        assertEquals(URI1, publishedIndexResponse.getItems().get(0).getUri());
    }

    @Test
    public void testAddDocument_twoDocuments() {
        PublishedIndexResponse publishedIndexResponse = new PublishedIndexResponse();
        publishedIndexResponse.addDocuments(listOfDocuments(URI1, URI2));
        assertEquals(2, publishedIndexResponse.getCount());
        assertNotNull(publishedIndexResponse.getItems());
        assertEquals(2, publishedIndexResponse.getItems().size());
        assertNotNull(publishedIndexResponse.getItems().get(0));
        assertEquals(URI1, publishedIndexResponse.getItems().get(0).getUri());
        assertNotNull(publishedIndexResponse.getItems().get(1));
        assertEquals(URI2, publishedIndexResponse.getItems().get(1).getUri());
    }

    @Test
    public void testAddDocument_multipleCalls() {
        PublishedIndexResponse publishedIndexResponse = new PublishedIndexResponse();
        publishedIndexResponse.addDocuments(listOfDocuments(URI1));
        assertEquals(1, publishedIndexResponse.getCount());
        assertNotNull(publishedIndexResponse.getItems());
        assertEquals(1, publishedIndexResponse.getItems().size());
        assertNotNull(publishedIndexResponse.getItems().get(0));
        assertEquals(URI1, publishedIndexResponse.getItems().get(0).getUri());

        publishedIndexResponse.addDocuments(listOfDocuments(URI2));
        assertEquals(2, publishedIndexResponse.getCount());
        assertNotNull(publishedIndexResponse.getItems());
        assertEquals(2, publishedIndexResponse.getItems().size());
        assertNotNull(publishedIndexResponse.getItems().get(0));
        assertEquals(URI1, publishedIndexResponse.getItems().get(0).getUri());
        assertNotNull(publishedIndexResponse.getItems().get(1));
        assertEquals(URI2, publishedIndexResponse.getItems().get(1).getUri());
    }

    private List<Document> listOfDocuments(String... uris) {
        return Arrays.stream(uris)
                .map(u -> new Document(u, null))
                .collect(Collectors.toList());
    }
}
