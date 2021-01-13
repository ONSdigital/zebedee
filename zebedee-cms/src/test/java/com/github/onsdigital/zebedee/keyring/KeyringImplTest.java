package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

public class KeyringImplTest {

    private Keyring keyring;

    @Mock
    private KeyringStore store;

    @Mock
    private SecretKey secretKey;

    @Mock
    private Collection collection;

    @Mock
    private CollectionDescription description;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.keyring = new KeyringImpl(store, secretKey);
    }

    @Test(expected = KeyringException.class)
    public void add_collectionNull_shouldThrowException() throws Exception {
        try {
            keyring.add(null, null);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo("keyring.add requires collection but was null"));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void add_collectionDescriptionNull_shouldThrowException() throws Exception {
        try {
            keyring.add(collection, null);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo("keyring.add requires collection.description but was null"));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void add_collectionKeyNull_shouldThrowException() throws Exception {
        when(collection.getDescription())
                .thenReturn(description);

        try {
            keyring.add(collection, null);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo("keyring.add requires secretKey but was null"));
            throw ex;
        }
    }
}
