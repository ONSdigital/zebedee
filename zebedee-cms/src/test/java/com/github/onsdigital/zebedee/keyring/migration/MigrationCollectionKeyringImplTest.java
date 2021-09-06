package com.github.onsdigital.zebedee.keyring.migration;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.user.model.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.onsdigital.zebedee.keyring.migration.MigrationCollectionKeyringImpl.ADD_KEY_ERR;
import static com.github.onsdigital.zebedee.keyring.migration.MigrationCollectionKeyringImpl.GET_KEY_ERR;
import static com.github.onsdigital.zebedee.keyring.migration.MigrationCollectionKeyringImpl.KEY_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.migration.MigrationCollectionKeyringImpl.LIST_KEYS_ERR;
import static com.github.onsdigital.zebedee.keyring.migration.MigrationCollectionKeyringImpl.POPULATE_FROM_USER_ERR;
import static com.github.onsdigital.zebedee.keyring.migration.MigrationCollectionKeyringImpl.REMOVE_KEY_ERR;
import static com.github.onsdigital.zebedee.keyring.migration.MigrationCollectionKeyringImpl.ROLLBACK_FAILED_ERR;
import static com.github.onsdigital.zebedee.keyring.migration.MigrationCollectionKeyringImpl.UNLOCK_KEYRING_ERR;
import static com.github.onsdigital.zebedee.keyring.migration.MigrationCollectionKeyringImpl.WRAPPED_ERR_FMT;
import static java.text.MessageFormat.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class MigrationCollectionKeyringImplTest {

    @Mock
    protected User user;

    @Mock
    protected Collection collection;

    @Mock
    protected SecretKey secretKey;

    @Mock
    protected CollectionKeyring legacyCollectionKeyring;

    @Mock
    protected CollectionKeyring collectionKeyring;

    protected CollectionKeyring keyring;

    protected KeyringException keyringException;

    protected boolean enabled = true;
    protected boolean disabled = false;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.keyringException = new KeyringException("keyringException");
    }

    protected void assertWrappedException(KeyringException actual, String expectedMsg, boolean migrationEnabled) {
        String msg = format(WRAPPED_ERR_FMT, expectedMsg, migrationEnabled);
        assertThat(actual.getMessage(), equalTo(msg));
        assertThat(actual.getCause(), is(notNullValue()));
    }

    protected void assertException(KeyringException actual, String expectedMsg, boolean migrationEnabled) {
        String msg = format(WRAPPED_ERR_FMT, expectedMsg, migrationEnabled);
        assertThat(actual.getMessage(), equalTo(msg));
    }
}
