package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;

import static com.github.onsdigital.zebedee.keyring.KeyringException.formatErrorMesage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KeyringServiceImplTest {

    static final String COLLECTION_ID = "138"; // We are 138 \M/

    private KeyringService keyringService;

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

        when(collection.getDescription())
                .thenReturn(description);

        when(description.getId())
                .thenReturn(COLLECTION_ID);

        this.keyring = new Keyring();
        this.keyringService = new KeyringServiceImpl(store, keyring);
    }

    @Test(expected = KeyringException.class)
    public void add_collectionNull_shouldThrowException() throws Exception {
        try {
            keyringService.add(null, null);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo("keyring.add requires collection but was null"));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void add_collectionDescriptionNull_shouldThrowException() throws Exception {
        try {
            keyringService.add(mock(Collection.class), null);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo("keyring.add requires collection.description but was null"));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void add_collectionKeyNull_shouldThrowException() throws Exception {
        try {
            keyringService.add(collection, null);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo("keyring.add requires secretKey but was null"));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void add_keyringStoreWriteExecption_shouldThrowException() throws Exception {

        when(store.save(collection, secretKey))
                .thenThrow(new KeyringException("error updating collection keyring", COLLECTION_ID));

        try {
            keyringService.add(collection, secretKey);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(formatErrorMesage("error updating collection keyring", COLLECTION_ID)));
            assertTrue(keyring.isEmpty());

            verify(store, times(1)).save(collection, secretKey);
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void add_keyringStoreWriteUnsuccessful_shouldThrowException() throws Exception {
        when(store.save(collection, secretKey))
                .thenReturn(false);

        try {
            keyringService.add(collection, secretKey);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(formatErrorMesage("updating keyring was unsuccessful", COLLECTION_ID)));
            assertTrue(keyring.isEmpty());

            verify(store, times(1)).save(collection, secretKey);
            throw ex;
        }
    }

    @Test
    public void add_Success_shouldAddKeyToKeyring() throws Exception {
        when(store.save(collection, secretKey))
                .thenReturn(true);

        keyringService.add(collection, secretKey);

        assertFalse(keyring.isEmpty());
        assertThat(keyring.get(collection.getDescription().getId()), equalTo(secretKey));

        verify(store, times(1)).save(collection, secretKey);
    }
}
