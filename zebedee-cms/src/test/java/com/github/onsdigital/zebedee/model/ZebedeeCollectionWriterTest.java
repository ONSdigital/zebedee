package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.keyring.Keyring;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;
import java.io.IOException;

import static com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter.COLLECTION_KEY_NULL_ERR;
import static com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter.COLLECTION_NULL_ERR;
import static com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter.GET_USER_ERR;
import static com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter.KEYRING_NULL_ERR;
import static com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter.PERMISSIONS_CHECK_ERR;
import static com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter.PERMISSIONS_SERVICE_NULL_ERR;
import static com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter.PERMISSION_DENIED_ERR;
import static com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter.SESSION_NULL_ERR;
import static com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter.USERS_SERVICE_NULL_ERR;
import static com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter.USER_NULL_ERR;
import static com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter.ZEBEDEE_NULL_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZebedeeCollectionWriterTest {

    static final String EMAIL = "ellen.ripley@weyland-yutanicopr.com";

    @Mock
    private Zebedee zebedee;

    @Mock
    private PermissionsService permissionsService;

    @Mock
    private UsersService usersService;

    @Mock
    private Keyring keyring;

    @Mock
    private Collection collection;

    @Mock
    private CollectionDescription description;

    @Mock
    private User user;

    @Mock
    private Session session;

    @Mock
    private SecretKey key;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        setUpMocksForHappyPath();
    }

    private void setUpMocksForHappyPath() throws Exception {
        when(zebedee.getPermissionsService())
                .thenReturn(permissionsService);

        when(zebedee.getUsersService())
                .thenReturn(usersService);

        when(zebedee.getCollectionKeyring())
                .thenReturn(keyring);

        when(collection.getDescription())
                .thenReturn(description);

        when(permissionsService.canEdit(session, description))
                .thenReturn(true);

        when(usersService.getUserByEmail(EMAIL))
                .thenReturn(user);

        when(keyring.get(user, collection))
                .thenReturn(key);

        when(session.getEmail())
                .thenReturn(EMAIL);
    }

    private ZebedeeCollectionWriter newCollectionWriter(Zebedee z, Collection c, Session s) throws Exception {
        return new ZebedeeCollectionWriter(z, c, s);
    }

    @Test
    public void testNew_ZebedeeNull_shouldThrowException() throws Exception {
        IOException ex = assertThrows(IOException.class, () -> newCollectionWriter(null, null, null));

        assertThat(ex.getMessage(), equalTo(ZEBEDEE_NULL_ERR));
    }

    @Test
    public void testNew_permissionsServiceNull_shouldThrowException() throws Exception {
        when(zebedee.getPermissionsService())
                .thenReturn(null);

        IOException ex = assertThrows(IOException.class, () -> newCollectionWriter(zebedee, null, null));

        assertThat(ex.getMessage(), equalTo(PERMISSIONS_SERVICE_NULL_ERR));
    }

    @Test
    public void testNew_usersServiceNull_shouldThrowException() throws Exception {
        when(zebedee.getUsersService())
                .thenReturn(null);

        IOException ex = assertThrows(IOException.class, () -> newCollectionWriter(zebedee, null, null));

        assertThat(ex.getMessage(), equalTo(USERS_SERVICE_NULL_ERR));
    }

    @Test
    public void testNew_keyringNull_shouldThrowException() throws Exception {
        when(zebedee.getCollectionKeyring())
                .thenReturn(null);

        IOException ex = assertThrows(IOException.class, () -> newCollectionWriter(zebedee, null, null));

        assertThat(ex.getMessage(), equalTo(KEYRING_NULL_ERR));
    }

    @Test
    public void testNew_collectionNull_shouldThrowException() throws Exception {
        NotFoundException ex = assertThrows(NotFoundException.class, () -> newCollectionWriter(zebedee, null, null));

        assertThat(ex.getMessage(), equalTo(COLLECTION_NULL_ERR));
    }

    @Test
    public void testNew_sessionNull_shouldThrowException() throws Exception {
        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> newCollectionWriter(zebedee, collection, null));

        assertThat(ex.getMessage(), equalTo(SESSION_NULL_ERR));
    }

    @Test
    public void testNew_permissionDenied_shouldThrowException() throws Exception {
        when(permissionsService.canEdit(session, description))
                .thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> newCollectionWriter(zebedee, collection, session));

        assertThat(ex.getMessage(), equalTo(PERMISSION_DENIED_ERR));
        verify(permissionsService, times(1)).canEdit(session, description);
    }

    @Test
    public void testNew_getUserError_shouldThrowException() throws Exception {
        when(usersService.getUserByEmail(EMAIL))
                .thenThrow(IOException.class);

        IOException ex = assertThrows(IOException.class,
                () -> newCollectionWriter(zebedee, collection, session));

        assertThat(ex.getMessage(), equalTo(GET_USER_ERR));
        verify(permissionsService, times(1)).canEdit(session, description);
        verify(usersService, times(1)).getUserByEmail(EMAIL);
    }

    @Test
    public void testNew_getUserReturnsNull_shouldThrowException() throws Exception {
        when(usersService.getUserByEmail(EMAIL))
                .thenReturn(null);

        IOException ex = assertThrows(IOException.class,
                () -> newCollectionWriter(zebedee, collection, session));

        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
        verify(permissionsService, times(1)).canEdit(session, description);
        verify(usersService, times(1)).getUserByEmail(EMAIL);
    }

    @Test
    public void testNew_canEditReturnsError_shouldThrowException() throws Exception {
        when(permissionsService.canEdit(session, description))
                .thenThrow(IOException.class);

        IOException ex = assertThrows(IOException.class,
                () -> newCollectionWriter(zebedee, collection, session));

        assertThat(ex.getMessage(), equalTo(PERMISSIONS_CHECK_ERR));
        verify(permissionsService, times(1)).canEdit(session, description);
    }

    @Test
    public void testNew_getKeyThrowsException_shouldThrowException() throws Exception {
        when(keyring.get(user, collection))
                .thenThrow(KeyringException.class);

        IOException ex = assertThrows(IOException.class,
                () -> newCollectionWriter(zebedee, collection, session));

        verify(permissionsService, times(1)).canEdit(session, description);
        verify(usersService, times(1)).getUserByEmail(EMAIL);
        verify(keyring, times(1)).get(user, collection);
    }

    @Test
    public void testNew_getKeyReturnsNull_shouldThrowException() throws Exception {
        when(keyring.get(user, collection))
                .thenReturn(null);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> newCollectionWriter(zebedee, collection, session));

        assertThat(ex.getMessage(), equalTo(COLLECTION_KEY_NULL_ERR));
        verify(permissionsService, times(1)).canEdit(session, description);
        verify(usersService, times(1)).getUserByEmail(EMAIL);
        verify(keyring, times(1)).get(user, collection);
    }
}
