package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.model.CollectionTest.createCollection;
import static com.github.onsdigital.zebedee.model.ZebedeeCollectionReader.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

public class ZebedeeCollectionReaderTest {

    private static final String COLLECTION_NAME = "test collection";
    private static final CollectionType TEST_COLLECTION_TYPE = CollectionType.manual;
    private static final String SESSION_ID = "session-id";
    private static final String SESSION_EMAIL = "user@example.com";
    private static final Session SESSION = new Session(SESSION_ID, SESSION_EMAIL);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private Zebedee zebedee;

    @Mock
    private PermissionsService permissionsService;

    @Mock
    private CollectionKeyring keyring;

    @Mock
    private SecretKey key;

    @Mock
    private UsersService usersService;

    @Mock
    private ReaderConfiguration readerConfiguration;

    private ZebedeeCollectionReader reader;
    private MockedStatic<ReaderConfiguration> readerConfigurationMock;
    private Collection collection;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create test content directories
        temporaryFolder.create();
        Path collectionsPath = temporaryFolder.getRoot().toPath();

        when(zebedee.getPermissionsService())
                .thenReturn(permissionsService);

        when(zebedee.getCollectionKeyring())
                .thenReturn(keyring);

        when(zebedee.getUsersService())
                .thenReturn(usersService);

        // Create a collection instance for use in most tests
        collection = createCollection(collectionsPath, COLLECTION_NAME, zebedee);
        collection.getDescription().setType(TEST_COLLECTION_TYPE);

        when(permissionsService.canView(SESSION, collection.getId(), TEST_COLLECTION_TYPE))
                .thenReturn(true);

        when(keyring.get(eq(SESSION), any(Collection.class)))
                .thenReturn(key);

        // Override reader config to maintain isolation from environment variables
        readerConfigurationMock = Mockito.mockStatic(ReaderConfiguration.class);
        readerConfigurationMock.when(ReaderConfiguration::get).thenReturn(readerConfiguration);
        when(readerConfiguration.getInProgressFolderName()).thenReturn(Collection.IN_PROGRESS);
        when(readerConfiguration.getCompleteFolderName()).thenReturn(Collection.COMPLETE);
        when(readerConfiguration.getReviewedFolderName()).thenReturn(Collection.REVIEWED);

