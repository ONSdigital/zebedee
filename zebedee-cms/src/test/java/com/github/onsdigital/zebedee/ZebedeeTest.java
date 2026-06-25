package com.github.onsdigital.zebedee;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.exceptions.CollectionNotFoundException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.service.UsersService;
import com.github.onsdigital.zebedee.util.versioning.VersionsService;
import com.github.onsdigital.zebedee.util.versioning.VersionsServiceImpl;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


public class ZebedeeTest {

    private static final String TEST_EMAIL = "test@ons.gov.uk";
    private static final String[] COLLECTION_NAMES = {"Inflation Q2 2015", "Labour Market Q2 2015"};
    private static final String[] PUBLISHED_CONTENT_URIS = {"/economy/inflationandpriceindices/bulletins/consumerpriceinflationjune2014", "/employmentandlabourmarket/peopleinwork/earningsandworkinghours/bulletins/uklabourmarketjuly2014"};

    @Mock
    private Credentials credentials;

    @Mock
    private Sessions sessions;

    @Mock
    private PermissionsService permissionsService;

    @Mock
    private Session userSession;

    @Mock
    private UsersService usersService;

    @Mock
    private User user;

    @Mock
    private ZebedeeConfiguration zebCfg;

    @Mock
    private CollectionKeyring collectionKeyring;

    Path rootPath;
    private List<Path> collectionPaths;
    private List<String> contentUris;
    private Collections collections;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(zebCfg.getSessions())
                .thenReturn(sessions);

        when(zebCfg.getUsersService())
                .thenReturn(usersService);

        when(zebCfg.getCollectionKeyring())
                .thenReturn(collectionKeyring);

        Root.env = new HashMap<>();

        rootPath = Files.createTempDirectory(Random.id());
        Path collectionsPath = Files.createDirectory(rootPath.resolve(Zebedee.COLLECTIONS));
        Path publishedContentPath = Files.createDirectory(rootPath.resolve(Zebedee.PUBLISHED));

        when(zebCfg.getZebedeePath())
                .thenReturn(rootPath);

        Content publishedContent = new Content(publishedContentPath);
        collections = new Collections(collectionsPath, permissionsService, publishedContent);

        when(zebCfg.getCollections())
                .thenReturn(collections);

