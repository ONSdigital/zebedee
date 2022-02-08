package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.cryptolite.Keys;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.ZebedeeTestBaseFixture;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.io.IOException;

import static com.github.onsdigital.zebedee.model.ZebedeeCollectionReader.COLLECTION_KEY_NULL_ERR;
import static com.github.onsdigital.zebedee.model.ZebedeeCollectionReader.COLLECTION_NULL_ERR;
import static com.github.onsdigital.zebedee.model.ZebedeeCollectionReader.GET_USER_ERR;
import static com.github.onsdigital.zebedee.model.ZebedeeCollectionReader.KEYRING_NULL_ERR;
import static com.github.onsdigital.zebedee.model.ZebedeeCollectionReader.PERMISSIONS_CHECK_ERR;
import static com.github.onsdigital.zebedee.model.ZebedeeCollectionReader.PERMISSIONS_SERVICE_NULL_ERR;
import static com.github.onsdigital.zebedee.model.ZebedeeCollectionReader.PERMISSION_DENIED_ERR;
import static com.github.onsdigital.zebedee.model.ZebedeeCollectionReader.SESSION_NULL_ERR;
import static com.github.onsdigital.zebedee.model.ZebedeeCollectionReader.USERS_SERVICE_NULL_ERR;
import static com.github.onsdigital.zebedee.model.ZebedeeCollectionReader.ZEBEDEE_NULL_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZebedeeCollectionReaderTest extends ZebedeeTestBaseFixture {

    static final String COLLECTION_ID = "1234";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private Zebedee zebedeeMock;

    @Mock
    private PermissionsService permissionsService;

    @Mock
    private CollectionKeyring keyring;

    @Mock
    private Collection collection;

    @Mock
    private CollectionDescription description;

    @Mock
    private SecretKey key;

    ZebedeeCollectionReader reader;

    public void setUp() throws Exception {
        temporaryFolder.create();

        when(zebedeeMock.getPermissionsService())
                .thenReturn(permissionsService);

        when(zebedeeMock.getCollectionKeyring())
                .thenReturn(keyring);

        when(zebedeeMock.getUsersService())
                .thenReturn(usersService);

        when(collection.getDescription())
                .thenReturn(description);

        when(description.getId())
                .thenReturn(COLLECTION_ID);

        when(permissionsService.canView(userSession, COLLECTION_ID))
                .thenReturn(true);

        when(usersService.getUserByEmail(userSession.getEmail()))
                .thenReturn(builder.publisher1);

        when(keyring.get(userSession, collection))
                .thenReturn(key);

        when(collection.getPath())
                .thenReturn(temporaryFolder.getRoot().toPath());

        when(userSession.getEmail())
                .thenReturn(builder.publisher1.getEmail());

        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);

        setUpPermissionsServiceMockForLegacyTests(zebedee, session);

        SecretKey collectionKey = Keys.newSecretKey();
        setUpKeyringMockForLegacyTests(zebedee, session, collectionKey);

        when(keyring.get(any(), any()))
                .thenReturn(collectionKey);

        reader = new ZebedeeCollectionReader(zebedee, collection, session);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerIfNoUriOnReadContent()
            throws IOException, ZebedeeException {

        // Given
        // A null session
        String uri = null;

        // When
        // We attempt to read content
        reader.getResource(uri);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = NotFoundException.class)
    public void shouldThrowNotFoundForReadingNonexistentFile()
            throws IOException, ZebedeeException {

        // Given
        // A nonexistent file
        String uri = "/this/file/doesnt/exist.json";

        // When
        // We attempt to read the file
        reader.getResource(uri);

        // Then
        // We should get the expected exception
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForReadingADirectoryAsAFile()
            throws IOException, ZebedeeException {

        // Given
        // A uri that defines a directory
        String uri = "/this/is/a/directory/";
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        when(permissionsService.canEdit(userSession))
                .thenReturn(true);
        ReflectionTestUtils.setField(zebedee, "permissionsService", permissionsService);

        assertTrue(collection.create(userSession, uri + "file.json"));

        // When
        // We attempt to read the file
        reader.getResource(uri);

        // Then
        // We should get the expected exception
    }

    @Test
    public void shouldReadFile()
            throws IOException, ZebedeeException {

        when(permissionsService.canEdit(userSession))
                .thenReturn(true);

        ReflectionTestUtils.setField(zebedee, "permissionsService", permissionsService);

        // Given
        // A nonexistent file
        String uri = "/file.json";
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        assertTrue(collection.create(userSession, uri));

        // When
        // We attempt to read the file
        Resource resource = reader.getResource(uri);

        // Then
        // Check the expected interactions
        assertNotNull(resource);
        assertNotNull(resource.getData());
    }

    @Test
    public void testNew_zebedeeNull_shouldThrowException() throws Exception {
        IOException ex = assertThrows(IOException.class, () -> newReader(null, null, null));

        assertThat(ex.getMessage(), equalTo(ZEBEDEE_NULL_ERR));
    }

    @Test
    public void testNew_permissionsServiceNull_shouldThrowException() throws Exception {
        when(zebedeeMock.getPermissionsService())
                .thenReturn(null);

        IOException ex = assertThrows(IOException.class, () -> newReader(zebedeeMock, null, null));

        assertThat(ex.getMessage(), equalTo(PERMISSIONS_SERVICE_NULL_ERR));
        verify(zebedeeMock, times(1)).getPermissionsService();
    }

    @Test
    public void testNew_keyringNull_shouldThrowException() throws Exception {
        when(zebedeeMock.getCollectionKeyring())
                .thenReturn(null);

        IOException ex = assertThrows(IOException.class, () -> newReader(zebedeeMock, null, null));

        assertThat(ex.getMessage(), equalTo(KEYRING_NULL_ERR));
        verify(zebedeeMock, times(1)).getPermissionsService();
        verify(zebedeeMock, times(1)).getCollectionKeyring();
    }


    @Test
    public void testNew_usersServiceNull_shouldThrowException() throws Exception {
        when(zebedeeMock.getUsersService())
                .thenReturn(null);

        IOException ex = assertThrows(IOException.class, () -> newReader(zebedeeMock, null, null));

        assertThat(ex.getMessage(), equalTo(USERS_SERVICE_NULL_ERR));
        verify(zebedeeMock, times(1)).getPermissionsService();
        verify(zebedeeMock, times(1)).getCollectionKeyring();
        verify(zebedeeMock, times(1)).getUsersService();
    }


    @Test
    public void testNew_collectionNull_shouldThrowException() throws Exception {
        NotFoundException ex = assertThrows(NotFoundException.class, () -> newReader(zebedeeMock, null, null));

        assertThat(ex.getMessage(), equalTo(COLLECTION_NULL_ERR));
        verify(zebedeeMock, times(1)).getPermissionsService();
        verify(zebedeeMock, times(1)).getCollectionKeyring();
    }

    @Test
    public void testNew_sessionNull_shouldThrowException() throws Exception {
        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> newReader(zebedeeMock, collection, null));

        assertThat(ex.getMessage(), equalTo(SESSION_NULL_ERR));
        verify(zebedeeMock, times(1)).getPermissionsService();
        verify(zebedeeMock, times(1)).getCollectionKeyring();
    }

    @Test
    public void testNew_checkUserAuthError_shouldThrowException() throws Exception {
        when(permissionsService.canView(userSession, COLLECTION_ID))
                .thenThrow(IOException.class);

        IOException ex = assertThrows(IOException.class,
                () -> newReader(zebedeeMock, collection, userSession));

        assertThat(ex.getMessage(), equalTo(PERMISSIONS_CHECK_ERR));
        verify(permissionsService, times(1)).canView(userSession, COLLECTION_ID);
    }

    @Test
    public void testNew_userAuthDenied_shouldThrowException() throws Exception {
        when(permissionsService.canView(userSession, COLLECTION_ID))
                .thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> newReader(zebedeeMock, collection, userSession));

        assertThat(ex.getMessage(), equalTo(PERMISSION_DENIED_ERR));
        verify(permissionsService, times(1)).canView(userSession, COLLECTION_ID);
    }

    @Test
    public void testNew_getUserError_shouldThrowException() throws Exception {
        when(usersService.getUserByEmail(anyString()))
                .thenThrow(IOException.class);

        IOException ex = assertThrows(IOException.class,
                () -> newReader(zebedeeMock, collection, userSession));

        assertThat(ex.getMessage(), equalTo(GET_USER_ERR));
        verify(permissionsService, times(1)).canView(userSession, COLLECTION_ID);
    }

    @Test
    public void testNew_getKeyError_shouldThrowException() throws Exception {
        when(keyring.get(userSession, collection))
                .thenThrow(KeyringException.class);

        assertThrows(KeyringException.class, () -> newReader(zebedeeMock, collection, userSession));

        verify(permissionsService, times(1)).canView(userSession, COLLECTION_ID);
        verify(keyring, times(1)).get(userSession, collection);
    }

    @Test
    public void testNew_getKeyReturnsNull_shouldThrowException() throws Exception {
        when(keyring.get(userSession, collection))
                .thenReturn(null);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> newReader(zebedeeMock, collection, userSession));

        assertThat(ex.getMessage(), equalTo(COLLECTION_KEY_NULL_ERR));
        verify(permissionsService, times(1)).canView(userSession, COLLECTION_ID);
        verify(keyring, times(1)).get(userSession, collection);
    }

    @Test
    public void testNew_success_shouldCreateNewReader() throws Exception {
        CollectionReader reader = newReader(zebedeeMock, collection, userSession);

        assertThat(reader, is(notNullValue()));
        verify(permissionsService, times(1)).canView(userSession, COLLECTION_ID);
        verify(keyring, times(1)).get(userSession, collection);
    }

    private CollectionReader newReader(Zebedee z, Collection c, Session s) throws Exception {
        return new ZebedeeCollectionReader(z, c, s);
    }
}