        reader = new ZebedeeCollectionReader(zebedee, collection, SESSION);
    }

    @After
    public void tearDown() {
        readerConfigurationMock.close();
    }

    @Test(expected = NullPointerException.class)
    public void getResource_shouldThrowNullPointerIfNoUriOnReadContent()
            throws IOException, ZebedeeException {

        // Given
        // a null session
        String uri = null;

        // When
        // we attempt to read content
        reader.getResource(uri);

        // Then
        // we should get the expected exception, not a null pointer.
    }

    @Test(expected = NotFoundException.class)
    public void getResource_shouldThrowNotFoundForReadingNonexistentFile()
            throws IOException, ZebedeeException {

        // Given
        // a nonexistent file
        String uri = "/this/file/doesnt/exist.json";

        // When
        // we attempt to read the file
        reader.getResource(uri);

        // Then
        // we should get the expected exception
    }

    @Test(expected = BadRequestException.class)
    public void getResource_whenReadingDirectoryAsFile_shouldThrowBadRequestException()
            throws IOException, ZebedeeException {

        // Given
        // a uri that defines a directory
        String uri = "/this/is/a/directory/";
        Files.createDirectories(collection.getInProgress().getPath().resolve(uri.substring(1)));

        // When
        // we attempt to read the file
        reader.getResource(uri);

        // Then
        // we should get the expected exception
    }

    @Test
    public void getResource_shouldReadFile()
            throws IOException, ZebedeeException {

        // Given
        // a uri that defines a valid file
        String uri = "/file.json";
        PathUtils.create(collection.getInProgress().getPath().resolve(uri.substring(1)));

        // When
        // we attempt to read the file
        Resource resource = reader.getResource(uri);

        // Then
        // check the expected interactions
        assertNotNull(resource);
        assertNotNull(resource.getData());
    }

    @Test
    public void constructor_givenNullZebedee_shouldThrowException() {
        // Given
        // A null zebedee
        Zebedee nullZebedee = null;

        // When
        // constructing a new ZebedeeCollectionReader
        // Then
        // An exception is thrown
        IOException ex = assertThrows(IOException.class, () -> newReader(nullZebedee, collection, SESSION));

        assertThat(ex.getMessage(), equalTo(ZEBEDEE_NULL_ERR));
    }

    @Test
    public void constructor_givenNullPermissionsService_shouldThrowException() {
        // Given
        // a null permissions service
        when(zebedee.getPermissionsService())
                .thenReturn(null);

        // When
        // constructing a new ZebedeeCollectionReader
        // Then
        // an exception is thrown
        IOException ex = assertThrows(IOException.class, () -> newReader(zebedee, collection, SESSION));

        assertThat(ex.getMessage(), equalTo(PERMISSIONS_SERVICE_NULL_ERR));
    }

    @Test
    public void constructor_givenNullCollectionKeyring_shouldThrowException() {
        // Given
        // a null collection keyring
        when(zebedee.getCollectionKeyring())
                .thenReturn(null);

        // When
        // constructing a new ZebedeeCollectionReader
        // Then
        // an exception is thrown
        IOException ex = assertThrows(IOException.class, () -> newReader(zebedee, collection, SESSION));

        assertThat(ex.getMessage(), equalTo(KEYRING_NULL_ERR));
    }


    @Test
    public void constructor_givenNullUsersService_shouldThrowException() {
        // Given
        // a null user service
        when(zebedee.getUsersService())
                .thenReturn(null);

        // When
        // constructing a new ZebedeeCollectionReader
        // Then
        // an exception is thrown
        IOException ex = assertThrows(IOException.class, () -> newReader(zebedee, collection, SESSION));

        assertThat(ex.getMessage(), equalTo(USERS_SERVICE_NULL_ERR));
    }


    @Test
    public void constructor_givenNullCollection_shouldThrowException() {
        // Given
        // a null session
        Collection nullCollection = null;

        // When
        // constructing a new ZebedeeCollectionReader
        // Then
        // an UnauthorizedException is thrown
        NotFoundException ex = assertThrows(NotFoundException.class, () -> newReader(zebedee, nullCollection, SESSION));

        assertThat(ex.getMessage(), equalTo(COLLECTION_NULL_ERR));
    }

    @Test
    public void constructor_givenNullSession_shouldThrowException() {
        // Given
        // a null session
        Session nullSession = null;

        // When
        // constructing a new ZebedeeCollectionReader
        // Then
        // an UnauthorizedException is thrown
        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> newReader(zebedee, collection, nullSession));

        assertThat(ex.getMessage(), equalTo(SESSION_NULL_ERR));
    }

    @Test
    public void constructor_whenAuthCheckError_shouldThrowException() throws Exception {
        // Given
        // authorisation check experiences error
        when(permissionsService.canView(SESSION, collection.getId(), TEST_COLLECTION_TYPE))
                .thenThrow(IOException.class);

        // When
        // constructing a new ZebedeeCollectionReader
        // Then
        // an exception is thrown
        IOException ex = assertThrows(IOException.class,
                () -> newReader(zebedee, collection, SESSION));

        assertThat(ex.getMessage(), equalTo(PERMISSIONS_CHECK_ERR));
    }

    @Test
    public void constructor_givenUnauthorisedSession_shouldThrowException() throws Exception {
        // Given
        // session not authorised to access collection
        when(permissionsService.canView(SESSION, collection.getId(), TEST_COLLECTION_TYPE))
                .thenReturn(false);

        // When
        // constructing a new ZebedeeCollectionReader
        // Then
        // a KeyringException is thrown
        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> newReader(zebedee, collection, SESSION));

        assertThat(ex.getMessage(), equalTo(PERMISSION_DENIED_ERR));
    }

    @Test
    public void constructor_givenKeyringError_shouldThrowException() throws Exception {
        // Given
        // keyring error
        when(keyring.get(SESSION, collection))
                .thenThrow(KeyringException.class);

        // When
        // constructing a new ZebedeeCollectionReader
        // Then
        // a KeyringException is thrown
        assertThrows(KeyringException.class, () -> newReader(zebedee, collection, SESSION));
    }

    @Test
    public void constructor_givenNullKey_shouldThrowException() throws Exception {
        // Given
        // unable to get collection key
        when(keyring.get(SESSION, collection))
                .thenReturn(null);

        // When
        // constructing a new ZebedeeCollectionReader
        // Then
        // an UnauthorisedException is thrown
        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> newReader(zebedee, collection, SESSION));

        assertThat(ex.getMessage(), equalTo(COLLECTION_KEY_NULL_ERR));
    }

    @Test
    public void constructor_givenValidArgs_shouldCreateNewReader() throws Exception {
        // Given
        // valid constructor args

        // When
        // constructing a new ZebedeeCollectionReader
        CollectionReader reader = newReader(zebedee, collection, SESSION);

        // Then
        // the instance should be created successfully
        assertThat(reader, is(notNullValue()));
        assertEquals(collection.getPath(), reader.getRoot().getRootFolder());
    }

    private CollectionReader newReader(Zebedee z, Collection c, Session s) throws Exception {
        return new ZebedeeCollectionReader(z, c, s);
    }
}
