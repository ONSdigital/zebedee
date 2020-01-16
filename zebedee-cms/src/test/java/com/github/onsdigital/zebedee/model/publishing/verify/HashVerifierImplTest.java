package com.github.onsdigital.zebedee.model.publishing.verify;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.publishing.client.PublishingClient;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class HashVerifierImplTest {

    @Mock
    private PublishingClient publishingClient;

    @Mock
    private Collection collection;

    @Mock
    private CollectionDescription description;

    @Mock
    private CollectionReader reader;

    private HashVerifier hashVerifier;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        hashVerifier = new HashVerifierImpl(publishingClient);
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyTransactionContent_collectionIsNull_ExceptionThrown() throws Exception {
        try {
            hashVerifier.verifyTransactionContent(null, null);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("collection required but was null"));
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyTransactionContent_collectionReaderIsNull_ExceptionThrown() throws Exception {
        when(collection.getDescription())
                .thenReturn(description);

        try {
            hashVerifier.verifyTransactionContent(collection, null);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("collection reader required but was null"));
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyTransactionContent_collectionDescriptionIsNull_ExceptionThrown() throws Exception {
        when(collection.getDescription())
                .thenReturn(null);

        try {
            hashVerifier.verifyTransactionContent(collection, reader);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("collection.description required but was null"));
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyTransactionContent_collectionDescriptionTransactionsIdsNull_ExceptionThrown() throws Exception {
        when(collection.getDescription())
                .thenReturn(description);

        when(description.getPublishTransactionIds())
                .thenReturn(null);

        try {
            hashVerifier.verifyTransactionContent(collection, reader);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("description.publishingTransactionIds required but was null"));
            throw ex;
        }
    }
}
