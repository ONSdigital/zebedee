package com.github.onsdigital.zebedee.reader.api.bean;

import com.github.onsdigital.zebedee.search.indexing.Document;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class PublishedIndexResponseTest  {

    static final String URI1 = "/uri/1";
    static final String URI2 = "/uri/2";

    @Test
    public void testAddDocument_null() {
        PublishedIndexResponse publishedIndexResponse = new PublishedIndexResponse();
        publishedIndexResponse.addDocuments(null);
        assertEquals(publishedIndexResponse.getCount(),0);
        assertNotNull(publishedIndexResponse.getItems());
        assertEquals(publishedIndexResponse.getItems().size(),0);
    }

    @Test
    public void testAddDocument_noDocuments() {
        PublishedIndexResponse publishedIndexResponse = new PublishedIndexResponse();
        publishedIndexResponse.addDocuments(listOfDocuments());
        assertEquals(publishedIndexResponse.getCount(),0);
        assertNotNull(publishedIndexResponse.getItems());
        assertEquals(publishedIndexResponse.getItems().size(),0);
    }

    @Test
    public void testAddDocument_oneDocument() {
        PublishedIndexResponse publishedIndexResponse = new PublishedIndexResponse();
        publishedIndexResponse.addDocuments(listOfDocuments(URI1));
        assertEquals(publishedIndexResponse.getCount(),1);
        assertNotNull(publishedIndexResponse.getItems());
        assertEquals(publishedIndexResponse.getItems().size(),1);
        assertNotNull(publishedIndexResponse.getItems().get(0));
        assertEquals(publishedIndexResponse.getItems().get(0).getUri(),URI1);
    }

    @Test
    public void testAddDocument_twoDocuments() {
        PublishedIndexResponse publishedIndexResponse = new PublishedIndexResponse();
        publishedIndexResponse.addDocuments(listOfDocuments(URI1,URI2));
        assertEquals(publishedIndexResponse.getCount(),2);
        assertNotNull(publishedIndexResponse.getItems());
        assertEquals(publishedIndexResponse.getItems().size(),2);
        assertNotNull(publishedIndexResponse.getItems().get(0));
        assertEquals(publishedIndexResponse.getItems().get(0).getUri(),URI1);
        assertNotNull(publishedIndexResponse.getItems().get(1));
        assertEquals(publishedIndexResponse.getItems().get(1).getUri(),URI2);
    }

    @Test
    public void testAddDocument_multipleCalls() {
        PublishedIndexResponse publishedIndexResponse = new PublishedIndexResponse();
        publishedIndexResponse.addDocuments(listOfDocuments(URI1));
        assertEquals(publishedIndexResponse.getCount(),1);
        assertNotNull(publishedIndexResponse.getItems());
        assertEquals(publishedIndexResponse.getItems().size(),1);
        assertNotNull(publishedIndexResponse.getItems().get(0));
        assertEquals(publishedIndexResponse.getItems().get(0).getUri(),URI1);
        
        publishedIndexResponse.addDocuments(listOfDocuments(URI2));
        assertEquals(publishedIndexResponse.getCount(),2);
        assertNotNull(publishedIndexResponse.getItems());
        assertEquals(publishedIndexResponse.getItems().size(),2);
        assertNotNull(publishedIndexResponse.getItems().get(0));
        assertEquals(publishedIndexResponse.getItems().get(0).getUri(),URI1);
        assertNotNull(publishedIndexResponse.getItems().get(1));
        assertEquals(publishedIndexResponse.getItems().get(1).getUri(),URI2);
    }

    private List<Document> listOfDocuments(String... uris) {
        return Arrays.stream(uris)
                .map(u -> new Document(u,null))
                .collect(Collectors.toList());
    }
}