        createTestContent();
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(rootPath.toFile());
    }

    @Test
    public void getCollections_shouldListReleases() throws IOException {

        // Given
        Zebedee zebedee = new Zebedee(zebCfg);

        // When
        List<Collection> releases = zebedee.getCollections().list();

        // Then
        assertEquals(this.collectionPaths.size(), releases.size());
    }

    @Test
    public void isBeingEdited_shouldNotBeBeingEdited() throws IOException {

        // Given
        Zebedee zebedee = new Zebedee(zebCfg);

        // When
        int actual = zebedee.isBeingEdited(this.contentUris.get(0));

        // Then
        assertEquals(0, actual);
    }

    @Test
    public void isBeingEdited_shouldBeBeingEdited() throws IOException {

        // Given
        Zebedee zebedee = new Zebedee(zebCfg);
        String path = this.contentUris.get(0).substring(1);
        Path reviewed = this.collectionPaths.get(0).resolve(Collection.REVIEWED);
        Path beingEdited = reviewed.resolve(path);
        Files.createDirectories(beingEdited.getParent());
        Files.createFile(beingEdited);

        // When
        int actual = zebedee.isBeingEdited(this.contentUris.get(0));

        // Then
        assertEquals(1, actual);
    }

    @Test
    public void toUri_givenCollectionFilePath_shouldReturnUri() throws IOException {
        // Given
        // a zebedee implementation
        Zebedee zebedee = new Zebedee(zebCfg);
        String expectedURI = "/expected";
        String inprogress = "collections/mycollection/inprogress/expected/data.json";
        String complete = "collections/mycollection/complete/expected/data.json";
        String reviewed = "collections/mycollection/reviewed/expected/data.json";

        // When
        // we convert these to URIs
        String inProgressURI = zebedee.toUri(zebedee.getPath().resolve(inprogress));
        String completeURI = zebedee.toUri(zebedee.getPath().resolve(complete));
        String reviewedURI = zebedee.toUri(zebedee.getPath().resolve(reviewed));

        // Then
        // we expect the uri
        assertEquals(expectedURI, inProgressURI);
        assertEquals(expectedURI, completeURI);
        assertEquals(expectedURI, reviewedURI);
    }

    @Test
    public void toUri_givenCollectionFilePathAsString_shouldReturnUri() throws IOException {
        // Given
        // a zebedee implementation
        Zebedee zebedee = new Zebedee(zebCfg);
        String expectedURI = "/expected";
        String inprogress = "collections/mycollection/inprogress/expected/data.json";
        String complete = "collections/mycollection/complete/expected/data.json";
        String reviewed = "collections/mycollection/reviewed/expected/data.json";

        // When
        // we convert these to URIs
        String inProgressURI = zebedee.toUri(zebedee.getPath().resolve(inprogress).toString());
        String completeURI = zebedee.toUri(zebedee.getPath().resolve(complete).toString());
        String reviewedURI = zebedee.toUri(zebedee.getPath().resolve(reviewed).toString());

        // Then
        // we expect the uri
        assertEquals(expectedURI, inProgressURI);
        assertEquals(expectedURI, completeURI);
        assertEquals(expectedURI, reviewedURI);
    }

    @Test
    public void toUri_givenPathOutsideZebedee_shouldReturnNull() throws IOException {
        // Given
        // a zebedee implementation
        Zebedee zebedee = new Zebedee(zebCfg);
        String notZebedee = "/NotZebedee/data.json"; // (non zebedee path)
        Path notZebedeePath = Paths.get(notZebedee);

        // When
        // we convert these to URIs
        String notZebedeeUri = zebedee.toUri(notZebedeePath);

        // Then
        // we expect the uri to be null
        assertNull(notZebedeeUri);
    }

    @Test
    public void checkForCollectionBlockingChange_whenCollectionContainsURI_shouldReturnContainingCollection() throws IOException, CollectionNotFoundException {
        Zebedee zebedee = new Zebedee(zebCfg);
        Collection collectionOne = zebedee.getCollections().getCollectionByName(PathUtils.toFilename(COLLECTION_NAMES[0]));
        Collection collectionTwo = zebedee.getCollections().getCollectionByName(PathUtils.toFilename(COLLECTION_NAMES[1]));

        String contentPath = "/aboutus/data.json";

        // create content in collection 01.
        Path inProgress = collectionPaths.get(1).resolve(Collection.IN_PROGRESS);
        createFile(inProgress, contentPath);

        Optional<Collection> blockingCollection = zebedee.checkForCollectionBlockingChange(collectionOne, contentPath);

        assertThat(blockingCollection.isPresent(), is(true));
        assertThat(blockingCollection.get().getDescription().getName(), equalTo(collectionTwo.getDescription().getName()));
        assertThat(collectionTwo.inProgressUris().contains(contentPath), is(true));
        assertThat(collectionTwo.completeUris().contains(contentPath), is(false));
        assertThat(collectionTwo.reviewedUris().contains(contentPath), is(false));
    }

    @Test
    public void checkForCollectionBlockingChange_whenNoCollectionContainsURI_shouldReturnEmptyOptional() throws IOException, CollectionNotFoundException {
        Zebedee zebedee = new Zebedee(zebCfg);
        String contentPath = "/aboutus/data.json";
        Collection collectionOne = zebedee.getCollections().getCollectionByName(PathUtils.toFilename(COLLECTION_NAMES[0]));

        Optional<Collection> blockingCollection = zebedee.checkForCollectionBlockingChange(collectionOne, contentPath);
        assertThat(blockingCollection.isPresent(), is(false));

        Collection collectionTwo = zebedee.getCollections().getCollectionByName(PathUtils.toFilename(COLLECTION_NAMES[1]));
        assertThat(collectionTwo.inProgressUris().contains(contentPath), is(false));
        assertThat(collectionTwo.completeUris().contains(contentPath), is(false));
        assertThat(collectionTwo.reviewedUris().contains(contentPath), is(false));
    }

    @Test
    public void openSession_credentialsNull() throws Exception {
        // Given null credentials
        Credentials credentials = null;

        Zebedee zebedee = new Zebedee(zebCfg);

        // When openSession is called
        Session actual = zebedee.openSession(credentials);

        // Then null is returned
        assertThat(actual, is(nullValue()));
        verifyNoInteractions(sessions, usersService, collectionKeyring);
    }

    @Test
    public void openSession_getUserByEmailException() throws Exception {
        // Given userService.getUserByEmail throws an exception
        when(credentials.getEmail())
                .thenReturn(TEST_EMAIL);

        when(usersService.getUserByEmail(any()))
                .thenThrow(new IOException("boom"));

        Zebedee zebedee = new Zebedee(zebCfg);

        // When openSession is called
        IOException ex = assertThrows(IOException.class, () -> zebedee.openSession(credentials));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo("boom"));
        verify(usersService, times(1)).getUserByEmail(TEST_EMAIL);
        verifyNoMoreInteractions(sessions, usersService, collectionKeyring);
    }

    @Test
    public void openSession_getUserByEmailReturnsNull() throws Exception {
        // Given userService.getUserByEmail returns null
        when(credentials.getEmail())
                .thenReturn(TEST_EMAIL);

        when(usersService.getUserByEmail(any()))
                .thenReturn(null);

        Zebedee zebedee = new Zebedee(zebCfg);

        // When openSession is called
        Session actual = zebedee.openSession(credentials);

        // Then null is returned
        assertThat(actual, is(nullValue()));
        verify(usersService, times(1)).getUserByEmail(TEST_EMAIL);

        verifyNoInteractions(sessions, collectionKeyring);
    }

    @Test
    public void openSession_createSessionThrowsException() throws Exception {
        // Given sessions.create throws an exception
        when(credentials.getEmail())
                .thenReturn(TEST_EMAIL);

        when(usersService.getUserByEmail(any()))
                .thenReturn(user);

        when(user.getEmail())
                .thenReturn(TEST_EMAIL);

        when(sessions.create(TEST_EMAIL))
                .thenThrow(new IOException("boom"));

        Zebedee zebedee = new Zebedee(zebCfg);

        // When openSession is called
        IOException ex = assertThrows(IOException.class, () -> zebedee.openSession(credentials));

        // Then an exception is thrown
        assertThat(ex.getCause().getMessage(), equalTo("boom"));
        verify(usersService, times(1)).getUserByEmail(TEST_EMAIL);
        verify(sessions, times(1)).create(TEST_EMAIL);

        verifyNoInteractions(collectionKeyring);
    }

    @Test
    public void openSession_success_shouldReturnSession() throws Exception {
        // Given open session is successful
        when(credentials.getEmail())
                .thenReturn(TEST_EMAIL);

        when(usersService.getUserByEmail(TEST_EMAIL))
                .thenReturn(user);

        when(user.getEmail())
                .thenReturn(TEST_EMAIL);

        when(sessions.create(TEST_EMAIL))
                .thenReturn(userSession);

        Zebedee zebedee = new Zebedee(zebCfg);

        // When openSession is called
        Session actual = zebedee.openSession(credentials);

        // Then the expected session is returned
        assertThat(actual, equalTo(userSession));
        verify(usersService, times(1)).getUserByEmail(TEST_EMAIL);
        verify(sessions, times(1)).create(TEST_EMAIL);
    }

    /**
     * Generates basic test content.
     * Will generate collections listed in COLLECTION_NAMES and published content listed in PUBLISHED_CONTENT_URIS.
     *
     * @throws IOException If a filesystem error occurs.
     */
    private void createTestContent() throws IOException {
        // Create test collections
        collectionPaths = new ArrayList<>();
        for (String collectionName : COLLECTION_NAMES) {
            Path collection = createCollection(collectionName, rootPath);
            collectionPaths.add(collection);
        }

        // Create test published content
        Path published = rootPath.resolve(Zebedee.PUBLISHED);
        contentUris = new ArrayList<>();
        for (String contentUri : PUBLISHED_CONTENT_URIS) {
            createFile(published, contentUri);
            contentUris.add(contentUri);
        }
    }

    /**
     * Creates a file in the given directory.
     *
     * @param directory The directory to be created.
     * @param uri       The URI to be created.
     * @throws IOException If a filesystem error occurs.
     */
    private Path createFile(Path directory, String uri) throws IOException {
        Path contentPath = directory.resolve(uri.substring(1));
        Files.createDirectories(contentPath.getParent());
        Files.createFile(contentPath);
        return contentPath;
    }

    /**
     * This method creates the expected set of folders for a Zebedee structure.
     * <p>
     * This ensures there's a fixed expectation, rather than relying on a method that will be tested as part
     * of the test suite.
     *
     * @param root The root of the {@link Zebedee} structure
     * @param name The name of the {@link com.github.onsdigital.zebedee.model.Collection}.
     * @return The root {@link com.github.onsdigital.zebedee.model.Collection} path.
     * @throws IOException If a filesystem error occurs.
     */
    private Path createCollection(String name, Path root) throws IOException {

        String filename = PathUtils.toFilename(name);
        Path collections = root.resolve(Zebedee.COLLECTIONS);

        // Create the folders:
        Path collection = collections.resolve(filename);
        Files.createDirectory(collection);
        Files.createDirectory(collection.resolve(com.github.onsdigital.zebedee.model.Collection.REVIEWED));
        Files.createDirectory(collection.resolve(com.github.onsdigital.zebedee.model.Collection.COMPLETE));
        Files.createDirectory(collection.resolve(com.github.onsdigital.zebedee.model.Collection.IN_PROGRESS));

        // Create the description:
        Path collectionDescription = collections.resolve(filename + ".json");
        CollectionDescription description = new CollectionDescription();
        description.setId(Random.id());
        description.setName(name);
        try (OutputStream output = Files.newOutputStream(collectionDescription)) {
            Serialiser.serialise(output, description);
        }

        return collection;
    }
}
