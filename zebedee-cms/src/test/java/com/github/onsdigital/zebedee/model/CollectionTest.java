package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.cryptolite.Keys;
import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.configuration.CMSFeatureFlags;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.release.Release;
import com.github.onsdigital.zebedee.content.page.statistics.document.article.Article;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.content.partial.markdown.MarkdownSection;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.*;
import com.github.onsdigital.zebedee.json.*;
import com.github.onsdigital.zebedee.json.serialiser.IsoDateSerializer;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.model.content.item.ContentItemVersion;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.model.encryption.EncryptionKeyFactory;
import com.github.onsdigital.zebedee.model.publishing.scheduled.DummyScheduler;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.teams.service.TeamsService;
import com.github.onsdigital.zebedee.util.ContentDetailUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.*;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

import static com.github.onsdigital.zebedee.configuration.CMSFeatureFlags.cmsFeatureFlags;
import static com.github.onsdigital.zebedee.model.PathUtils.toFilename;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class CollectionTest {

    private static final String COLLECTION_NAME = "Inflation Q2 2015";
    private static final String SECOND_COLLECTION_NAME = "Trade Q2 2015";
    private static final String TEAM_NAME = "some team";
    private static final String TEAM_ID = "12";
    private static final boolean RECURSIVE = false;

    @Rule
    public TemporaryFolder rootDir = new TemporaryFolder();

    @Mock
    private TeamsService teamsService;

    @Mock
    private Zebedee zebedee;

    @Mock
    private Collections collections;

    @Mock
    private PermissionsService permissionsService;

    @Mock
    private EncryptionKeyFactory encryptionKeyFactory;

    @Mock
    private CollectionKeyring collectionKeyring;

    @Mock
    private CollectionWriter collectionWriter;

    @Mock
    private ReaderConfiguration readerConfiguration;

    private Team team;
    private Collection collection;
    private Session publisher1Session;
    private Session publisher2Session;
    private String publisher1Email;
    private Path collectionsPath;
    private Path publishedPath;
    private Article martin;
    private Article bedford;
    private Article bedfordshire;

    private MockedStatic<ReaderConfiguration> readerConfigurationMock;
    private MockedConstruction<ZebedeeCollectionWriter> zebedeeCollectionWriterMock;
    private MockedConstruction<ZebedeeCollectionReader> zebedeeCollectionReaderMock;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Set ISO date formatting in Gson to match Javascript Date.toISODate()
        Serialiser.getBuilder().registerTypeAdapter(Date.class, new IsoDateSerializer());

        // Create necessary directory structure
        rootDir.create();
        Path rootPath = Paths.get(rootDir.getRoot().getPath());
        collectionsPath = Files.createDirectory(rootPath.resolve(Zebedee.COLLECTIONS));
        publishedPath = Files.createDirectory(rootPath.resolve(Zebedee.PUBLISHED));

        // Initialise zebedee mock
        Content publishedContent = new Content(publishedPath);
        when(zebedee.getPublished()).thenReturn(publishedContent);

        when(zebedee.getCollections()).thenReturn(collections);
        when(collections.getPath()).thenReturn(collectionsPath);

        when(zebedee.getPermissionsService()).thenReturn(permissionsService);
        when(zebedee.getTeamsService()).thenReturn(teamsService);
        when(zebedee.getCollectionKeyring()).thenReturn(collectionKeyring);
        when(zebedee.getEncryptionKeyFactory()).thenReturn(encryptionKeyFactory);

        // override reader config for PageTypeResolver
        readerConfigurationMock = Mockito.mockStatic(ReaderConfiguration.class);
        readerConfigurationMock.when(ReaderConfiguration::get).thenReturn(readerConfiguration);
        when(readerConfiguration.isDatasetImportEnabled()).thenReturn(true);

        // Create some test users with the appropriate permissions
        publisher1Session = new Session("1234", "email1");
        publisher2Session = new Session("5678", "email2");
        publisher1Email = publisher1Session.getEmail();

        when(permissionsService.canEdit(publisher1Session))
                .thenReturn(true);
        when(permissionsService.canEdit(eq(publisher1Session), any(CollectionType.class)))
                .thenReturn(true);
        when(permissionsService.canEdit(publisher2Session))
                .thenReturn(true);
        when(permissionsService.canEdit(eq(publisher2Session), any(CollectionType.class)))
                .thenReturn(true);

        // Create an test team
        team = new Team();
        team.setId(TEAM_ID);
        team.setName(TEAM_NAME);
        when(teamsService.findTeam(TEAM_NAME)).thenReturn(team);

        // Create a collection instance for use in most tests
        collection = createCollection(collectionsPath, COLLECTION_NAME, zebedee);

        when(collectionWriter.getInProgress()).thenReturn(new ContentWriter(collection.getPath().resolve(Collection.IN_PROGRESS)));
        when(collectionWriter.getComplete()).thenReturn(new ContentWriter(collection.getPath().resolve(Collection.COMPLETE)));
        when(collectionWriter.getReviewed()).thenReturn(new ContentWriter(collection.getPath().resolve(Collection.REVIEWED)));
    }

    @After
    public void tearDown() {
        if (readerConfigurationMock != null) {
            readerConfigurationMock.close();
        }
        if (zebedeeCollectionWriterMock != null) {
            zebedeeCollectionWriterMock.close();
        }
        if (zebedeeCollectionReaderMock != null) {
            zebedeeCollectionReaderMock.close();
        }
    }

    @Test
    public void create_shouldCreateCollection() throws Exception {

        // Given
        // The content doesn't exist at any level:
        String name = "Population Release";
        CollectionDescription collectionDescription = new CollectionDescription(name);
        collectionDescription.setType(CollectionType.scheduled);
        collectionDescription.setPublishDate(new Date());
        String filename = toFilename(name);

        // When
        Collection.create(collectionDescription, zebedee, publisher1Session);

        // Then
        Path collectionPath = collectionsPath.resolve(filename);
        Path jsonPath = collectionsPath.resolve(filename + ".json");

        assertTrue(StringUtils.isNotEmpty(collectionDescription.getId()));

        assertTrue(Files.exists(collectionPath));
        assertTrue(Files.exists(jsonPath));
        assertTrue(Files.exists(collectionPath.resolve(Collection.REVIEWED)));
        assertTrue(Files.exists(collectionPath.resolve(Collection.COMPLETE)));
        assertTrue(Files.exists(collectionPath.resolve(Collection.IN_PROGRESS)));

        CollectionDescription createdCollectionDescription;
        try (InputStream inputStream = Files.newInputStream(jsonPath)) {
            createdCollectionDescription = Serialiser.deserialise(inputStream, CollectionDescription.class);
        }

        assertNotNull(createdCollectionDescription);
        assertEquals(collectionDescription.getName(), createdCollectionDescription.getName());
        assertEquals(collectionDescription.getPublishDate(), createdCollectionDescription.getPublishDate());
        assertEquals(ApprovalStatus.NOT_STARTED, createdCollectionDescription.getApprovalStatus());

        verifyKeyAddedToCollectionKeyring();
    }

    @Test
    public void rename_shouldRenameCollection() throws Exception {

        // Given an existing collection
        String name = "Population Release";
        CollectionDescription collectionDescription = new CollectionDescription(name);
        collectionDescription.setType(CollectionType.manual);
        collectionDescription.setPublishDate(new Date());
        String newName = "Economy Release";
        String filename = toFilename(newName);

        // When the rename function is called.

        Collection.create(collectionDescription, zebedee, publisher1Session);
        verifyKeyAddedToCollectionKeyring();

        Collection.rename(collectionDescription, newName, zebedee);

        // Then the collection is renamed.
        Path releasePath = collectionsPath.resolve(filename);
        Path jsonPath = collectionsPath.resolve(filename + ".json");

        Path oldJsonPath = collectionsPath.resolve(toFilename(name) + ".json");

        assertTrue(Files.exists(releasePath));
        assertTrue(Files.exists(jsonPath));
        assertFalse(Files.exists(oldJsonPath));
        assertTrue(Files.exists(releasePath.resolve(Collection.REVIEWED)));
        assertTrue(Files.exists(releasePath.resolve(Collection.COMPLETE)));
        assertTrue(Files.exists(releasePath.resolve(Collection.IN_PROGRESS)));

        CollectionDescription renamedCollectionDescription;
        try (InputStream inputStream = Files.newInputStream(jsonPath)) {
            renamedCollectionDescription = Serialiser.deserialise(inputStream, CollectionDescription.class);
        }

        assertNotNull(renamedCollectionDescription);
        assertEquals(collectionDescription.getId(), renamedCollectionDescription.getId());
        assertEquals(newName, renamedCollectionDescription.getName());
        assertEquals(collectionDescription.getPublishDate(), renamedCollectionDescription.getPublishDate());
        assertEquals(collectionDescription.getType(), renamedCollectionDescription.getType());
    }

    @Test
    public void rename_shouldRenameCollectionSpecialChars() throws Exception {

        // Given an existing collection
        String name = "Collection A $$";
        CollectionDescription collectionDescription = new CollectionDescription(name);
        collectionDescription.setType(CollectionType.manual);
        collectionDescription.setPublishDate(new Date());
        String newName = "Collection A";
        String filename = toFilename(newName);
        Collection.create(collectionDescription, zebedee, publisher1Session);

        // When the rename function is called.
        Collection.rename(collectionDescription, newName, zebedee);

        // Then the collection is renamed.
        Path releasePath = collectionsPath.resolve(filename);
        Path jsonPath = collectionsPath.resolve(filename + ".json");

        Path oldJsonPath = collectionsPath.resolve(toFilename(name) + ".json");

        assertTrue(Files.exists(releasePath));
        assertTrue(Files.exists(jsonPath));
        assertFalse(Files.exists(oldJsonPath));
        assertTrue(Files.exists(releasePath.resolve(Collection.REVIEWED)));
        assertTrue(Files.exists(releasePath.resolve(Collection.COMPLETE)));
        assertTrue(Files.exists(releasePath.resolve(Collection.IN_PROGRESS)));

        CollectionDescription renamedCollectionDescription;
        try (InputStream inputStream = Files.newInputStream(jsonPath)) {
            renamedCollectionDescription = Serialiser.deserialise(inputStream, CollectionDescription.class);
        }

        assertNotNull(renamedCollectionDescription);
        assertEquals(collectionDescription.getId(), renamedCollectionDescription.getId());
        assertEquals(newName, renamedCollectionDescription.getName());
        assertEquals(collectionDescription.getPublishDate(), renamedCollectionDescription.getPublishDate());
        assertEquals(collectionDescription.getType(), renamedCollectionDescription.getType());
    }

    @Test
    public void rename_shouldRenameCollectionSameaName() throws Exception {

        // Given an existing collection
        String name = "Collection A";
        CollectionDescription collectionDescription = new CollectionDescription(name);
        collectionDescription.setType(CollectionType.manual);
        collectionDescription.setPublishDate(new Date());
        String newName = "Collection A";
        String filename = toFilename(newName);
        Collection.create(collectionDescription, zebedee, publisher1Session);

        // When the rename function is called.
        Collection.rename(collectionDescription, newName, zebedee);

        // Then the collection is renamed.
        Path releasePath = collectionsPath.resolve(filename);
        Path jsonPath = collectionsPath.resolve(filename + ".json");

        Path oldJsonPath = collectionsPath.resolve(toFilename(name) + ".json");

        assertTrue(Files.exists(releasePath));
        assertTrue(Files.exists(jsonPath));
        assertTrue(Files.exists(oldJsonPath));
        assertTrue(Files.exists(releasePath.resolve(Collection.REVIEWED)));
        assertTrue(Files.exists(releasePath.resolve(Collection.COMPLETE)));
        assertTrue(Files.exists(releasePath.resolve(Collection.IN_PROGRESS)));

        CollectionDescription renamedCollectionDescription;
        try (InputStream inputStream = Files.newInputStream(jsonPath)) {
            renamedCollectionDescription = Serialiser.deserialise(inputStream, CollectionDescription.class);
        }

        assertNotNull(renamedCollectionDescription);
        assertEquals(collectionDescription.getId(), renamedCollectionDescription.getId());
        assertEquals(newName, renamedCollectionDescription.getName());
        assertEquals(collectionDescription.getPublishDate(), renamedCollectionDescription.getPublishDate());
        assertEquals(collectionDescription.getType(), renamedCollectionDescription.getType());
    }

    @Test
    public void update_shouldUpdateCollection() throws Exception {
        Set<String> teamIds = new HashSet<>(Arrays.asList(TEAM_ID));

        doNothing().when(permissionsService).setViewerTeams(
                publisher1Session, collection.getDescription().getId(), teamIds);

        // Given an existing collection
        String name = "Population Release";
        CollectionDescription collectionDescription = new CollectionDescription(name);
        collectionDescription.setType(CollectionType.manual);
        collectionDescription.setPublishDate(new Date());
        collectionDescription.setTeams(new ArrayList<>());
        Collection collection = Collection.create(collectionDescription, zebedee, publisher1Session);

        // When the collection is updated
        String newName = "Economy Release";
        String filename = toFilename(newName);
        CollectionDescription updatedDescription = new CollectionDescription(newName);
        updatedDescription.setType(CollectionType.scheduled);
        updatedDescription.setPublishDate(new DateTime(collectionDescription.getPublishDate()).plusHours(1).toDate());
        updatedDescription.setId(collectionDescription.getId());

        /*
        The prior to JWT sessions, Florence was incorrectly passing the team names as the team IDs in the description.
        To address this zebedee had a "fix" to lookup the IDs when reading the team IDs from the collection description.
        This issue was addressed in Florence and the IDs are now correctly passed to zebedee. The following conditional
        mimics this behaviour.
         */
        if (cmsFeatureFlags().isJwtSessionsEnabled()) {
            updatedDescription.setTeams(Arrays.asList(TEAM_ID));
        } else {
            updatedDescription.setTeams(Arrays.asList(TEAM_NAME));
        }

        setUpKeyringMocks();

        Collection.update(collection, updatedDescription, zebedee, new DummyScheduler(), publisher1Session);

        // Then the properties of the description passed to update have been updated.
        Path collectionFolderPath = collectionsPath.resolve(filename);
        Path collectionJsonPath = collectionsPath.resolve(filename + ".json");

        Path oldJsonPath = collectionsPath.resolve(toFilename(name) + ".json");

        assertTrue(Files.exists(collectionFolderPath));
        assertTrue(Files.exists(collectionJsonPath));
        assertFalse(Files.exists(oldJsonPath));

        CollectionDescription updatedCollectionDescription;
        try (InputStream inputStream = Files.newInputStream(collectionJsonPath)) {
            updatedCollectionDescription = Serialiser.deserialise(inputStream, CollectionDescription.class);
        }

        assertNotNull(updatedCollectionDescription);
        assertEquals(collectionDescription.getId(), updatedCollectionDescription.getId());
        assertEquals(newName, updatedCollectionDescription.getName());
        assertEquals(updatedDescription.getType(), updatedCollectionDescription.getType());
        assertEquals(updatedDescription.getPublishDate(), updatedCollectionDescription.getPublishDate());
        assertTrue(updatedCollectionDescription.getEvents().hasEventForType(EventType.CREATED));
        assertEquals(updatedDescription.getTeams(), updatedCollectionDescription.getTeams());
        verify(permissionsService, times(1)).setViewerTeams(
                publisher1Session, collection.getDescription().getId(), teamIds);
    }

    @Test
    public void update_shouldNotCallPermissionsServiceWhenPermissionsApiEnabled() throws Exception {
        System.setProperty("ENABLE_PERMISSIONS_API", "true");
        CMSFeatureFlags.reset();

        try {
            // Given an existing collection
            String name = "Population Release";
            CollectionDescription collectionDescription = new CollectionDescription(name);
            collectionDescription.setType(CollectionType.manual);
            collectionDescription.setPublishDate(new Date());
            collectionDescription.setTeams(new ArrayList<>());
            Collection collection = Collection.create(collectionDescription, zebedee, publisher1Session);

            // When the collection is updated
            CollectionDescription updatedDescription = new CollectionDescription(name);
            updatedDescription.setId(collectionDescription.getId());

            /*
            The prior to JWT sessions, Florence was incorrectly passing the team names as the team IDs in the description.
            To address this zebedee had a "fix" to lookup the IDs when reading the team IDs from the collection description.
            This issue was addressed in Florence and the IDs are now correctly passed to zebedee. The following conditional
            mimics this behaviour.
             */
            if (cmsFeatureFlags().isJwtSessionsEnabled()) {
                updatedDescription.setTeams(Arrays.asList(TEAM_ID));
            } else {
                updatedDescription.setTeams(Arrays.asList(TEAM_NAME));
            }

            Collection.update(collection, updatedDescription, zebedee, new DummyScheduler(), publisher1Session);

            // Then permissions service is not invoked when Permissions API is enabled
            verify(permissionsService, times(0)).setViewerTeams(any(), any(), any());
        } finally {
            System.clearProperty("ENABLE_PERMISSIONS_API");
            CMSFeatureFlags.reset();
        }
    }

    @Test
    public void update_shouldRemoveViewerTeams() throws Exception {
        // Given an existing collection
        String name = "Population Release 2";
        CollectionDescription collectionDescription = new CollectionDescription(name);
        collectionDescription.setType(CollectionType.manual);
        collectionDescription.setPublishDate(new Date());

        /*
        The prior to JWT sessions, Florence was incorrectly passing the team names as the team IDs in the description.
        To address this zebedee had a "fix" to lookup the IDs when reading the team IDs from the collection description.
        This issue was addressed in Florence and the IDs are now correctly passed to zebedee. The following conditional
        mimics this behaviour.
         */
        if (cmsFeatureFlags().isJwtSessionsEnabled()) {
            collectionDescription.setTeams(Arrays.asList(TEAM_ID));
        } else {
            collectionDescription.setTeams(Arrays.asList(TEAM_NAME));
        }        Collection collection = Collection.create(collectionDescription, zebedee, publisher1Session);

        // When the collection is updated
        String filename = toFilename(name);
        CollectionDescription updatedDescription = new CollectionDescription(name);
        updatedDescription.setId(collectionDescription.getId());
        updatedDescription.setTeams(new ArrayList<>());

        Collection.update(collection, updatedDescription, zebedee, new DummyScheduler(), publisher1Session);

        // Then the properties of the description passed to update have been updated.
        Path collectionFolderPath = collectionsPath.resolve(filename);
        Path collectionJsonPath = collectionsPath.resolve(filename + ".json");

        assertTrue(Files.exists(collectionFolderPath));
        assertTrue(Files.exists(collectionJsonPath));

        CollectionDescription updatedCollectionDescription;
        try (InputStream inputStream = Files.newInputStream(collectionJsonPath)) {
            updatedCollectionDescription = Serialiser.deserialise(inputStream, CollectionDescription.class);
        }

        assertNotNull(updatedCollectionDescription);
        assertEquals(collectionDescription.getId(), updatedCollectionDescription.getId());
        assertTrue(updatedCollectionDescription.getEvents().hasEventForType(EventType.CREATED));
        assertEquals(updatedDescription.getTeams(), updatedCollectionDescription.getTeams());
         verify(permissionsService, times(1)).setViewerTeams(
                publisher1Session, collection.getDescription().getId(), new HashSet<String>());
    }

    @Test
    public void update_shouldUpdateCollectionNameIfCaseIsChanged() throws Exception {

        // Given an existing collection
        String name = "population release";
        CollectionDescription collectionDescription = new CollectionDescription(name);
        collectionDescription.setType(CollectionType.manual);
        collectionDescription.setPublishDate(new Date());
        Collection collection = Collection.create(collectionDescription, zebedee, publisher1Session);

        // When the collection is updated
        String newName = "Population Release";
        String filename = toFilename(newName);
        CollectionDescription updatedDescription = new CollectionDescription(newName);
        updatedDescription.setType(CollectionType.manual);
        updatedDescription.setPublishDate(new Date());
        Collection.update(collection, updatedDescription, zebedee, new DummyScheduler(), publisher1Session);

        // Then the properties of the description passed to update have been updated.
        Path collectionFolderPath = collectionsPath.resolve(filename);
        Path collectionJsonPath = collectionsPath.resolve(filename + ".json");

        assertTrue(Files.exists(collectionFolderPath));
        assertTrue(Files.exists(collectionJsonPath));

        CollectionDescription updatedCollectionDescription;
        try (InputStream inputStream = Files.newInputStream(collectionJsonPath)) {
            updatedCollectionDescription = Serialiser.deserialise(inputStream, CollectionDescription.class);
        }

        assertNotNull(updatedCollectionDescription);
        assertEquals(collectionDescription.getId(), updatedCollectionDescription.getId());
        assertEquals(newName, updatedCollectionDescription.getName());
        assertEquals(updatedDescription.getType(), updatedCollectionDescription.getType());
        assertTrue(updatedCollectionDescription.getEvents().hasEventForType(EventType.CREATED));
    }

    @Test
    public void update_shouldUpdateScheduleTimeForAScheduledCollection() throws Exception {

        // Given an existing collection that has been scheduled
        String name = "Population Release";
        CollectionDescription collectionDescription = new CollectionDescription(name);
        collectionDescription.setPublishDate(DateTime.now().plusSeconds(2).toDate());
        collectionDescription.setType(CollectionType.scheduled);
        collectionDescription.setTeams(new ArrayList<>());
        Collection collection = Collection.create(collectionDescription, zebedee, publisher1Session);

        DummyScheduler scheduler = new DummyScheduler();
        scheduler.schedulePublish(collection, zebedee);

        // When the collection is updated with a new release time
        String newName = "Economy Release";
        CollectionDescription updatedDescription = new CollectionDescription(newName);
        updatedDescription.setType(CollectionType.scheduled);
        updatedDescription.setPublishDate(DateTime.now().plusSeconds(10).toDate());
        Collection updated = Collection.update(collection, updatedDescription, zebedee, scheduler, publisher1Session);

        assertTrue(scheduler.taskExistsForCollection(updated));
        long timeUntilTaskRun = scheduler.getTaskForCollection(updated).getDelay(TimeUnit.SECONDS);
        assertTrue(timeUntilTaskRun > 8);
    }

    @Test(expected = BadRequestException.class)
    public void update_givenNullCollection_shouldThrowBadRequestException() throws Exception {

        // Given a null collection
        Collection collection = null;

        // When we call the static update method
        Collection.update(collection, new CollectionDescription("name"), zebedee, new DummyScheduler(),
                publisher1Session);

        // Then the expected exception is thrown.
    }

    @Test(expected = CollectionNotFoundException.class)
    public void constructor_shouldNotInstantiateInInvalidFolder() throws Exception {

        // Given
        // A folder that isn't a valid release:
        String name = "Population Release";
        CollectionDescription collectionDescription = new CollectionDescription(name);
        collectionDescription.setType(CollectionType.scheduled);
        collectionDescription.setPublishDate(new Date());

        Collection.create(collectionDescription, zebedee, publisher1Session);

        Path releasePath = collectionsPath.resolve(toFilename(name));
        FileUtils.cleanDirectory(releasePath.toFile());

        // When
        new Collection(releasePath, zebedee);

        // Then
        // We should get an exception.
    }

    @Test
    public void create_shouldCreate() throws IOException {

        // Given
        // The content doesn't exist at any level:
        String uri = "/economy/inflationandpriceindices/timeseries/abmi.html";

        // When
        boolean created = collection.create(publisher1Session, uri);

        // Then
        assertTrue(created);
        Path inProgress = collection.getPath().resolve(Collection.IN_PROGRESS);
        assertTrue(Files.exists(inProgress.resolve(uri.substring(1))));

        // check an event has been created for the content being created.
        assertTrue(collection.getDescription().getEventsByUri().get(uri).hasEventForType(EventType.CREATED));
    }

    @Test
    public void constructor_shouldAllowWritableCollectionWhenReadOnlyCollectionExists() throws Exception {

        Collection readOnlyCollection = new Collection(collection.getPath(), zebedee);
        Collection writeableCollection = null;

        try {
            writeableCollection = new Collection(collection.getPath(), zebedee, true);

            assertNotNull(writeableCollection);
            assertEquals(readOnlyCollection.getDescription().getId(), writeableCollection.getDescription().getId());
        } finally {
            if (writeableCollection != null) {
                writeableCollection.close();
            }
        }
    }

    @Test
    public void constructor_shouldBlockSecondWritableCollectionUntilFirstIsClosed() throws Exception {

        // Given collection write locking is enabled
        System.setProperty(CMSFeatureFlags.ENABLE_COLLECTION_WRITE_LOCKING, "true");
        CMSFeatureFlags.reset();

        try {
            Collection firstWriteableCollection = new Collection(collection.getPath(), zebedee, true);
            CountDownLatch secondConstructorStarted = new CountDownLatch(1);
            CountDownLatch secondConstructorFinished = new CountDownLatch(1);
            AtomicBoolean secondCollectionCreated = new AtomicBoolean(false);
            AtomicReference<Throwable> threadFailure = new AtomicReference<>();

            Thread secondCollectionThread = new Thread(() -> {
                secondConstructorStarted.countDown();
                try {
                    Collection secondWriteableCollection = new Collection(collection.getPath(), zebedee, true);
                    try {
                        secondCollectionCreated.set(true);
                    } finally {
                        secondWriteableCollection.close();
                    }
                } catch (Throwable t) {
                    threadFailure.set(t);
                } finally {
                    secondConstructorFinished.countDown();
                }
            });

            secondCollectionThread.start();

            try {
                assertTrue(secondConstructorStarted.await(1, TimeUnit.SECONDS));
                assertFalse(secondConstructorFinished.await(200, TimeUnit.MILLISECONDS));

                firstWriteableCollection.close();

                assertTrue(secondConstructorFinished.await(2, TimeUnit.SECONDS));
                assertNull(threadFailure.get());
                assertTrue(secondCollectionCreated.get());
            } finally {
                secondCollectionThread.join(TimeUnit.SECONDS.toMillis(2));
            }
        } finally {
            System.clearProperty(CMSFeatureFlags.ENABLE_COLLECTION_WRITE_LOCKING);
            CMSFeatureFlags.reset();
        }
    }

    @Test
    public void constructor_concurrentWriteLocksOnDifferentCollectionsShouldNotDeadlockWhenCallingList() throws Exception {

        // Given collection write locking is enabled
        System.setProperty(CMSFeatureFlags.ENABLE_COLLECTION_WRITE_LOCKING, "true");
        CMSFeatureFlags.reset();

        try {
            // Two different collections - each thread holds a write lock on one
            // and then calls collections.list(), which previously tried to acquire
            // a read lock on every collection including the one the other thread
            // holds a write lock on, causing circular wait.
            Path collectionPath1 = collection.getPath();
            Collection collection2 = createCollection(collectionsPath, SECOND_COLLECTION_NAME, zebedee);
            Path collectionPath2 = collection2.getPath();

            CountDownLatch bothLocksHeld = new CountDownLatch(2);
            CountDownLatch bothListsDone = new CountDownLatch(2);
            AtomicReference<Throwable> failure = new AtomicReference<>();

            Thread thread1 = new Thread(() -> {
                try {
                    Collection c1 = new Collection(collectionPath1, zebedee, true);
                    bothLocksHeld.countDown();
                    bothLocksHeld.await(); // don't call list() until both write locks are held
                    try {
                        zebedee.getCollections().list();
                    } finally {
                        c1.close();
                    }
                } catch (Throwable t) {
                    failure.set(t);
                } finally {
                    bothListsDone.countDown();
                }
            });

            Thread thread2 = new Thread(() -> {
                try {
                    Collection c2 = new Collection(collectionPath2, zebedee, true);
                    bothLocksHeld.countDown();
                    bothLocksHeld.await();
                    try {
                        zebedee.getCollections().list();
                    } finally {
                        c2.close();
                    }
                } catch (Throwable t) {
                    failure.set(t);
                } finally {
                    bothListsDone.countDown();
                }
            });

            thread1.start();
            thread2.start();

            // If this times out the threads are deadlocked
            assertTrue("Deadlock detected: threads did not complete within timeout",
                    bothListsDone.await(5, TimeUnit.SECONDS));
            assertNull("Thread threw unexpected exception: " + failure.get(), failure.get());

            thread1.join(TimeUnit.SECONDS.toMillis(5));
            thread2.join(TimeUnit.SECONDS.toMillis(5));
        } finally {
            System.clearProperty(CMSFeatureFlags.ENABLE_COLLECTION_WRITE_LOCKING);
            CMSFeatureFlags.reset();
        }
    }

    @Test
    public void constructor_shouldReleaseWriteLockWhenWritableConstructionFails() throws Exception {

        Path collectionPath = collection.getPath();
        Path collectionJsonPath = collectionPath.getParent().resolve(collectionPath.getFileName() + ".json");

        FileUtils.write(collectionJsonPath.toFile(), "not valid json", Charset.defaultCharset());

        assertThrows(IOException.class,() -> {
            new Collection(collectionPath, zebedee, true);
        });

        try (OutputStream outputStream = Files.newOutputStream(collectionJsonPath)) {
            Serialiser.serialise(outputStream, collection.getDescription());
        }

        Collection writeableCollection = null;
        try {
            writeableCollection = new Collection(collectionPath, zebedee, true);
            assertNotNull(writeableCollection);
        } finally {
            if (writeableCollection != null) {
                writeableCollection.close();
            }
        }
    }

    @Test
    public void constructor_shouldNotThrowWhenClosingWritableCollectionAfterDelete() throws Exception {

        Path collectionPath = collection.getPath();
        Path collectionJsonPath = collectionPath.getParent().resolve(collectionPath.getFileName() + ".json");
        Collection writeableCollection = new Collection(collectionPath, zebedee, true);

        writeableCollection.delete();
        assertFalse(Files.exists(collectionPath));
        assertFalse(Files.exists(collectionJsonPath));
        writeableCollection.close();
    }

    @Test
    public void delete_shouldReleaseLockSoWaitingThreadIsUnblocked() throws Exception {

        // Given collection write locking is enabled
        System.setProperty(CMSFeatureFlags.ENABLE_COLLECTION_WRITE_LOCKING, "true");
        CMSFeatureFlags.reset();

        try {

            // Given a writeable collection held open by one thread (simulating a publish)
            Path collectionPath = collection.getPath();
            Collection writeableCollection = new Collection(collectionPath, zebedee, true);

            // Grab the write lock reference now, before delete() removes it from the map.
            // This mirrors the Publisher pattern: it calls collection.getWriteLock().lock()
            // on a reference it already holds, concurrent with another thread that may call delete().
            Lock writeLock = writeableCollection.getWriteLock();

            CountDownLatch waitingThreadStarted = new CountDownLatch(1);
            CountDownLatch waitingThreadFinished = new CountDownLatch(1);
            AtomicBoolean lockAcquired = new AtomicBoolean(false);
            AtomicReference<Throwable> threadFailure = new AtomicReference<>();

            // A second thread blocks on the same write lock, simulating any concurrent
            // request (content save, review, etc.) that is waiting while a publish/delete runs.
            Thread waitingThread = new Thread(() -> {
                waitingThreadStarted.countDown();
                try {
                    writeLock.lock(); // blocks because writeableCollection holds it
                    lockAcquired.set(true);
                } catch (Throwable t) {
                    threadFailure.set(t);
                } finally {
                    if (lockAcquired.get()) {
                        writeLock.unlock();
                    }
                    waitingThreadFinished.countDown();
                }
            });

            waitingThread.start();
            assertTrue("waiting thread did not start", waitingThreadStarted.await(1, TimeUnit.SECONDS));

            // The second thread should be blocked waiting for the write lock.
            assertFalse("second thread acquired lock before delete - test precondition failed",
                    waitingThreadFinished.await(200, TimeUnit.MILLISECONDS));

            // When delete() is called it must release the write lock.
            writeableCollection.delete();

            // Then the waiting thread should be unblocked.
            assertTrue("waiting thread was not unblocked after delete()",
                    waitingThreadFinished.await(3, TimeUnit.SECONDS));
            assertNull("waiting thread threw an exception: " + threadFailure.get(), threadFailure.get());
            assertTrue(lockAcquired.get());

            waitingThread.join(TimeUnit.SECONDS.toMillis(3));
        } finally {
            System.clearProperty(CMSFeatureFlags.ENABLE_COLLECTION_WRITE_LOCKING);
            CMSFeatureFlags.reset();
        }
    }

    @Test
    public void create_shouldNotCreateIfPublished() throws IOException {

        // Given
        // The content already exists:
        String uri = "/economy/inflationandpriceindices/timeseries/abmi.html";
        createPublishedFile(uri);

        // When
        boolean created = collection.create(publisher1Session, uri);

        // Then
        assertFalse(created);
        Path inProgress = collection.getPath().resolve(Collection.IN_PROGRESS);
        assertFalse(Files.exists(inProgress.resolve(uri.substring(1))));
    }

    @Test
    public void create_shouldNotCreateIfReviewed() throws IOException {

        // Given
        // The content already exists:
        String uri = "/economy/inflationandpriceindices/timeseries/abmi.html";
        createReviewedFile(uri);

        // When
        boolean created = collection.create(publisher1Session, uri);

        // Then
        assertFalse(created);
        Path inProgress = collection.getPath().resolve(Collection.IN_PROGRESS);
        assertFalse(Files.exists(inProgress.resolve(uri.substring(1))));
    }

    @Test
    public void create_shouldNotCreateIfComplete() throws IOException {

        // Given
        // The content already exists:
        String uri = "/economy/inflationandpriceindices/timeseries/abmi.html";
        createReviewedFile(uri);

        // When
        boolean created = collection.create(publisher1Session, uri);

        // Then
        assertFalse(created);
        Path inProgress = collection.getPath().resolve(Collection.IN_PROGRESS);
        assertFalse(Files.exists(inProgress.resolve(uri.substring(1))));
    }

    @Test
    public void create_shouldNotCreateIfInProgress() throws IOException {

        // Given
        // The content already exists:
        String uri = "/economy/inflationandpriceindices/timeseries/abmi.html";
        createInProgressFile(uri);

        // When
        boolean created = collection.create(publisher1Session, uri);

        // Then
        assertFalse(created);
    }

    @Test
    public void deleteContentDirectory_shouldDeleteAllFilesFromInProgressDirectory() throws IOException {

        // Given a content instance with a json file and csv file in it.
        String jsonFile = Random.id() + ".json";
        String csvFile = Random.id() + ".csv";

        createInProgressFile("/" + jsonFile);
        createInProgressFile("/" + csvFile);

        Path inProgress = collection.getPath().resolve(Collection.IN_PROGRESS);

        // When the delete method is called on the json file
        boolean result = collection.deleteContentDirectory(publisher1Session.getEmail(), jsonFile);

        // Then both the json file and csv file are deleted.
        assertTrue(result);
        assertFalse(Files.exists(inProgress.resolve(jsonFile)));
        assertFalse(Files.exists(inProgress.resolve(csvFile)));
        // check an event has been created for the content being deleted.
        collection.getDescription().getEventsByUri().get("/" + jsonFile).hasEventForType(EventType.DELETED);
    }

    @Test
    public void deleteContentDirectory_shouldDeleteAllFilesFromCompleteDirectory() throws IOException {

        // Given a content instance with a json file and csv file in it.
        String jsonFile = Random.id() + ".json";
        String csvFile = Random.id() + ".csv";

        createCompleteFile("/" + jsonFile);
        createCompleteFile("/" + csvFile);

        Path root = collection.getPath().resolve(Collection.COMPLETE);

        // When the delete method is called on the json file
        boolean result = collection.deleteContentDirectory(publisher1Session.getEmail(), jsonFile);

        // Then both the json file and csv file are deleted.
        assertTrue(result);
        assertFalse(Files.exists(root.resolve(jsonFile)));
        assertFalse(Files.exists(root.resolve(csvFile)));
        collection.getDescription().getEventsByUri().get("/" + jsonFile).hasEventForType(EventType.DELETED);
    }

    @Test
    public void deleteContentDirectory_shouldDeleteAllFilesFromReviewedDirectory() throws IOException {

        // Given a content instance with a json file and csv file in it.
        String jsonFile = Random.id() + ".json";
        String csvFile = Random.id() + ".csv";

        createReviewedFile("/" + jsonFile);
        createReviewedFile("/" + csvFile);

        Path root = collection.getPath().resolve(Collection.REVIEWED);

        // When the delete method is called on the json file
        boolean result = collection.deleteContentDirectory(publisher1Email, jsonFile);

        // Then both the json file and csv file are deleted.
        assertTrue(result);
        assertFalse(Files.exists(root.resolve(jsonFile)));
        assertFalse(Files.exists(root.resolve(csvFile)));
        collection.getDescription().getEventsByUri().get("/" + jsonFile).hasEventForType(EventType.DELETED);
    }

    @Test
    public void deleteFile_shouldDeleteOnlyGivenFileFromReviewedDirectory() throws IOException {

        // Given a content instance with a json file and csv file in it.
        String jsonFile = Random.id() + ".json";
        String csvFile = Random.id() + ".csv";

        createReviewedFile("/" + jsonFile);
        createReviewedFile("/" + csvFile);

        Path root = collection.getPath().resolve(Collection.REVIEWED);

        // When the delete method is called on the json file
        boolean result = collection.deleteFile(jsonFile);

        // Then both the json file and csv file are deleted.
        assertTrue(result);
        assertFalse(Files.exists(root.resolve(jsonFile)));
        assertTrue(Files.exists(root.resolve(csvFile)));
    }

    @Test
    public void deleteFile_shouldDeleteOnlyGivenFileFromCompleteDirectory() throws IOException {

        // Given a content instance with a json file and csv file in it.
        String jsonFile = Random.id() + ".json";
        String csvFile = Random.id() + ".csv";

        createCompleteFile("/" + jsonFile);
        createCompleteFile("/" + csvFile);

        Path root = collection.getPath().resolve(Collection.COMPLETE);

        // When the delete method is called on the json file
        boolean result = collection.deleteFile(jsonFile);

        // Then both the json file and csv file are deleted.
        assertTrue(result);
        assertFalse(Files.exists(root.resolve(jsonFile)));
        assertTrue(Files.exists(root.resolve(csvFile)));
    }

    @Test
    public void deleteFile_shouldDeleteOnlyGivenFileFromInProgressDirectory() throws IOException {

        // Given a content instance with a json file and csv file in it.
        String jsonFile = Random.id() + ".json";
        String csvFile = Random.id() + ".csv";

        createInProgressFile("/" + jsonFile);
        createInProgressFile("/" + csvFile);

        Path root = collection.getPath().resolve(Collection.IN_PROGRESS);

        // When the delete method is called on the json file
        boolean result = collection.deleteFile(jsonFile);

        // Then both the json file and csv file are deleted.
        assertTrue(result);
        assertFalse(Files.exists(root.resolve(jsonFile)));
        assertTrue(Files.exists(root.resolve(csvFile)));
    }

    @Test
    public void edit_shouldEditPublished() throws IOException, BadRequestException {

        // Given
        // The content exists publicly:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        createPublishedFile(uri);

        // When
        boolean edited = collection.edit(publisher1Session, uri, collectionWriter, RECURSIVE);

        // Then
        assertTrue(edited);
        Path inProgress = collection.getPath().resolve(Collection.IN_PROGRESS);
        assertTrue(Files.exists(inProgress.resolve(uri.substring(1))));

        Path content = publishedPath.resolve(uri.substring(1));
        assertTrue(Files.exists(content));

        // check an event has been created for the content being created.
        collection.getDescription().getEventsByUri().get(uri).hasEventForType(EventType.EDITED);
    }

    @Test
    public void edit_shouldEditComplete() throws IOException, BadRequestException {

        // Given
        // The content exists, has been edited and completed:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        createPublishedFile(uri);
        createCompleteFile(uri);

        // When
        boolean edited = collection.edit(publisher1Session, uri, collectionWriter, RECURSIVE);

        // Then
        // It should be edited
        assertTrue(edited);

        // It should be in in progress
        Path inProgress = collection.getPath().resolve(Collection.IN_PROGRESS);
        assertTrue(Files.exists(inProgress.resolve(uri.substring(1))));

        // check the file no longer exists in complete, the previous version is no
        // longer wanted.
        Path complete = collection.getPath().resolve(Collection.COMPLETE);
        assertFalse(Files.exists(complete.resolve(uri.substring(1))));
    }

    @Test
    public void edit_shouldEditReviewed() throws IOException, BadRequestException {

        // Given
        // The content exists, has been edited and reviewed:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        createPublishedFile(uri);
        createReviewedFile(uri);

        // When
        boolean edited = collection.edit(publisher1Session, uri, collectionWriter, RECURSIVE);

        // Then
        // It should be edited
        assertTrue(edited);

        // It should be in in progress
        Path inProgress = collection.getPath().resolve(Collection.IN_PROGRESS);
        assertTrue(Files.exists(inProgress.resolve(uri.substring(1))));

        // check the file no longer exists in reviewed, the previous version is no
        // longer wanted.
        Path reviewed = collection.getPath().resolve(Collection.REVIEWED);
        assertFalse(Files.exists(reviewed.resolve(uri.substring(1))));
    }

    @Test
    public void edit_shouldEditIfEditingAlready() throws IOException, BadRequestException {

        // Given
        // The content already exists:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        createInProgressFile(uri);

        // When
        boolean edited = collection.edit(publisher1Session, uri, collectionWriter, RECURSIVE);

        // Then
        assertTrue(edited);
    }

    @Test
    public void edit_shouldNotEditIfEditingElsewhere() throws IOException, BadRequestException {

        // Given
        // The content already exists in another release:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        isBeingEditedElsewhere(uri);

        // When
        boolean edited = collection.edit(publisher1Session, uri, collectionWriter, RECURSIVE);

        // Then
        assertFalse(edited);
    }

    @Test
    public void edit_shouldNotEditIfDoesNotExist() throws IOException, BadRequestException {

        // Given
        // The content does not exist:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";

        // When
        boolean edited = collection.edit(publisher1Session, uri, collectionWriter, RECURSIVE);

        // Then
        assertFalse(edited);
    }

    @Test
    public void review_shouldReviewWithReviewer() throws IOException, ZebedeeException {

        // Given
        // The content exists, has been edited and complete:
        String uri = CreateCompleteContent();

        // When
        // One of the digital publishing team reviews it
        boolean reviewed = collection.review(publisher2Session, uri, RECURSIVE);

        // Then
        // The content should be reviewed and no longer located in "in progress"
        assertTrue(reviewed);
        Path edited = collection.getPath().resolve(Collection.IN_PROGRESS);
        assertFalse(Files.exists(edited.resolve(uri.substring(1))));

        // check an event has been created for the content being created.
        collection.getDescription().getEventsByUri().get(uri).hasEventForType(EventType.REVIEWED);
    }

    @Test(expected = ForbiddenException.class)
    public void review_shouldNotReviewAsPublisher() throws IOException, ZebedeeException {

        // Given
        // The content exists, has been edited and complete by publisher1:
        String uri = CreateCompleteContent();

        // When
        // the original content creator attempts to review the content
        collection.review(publisher1Session, uri, RECURSIVE);

        // Then
        // expect a Forbidden error
    }

    @Test
    public void review_shouldAllowSelfApproveForAutomatedCollection() throws Exception {

        // Given an automated collection
        collection.getDescription().setType(CollectionType.automated);
        // And an authorised user
        when(permissionsService.canSelfApprove(publisher1Session, CollectionType.automated))
                .thenReturn(true);
        String uri = CreateCompleteContent();

        // When the collection is reviewed
        boolean reviewed = collection.review(publisher1Session, uri, RECURSIVE);

        // Then the collection is successfully reviewed
        assertTrue(reviewed);
        Path reviewedPath = collection.getPath().resolve(Collection.REVIEWED);
        assertTrue(Files.exists(reviewedPath.resolve(uri.substring(1))));
    }

    @Test(expected = BadRequestException.class)
    public void review_shouldNotSelfApproveForManualCollection() throws Exception {

        // Given a non-automated collection
        collection.getDescription().setType(CollectionType.manual);
        // And a user with the self approve permission
        when(permissionsService.canSelfApprove(publisher1Session, CollectionType.manual))
                .thenReturn(true);
        String uri = CreateCompleteContent();

        // When the collection is reviewed
        collection.review(publisher1Session, uri, RECURSIVE);

        // Then
        // expect a BadRequestException
    }

    @Test(expected = BadRequestException.class)
    public void review_shouldNotSelfApproveForScheduledCollection() throws Exception {

        // Given a non-automated collection
        collection.getDescription().setType(CollectionType.scheduled);
        // And a user with the self approve permission
        when(permissionsService.canSelfApprove(publisher1Session, CollectionType.scheduled))
                .thenReturn(true);
        String uri = CreateCompleteContent();

        // When the collection is reviewed
        collection.review(publisher1Session, uri, RECURSIVE);

        // Then
        // expect a BadRequestException
    }

    @Test(expected = ForbiddenException.class)
    public void review_shouldNotReviewWhenEditPermissionDenied() throws Exception {

        // Given some content that has been edited and complete
        String uri = CreateCompleteContent();
        // And a user without edit permissions for the collection
        when(permissionsService.canEdit(eq(publisher2Session), any(CollectionType.class)))
                .thenReturn(false);

        // When the collection is reviewed
        collection.review(publisher2Session, uri, RECURSIVE);

        // Then
        // expect a ForbiddenException
    }

    @Test(expected = BadRequestException.class)
    public void review_shouldNotReviewIfContentHasNotBeenCompleted() throws IOException, ZebedeeException {

        // Given some content that has been edited by a publisher:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        createPublishedFile(uri);
        collection.edit(publisher1Session, uri, collectionWriter, RECURSIVE);

        // When - A reviewer edits reviews content
        collection.review(publisher2Session, uri, RECURSIVE);

        // Then
        // Expect an error
    }

    @Test
    public void complete_shouldComplete() throws IOException {

        // Given
        // The content exists, has been edited and complete:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        createPublishedFile(uri);
        createInProgressFile(uri);

        // When
        boolean complete = collection.complete(publisher1Session, uri, RECURSIVE);

        // Then
        assertTrue(complete);
        Path edited = collection.getPath().resolve(Collection.IN_PROGRESS);
        assertFalse(Files.exists(edited.resolve(uri.substring(1))));

        // check an event has been created for the content being created.
        collection.getDescription().getEventsByUri().get(uri).hasEventForType(EventType.COMPLETED);
    }

    @Test
    public void complete_shouldMoveFilesWithNoExtension() throws IOException {

        // Given
        // The content exists, has been edited and complete:
        String uri = "/economy/inflationandpriceindices/timeseries/fileWithNoExtension";
        createInProgressFile(uri);

        // When
        boolean complete = collection.complete(publisher1Session, uri, RECURSIVE);

        // Then
        assertTrue(complete);
        Path inProgressPath = collection.getPath().resolve(Collection.IN_PROGRESS);
        Path completedPath = collection.getPath().resolve(Collection.COMPLETE);
        assertFalse(Files.exists(inProgressPath.resolve(uri.substring(1))));
        assertTrue(Files.exists(completedPath.resolve(uri.substring(1))));

        // check an event has been created for the content being created.
        collection.getDescription().getEventsByUri().get(uri).hasEventForType(EventType.COMPLETED);
    }

    @Test
    public void complete_shouldNotCompleteIfReviewed() throws IOException {

        // Given
        // The content already exists:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        createReviewedFile(uri);

        // When
        boolean isComplete = collection.complete(publisher1Session, uri, RECURSIVE);

        // Then
        assertFalse(isComplete);
    }

    @Test
    public void complete_shouldNotCompleteIfAlreadyComplete() throws IOException {

        // Given
        // The content already exists:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        createCompleteFile(uri);

        // When
        boolean isComplete = collection.complete(publisher1Session, uri, RECURSIVE);

        // Then
        assertFalse(isComplete);
    }

    @Test
    public void complete_shouldNotCompleteIfNotEditing() throws IOException {

        // Given
        // The content already exists:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        createCompleteFile(uri);

        // When
        boolean isComplete = collection.complete(publisher1Session, uri, RECURSIVE);

        // Then
        assertFalse(isComplete);
    }

    @Test(expected = BadRequestException.class)
    public void review_shouldNotReviewIfAlreadyReviewed() throws IOException, ZebedeeException {

        // Given
        // The content already exists:
        String uri = CreateCompleteContent();
        createReviewedFile(uri);

        // When
        // An alternative publisher reviews the content
        collection.review(publisher2Session, uri, RECURSIVE);

        // Then
        // Expect error
    }

    @Test(expected = NotFoundException.class)
    public void review_shouldNotReviewIfNotPreviouslyCompleted() throws IOException, ZebedeeException {

        // Given
        // Some content:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        collection.edit(publisher1Session, uri, collectionWriter, RECURSIVE);

        // When content is trying to be reviewed before being completed
        boolean reviewed = collection.review(new Session(Random.id(), publisher1Email), uri, RECURSIVE);

        // Then the expected exception is thrown.
    }

    @Test
    public void isInProgress_correctlyDetectsInProgress() throws IOException {

        // Given
        // The content in inprogress, complete and reviewed
        String inProgressUri = "/economy/inflationandpriceindices/timeseries/inprogress.html";
        String completeUri = "/economy/inflationandpriceindices/timeseries/complete.html";
        String reviewedUri = "/economy/inflationandpriceindices/timeseries/reviewed.html";

        createInProgressFile(inProgressUri);
        createCompleteFile(completeUri);
        createReviewedFile(reviewedUri);

        // When
        // Checking if in progress
        boolean inProgress = collection.isInProgress(inProgressUri);
        boolean complete = collection.isInProgress(completeUri);
        boolean reviewed = collection.isInProgress(reviewedUri);

        // Then
        assertTrue(inProgress);
        assertFalse(complete);
        assertFalse(reviewed);
    }

    @Test
    public void isComplete_shouldCorrectlyDetectComplete() throws IOException {

        // Given
        // The content in inprogress, complete and reviewed
        String inProgressUri = "/economy/inflationandpriceindices/timeseries/inprogress.html";
        String completeUri = "/economy/inflationandpriceindices/timeseries/complete.html";
        String reviewedUri = "/economy/inflationandpriceindices/timeseries/reviewed.html";

        createInProgressFile(inProgressUri);
        createCompleteFile(completeUri);
        createReviewedFile(reviewedUri);

        // When
        // Checking if in progress
        boolean inProgress = collection.isComplete(inProgressUri);
        boolean complete = collection.isComplete(completeUri);
        boolean reviewed = collection.isComplete(reviewedUri);

        // Then
        assertFalse(inProgress);
        assertTrue(complete);
        assertFalse(reviewed);
    }

    @Test
    public void isReviewed_shouldCorrectlyDetectReviewed() throws IOException {

        // Given
        // The content in inprogress, complete and reviewed
        String inProgressUri = "/economy/inflationandpriceindices/timeseries/inprogress.html";
        String completeUri = "/economy/inflationandpriceindices/timeseries/complete.html";
        String reviewedUri = "/economy/inflationandpriceindices/timeseries/reviewed.html";

        createInProgressFile(inProgressUri);
        createCompleteFile(completeUri);
        createReviewedFile(reviewedUri);

        // When
        // Checking if in progress
        boolean inProgress = collection.isReviewed(inProgressUri);
        boolean complete = collection.isReviewed(completeUri);
        boolean reviewed = collection.isReviewed(reviewedUri);

        // Then
        assertFalse(inProgress);
        assertFalse(complete);
        assertTrue(reviewed);
    }

    @Test
    public void isInCollection_shouldDetectContentInAllStates() throws IOException {

        // Given
        // The content in inprogress, complete and reviewed
        String inProgressUri = "/economy/inflationandpriceindices/timeseries/inprogress.html";
        String completeUri = "/economy/inflationandpriceindices/timeseries/complete.html";
        String reviewedUri = "/economy/inflationandpriceindices/timeseries/reviewed.html";

        createInProgressFile(inProgressUri);
        createCompleteFile(completeUri);
        createReviewedFile(reviewedUri);

        // And
        // A content item that doesn't exist
        String doesNotExistUri = "/economy/inflationandpriceindices/timeseries/404.html";

        // When
        // Checking if in progress
        boolean inProgress = collection.isInCollection(inProgressUri);
        boolean complete = collection.isInCollection(completeUri);
        boolean reviewed = collection.isInCollection(reviewedUri);
        boolean doesNotExist = collection.isInCollection(doesNotExistUri);

        // Then
        assertTrue(inProgress);
        assertTrue(complete);
        assertTrue(reviewed);
        assertFalse(doesNotExist);
    }

    @Test
    public void getInProgressPath_shouldGetPath() throws IOException {

        // Given
        // We're editing some content:
        String uri = "/economy/inflationandpriceindices/timeseries/beer.html";
        createPublishedFile(uri);
        createReviewedFile(uri);
        createInProgressFile(uri);

        // When
        // We write some output to the content:
        Path path = collection.getInProgressPath(uri);
        try (Writer writer = Files.newBufferedWriter(path,
                StandardCharsets.UTF_8)) {
            writer.append("test");
        }

        // Then
        // The output should have gone to the expected copy of the file:
        Path inProgressPath = collection.getPath().resolve(
                Collection.IN_PROGRESS);
        Path expectedPath = inProgressPath.resolve(uri.substring(1));
        assertTrue(Files.size(expectedPath) > 0);
    }

    @Test
    public void shouldReturnInProgressUris() throws IOException {
        // Given
        // There are these files in progress:
        String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
        String uri2 = "/economy/someotherthing/timeseries/e4c4.html";
        createInProgressFile(uri);
        createInProgressFile(uri2);

        // When
        // We attempt to get the in progress files.
        List<String> uris = collection.inProgressUris();

        // Then
        // We get out the expected in progress files.
        assertTrue(uris.contains(uri));
        assertTrue(uris.contains(uri2));

        // and the uri lists for other states are empty.
        assertTrue(collection.completeUris().isEmpty());
        assertTrue(collection.reviewedUris().isEmpty());
    }

    @Test
    public void shouldReturnCompleteUris() throws IOException {
        // Given
        // There are these files complete:
        String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
        String uri2 = "/economy/someotherthing/timeseries/e4c4.html";
        createCompleteFile(uri);
        createCompleteFile(uri2);

        // When
        // We attempt to get the complete files.
        List<String> uris = collection.completeUris();

        // Then
        // We get out the expected complete files.
        assertTrue(uris.contains(uri));
        assertTrue(uris.contains(uri2));

        // and the uri lists for other states are empty.
        assertTrue(collection.inProgressUris().isEmpty());
        assertTrue(collection.reviewedUris().isEmpty());
    }

    @Test
    public void shouldReturnReviewedUris() throws IOException {
        // Given
        // There are these files reviewed:
        String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
        String uri2 = "/economy/someotherthing/timeseries/e4c4.html";
        createReviewedFile(uri);
        createReviewedFile(uri2);

        // When
        // We attempt to get the reviewed files.
        List<String> uris = collection.reviewedUris();

        // Then
        // We get out the expected reviewed files.
        assertTrue(uris.contains(uri));
        assertTrue(uris.contains(uri2));

        // and the uri lists for other states are empty.
        assertTrue(collection.inProgressUris().isEmpty());
        assertTrue(collection.completeUris().isEmpty());
    }

    @Test
    public void find_givenFilesExist_shouldFind() throws IOException {

        // Given
        // The content in inprogress, complete and reviewed
        String inProgressUri = "/economy/inflationandpriceindices/timeseries/inprogress.html";
        String completeUri = "/economy/inflationandpriceindices/timeseries/complete.html";
        String reviewedUri = "/economy/inflationandpriceindices/timeseries/reviewed.html";

        createInProgressFile(inProgressUri);
        createCompleteFile(completeUri);
        createReviewedFile(reviewedUri);

        // When
        // Try to find content
        Path inProgress = collection.find(inProgressUri);
        Path complete = collection.find(completeUri);
        Path reviewed = collection.find(reviewedUri);

        // Then
        // We get the path to the correct files
        assertTrue(inProgress.toString().endsWith("/" + Collection.IN_PROGRESS + inProgressUri));
        assertTrue(complete.toString().contains("/" + Collection.COMPLETE + completeUri));
        assertTrue(reviewed.toString().contains("/" + Collection.REVIEWED + reviewedUri));
    }

    @Test
    public void associateWithRelease_shouldUseExistingReleaseIfItsAlreadyInCollection()
            throws IOException, BadRequestException {

        // Given
        // There is a release already in progress
        String uri = String.format("/releases/%s", Random.id());
        Release release = createRelease(uri, new DateTime().plusWeeks(4).toDate());
        collection.edit(publisher1Session, uri + "/data.json", collectionWriter, RECURSIVE);

        // When we attempt to associate the collection with a release
        Release result = collection.associateWithRelease(publisher1Session, release, collectionWriter);

        assertTrue(result.getDescription().getPublished());
        assertEquals(URI.create(uri), result.getUri());
    }

    @Test
    public void associateWithRelease_shouldSetReleaseToPublished()
            throws IOException, BadRequestException {

        // Given a release that is announced
        String uri = String.format("/releases/%s", Random.id());
        Release release = createRelease(uri, new DateTime().plusWeeks(4).toDate());

        // When we attempt to associate the collection with a release
        Release result = collection.associateWithRelease(publisher1Session, release, collectionWriter);

        // Then the release is now in progress for the collection and the published flag
        // is set to true
        assertTrue(collection.isInProgress(uri));
        assertTrue(result.getDescription().getPublished());
        assertEquals(URI.create(uri), result.getUri());
    }

    @Test
    public void populateReleaseQuietly_shouldReturnNullWhenCollectionNotAssociatedToRelease()
            throws ZebedeeException, IOException {
        // Given a collection that is NOT associated with a release
        String releaseUri = "";
        collection.getDescription().setReleaseUri(releaseUri);

        // When we attempt to populate the release from the collection.

        DummyCollectionReader collectionReader = new DummyCollectionReader(collectionsPath, collection.getDescription().getId());
        DummyCollectionWriter collectionWriter = new DummyCollectionWriter(collectionsPath, collection.getDescription().getId());
        Iterable<ContentDetail> collectionContent = new ArrayList<>();

        Release result = collection.populateReleaseQuietly(
                collectionReader,
                collectionWriter,
                collectionContent);

        // Then the returned release object is null
        assertNull(result);
    }

    @Test
    public void populateReleaseQuietly_shouldReturnNullWhenReleaseJsonInvalid() throws ZebedeeException, IOException {
        // Given a collection that is associated with a release and has an article
        String uri = String.format("/releases/%s", Random.id());
        Release release = createRelease(uri, new DateTime().plusWeeks(4).toDate());

        CollectionDescription description = new CollectionDescription();
        description.setId(Random.id());
        description.setName(description.getId());

        collection.getDescription().setReleaseUri(uri);
        collection.associateWithRelease(publisher1Session, release, collectionWriter);

        String releaseJsonUri = uri + "/data.json";

        collection.complete(publisher1Session, releaseJsonUri, RECURSIVE);
        collection.review(publisher2Session, releaseJsonUri, RECURSIVE);

        FileUtils.write(collection.getReviewed().getPath().resolve(releaseJsonUri.substring(1)).toFile(),
                Serialiser.serialise(new Object()), Charset.defaultCharset());

        // When we attempt to populate the release from the collection.
        DummyCollectionReader collectionReader = new DummyCollectionReader(collection.getPath());
        DummyCollectionWriter collectionWriter = new DummyCollectionWriter(collection.getPath());
        Iterable<ContentDetail> collectionContent = ContentDetailUtil.resolveDetails(collection.getReviewed(),
                collectionReader.getReviewed());

        Release result = collection.populateReleaseQuietly(
                collectionReader,
                collectionWriter,
                collectionContent);

        // Then the returned release object is null
        assertNull(result);
    }

    @Test
    public void populateReleaseQuietly_shouldAddLinksToReleasePageForCollectionContent()
            throws ZebedeeException, IOException {
        // Given a collection that is associated with a release and has an article
        String uri = String.format("/releases/%s", Random.id());
        Release release = createRelease(uri, new DateTime().plusWeeks(4).toDate());

        CollectionDescription description = new CollectionDescription();
        description.setId(Random.id());
        description.setName(description.getId());

        collection.getDescription().setReleaseUri(uri);
        collection.associateWithRelease(publisher1Session, release, collectionWriter);

        String releaseJsonUri = uri + "/data.json";

        collection.complete(publisher1Session, releaseJsonUri, RECURSIVE);
        collection.review(publisher2Session, releaseJsonUri, RECURSIVE);

        ContentDetail articleDetail = new ContentDetail("My article", "/some/uri", PageType.ARTICLE);
        FileUtils.write(collection.getReviewed().getPath().resolve("some/uri/data.json").toFile(),
                Serialiser.serialise(articleDetail), Charset.defaultCharset());

        // When we attempt to populate the release from the collection.

        DummyCollectionReader collectionReader = new DummyCollectionReader(collectionsPath, collection.getDescription().getId());
        DummyCollectionWriter collectionWriter = new DummyCollectionWriter(collectionsPath, collection.getDescription().getId());
        Iterable<ContentDetail> collectionContent = ContentDetailUtil.resolveDetails(collection.getReviewed(),
                collectionReader.getReviewed());

        Release result = collection.populateReleaseQuietly(
                collectionReader,
                collectionWriter,
                collectionContent);

        // Then the release is now in progress for the collection and the published flag
        // is set to true
        assertNotNull(result);
        assertEquals(1, result.getRelatedDocuments().size());
        assertEquals("My article", result.getRelatedDocuments().get(0).getTitle());
        assertEquals("/some/uri", result.getRelatedDocuments().get(0).getUri().toString());
    }

    @Test
    public void populateReleaseQuietly_shouldAddLinksToReleasePageForCollectionContentCMD()
            throws ZebedeeException, IOException {
        // Given a collection that is associated with a release and has a CMD dataset
        String uri = String.format("/releases/%s", Random.id());
        Release release = createRelease(uri, new DateTime().plusWeeks(4).toDate());

        CollectionDescription description = new CollectionDescription();
        description.setId(Random.id());
        description.setName(description.getId());

        collection.getDescription().setReleaseUri(uri);
        collection.associateWithRelease(publisher1Session, release, collectionWriter);

        String releaseJsonUri = uri + "/data.json";

        collection.complete(publisher1Session, releaseJsonUri, RECURSIVE);
        collection.review(publisher2Session, releaseJsonUri, RECURSIVE);

        ContentDetail cmdDetail = new ContentDetail("My CMD dataset", "/some/uri", PageType.API_DATASET_LANDING_PAGE);
        FileUtils.write(collection.getReviewed().getPath().resolve("some/uri/data.json").toFile(),
                Serialiser.serialise(cmdDetail), Charset.defaultCharset());

        // When we attempt to populate the release from the collection.
        DummyCollectionReader collectionReader = new DummyCollectionReader(collectionsPath, collection.getDescription().getId());
        DummyCollectionWriter collectionWriter = new DummyCollectionWriter(collectionsPath, collection.getDescription().getId());
        Iterable<ContentDetail> collectionContent = ContentDetailUtil.resolveDetails(collection.getReviewed(),
                collectionReader.getReviewed());

        Release result = collection.populateReleaseQuietly(
                collectionReader,
                collectionWriter,
                collectionContent);

        // Then the release is populated with a link to the associated CMD dataset
        assertNotNull(result);
        assertEquals(1, result.getRelatedAPIDatasets().size());
        assertEquals(cmdDetail.description.title, result.getRelatedAPIDatasets().get(0).getTitle());
        assertEquals(cmdDetail.uri, result.getRelatedAPIDatasets().get(0).getUri().toString());
    }

    @Test
    public void create_ShouldAssociateWithReleaseIfReleaseUriIsPresent() throws Exception {
        // Given an existing release page
        String uri = String.format("/releases/%s", Random.id());
        Release release = createRelease(uri, new DateTime().plusWeeks(4).toDate());

        // When a new collection is created with the release uri given
        CollectionDescription collectionDescription = new CollectionDescription(Random.id());
        collectionDescription.setReleaseUri(release.getUri().toString());
        collectionDescription.setType(CollectionType.scheduled);

        zebedeeCollectionWriterMock = Mockito.mockConstruction(ZebedeeCollectionWriter.class, (mock, context) -> {
            DummyCollectionWriter dummyWriter = new DummyCollectionWriter(collectionsPath, collectionDescription.getId());
            when(mock.getInProgress()).thenReturn(dummyWriter.getInProgress());
        });

        Collection actual = Collection.create(collectionDescription, zebedee, publisher1Session);

        // The release page is in progress within the collection and the collection
        // publish date has been
        // taken from the release page date.
        assertTrue(actual.isInProgress(uri));
        assertEquals(release.getDescription().getReleaseDate(), actual.getDescription().getPublishDate());
    }

    @Test(expected = BadRequestException.class)
    public void create_ShouldThrowExceptionIfReleaseDateIsNull() throws Exception {

        // Given an existing release page with a null release date
        String uri = String.format("/releases/%s", Random.id());
        Release release = createRelease(uri, null);

        // When a new collection is created with the release uri given
        CollectionDescription collectionDescription = new CollectionDescription(Random.id());
        collectionDescription.setReleaseUri(release.getUri().toString());

        Collection.create(collectionDescription, zebedee, publisher1Session);

        // Then the expected exception is thrown
    }

    @Test(expected = ConflictException.class)
    public void create_shouldThrowExceptionIfReleaseIsInAnotherCollection() throws Exception {
        // Given an existing release page which is associated with an existing
        // collection
        String uri = String.format("/releases/%s", Random.id());
        Release release = createRelease(uri, new DateTime().plusWeeks(4).toDate());

        when(zebedee.isBeingEdited(eq(uri + "/data.json"))).thenReturn(1);

        // When a new collection is created with the same release uri given
        CollectionDescription collectionDescription = new CollectionDescription(Random.id());
        collectionDescription.setReleaseUri(release.getUri().toString());

        Collection.create(collectionDescription, zebedee, publisher1Session);

        // Then the expected exception is thrown
    }

    @Test(expected = NotFoundException.class)
    public void version_shouldThrowNotFoundIfContentIsNotPublished() throws Exception {

        // Given a URI that has not been published / does not exist.
        String uri = String.format("/economy/inflationandpriceindices/timeseries/%s", Random.id());

        // When we attempt to create a version for the page
        collection.version(publisher1Email, uri, collectionWriter);

        // Then a not found exception is thrown.
    }

    @Test(expected = ConflictException.class)
    public void version_shouldNotCreateASecondVersionForAURI() throws Exception {

        // Given a URI that has been published and already versioned in a collection.
        String uri = String.format("/economy/inflationandpriceindices/timeseries/%s", Random.id());
        createPublishedFile(uri + "/data.json");
        collection.version(publisher1Email, uri, collectionWriter);

        // When we attempt to create a version for the page for a second time
        collection.version(publisher1Email, uri, collectionWriter);

        // Then a ConflictException exception is thrown.
    }

    @Test
    public void version_shouldCreateVersionForUri() throws Exception {

        // Given an existing uri that has been publised.
        String uri = String.format("/economy/inflationandpriceindices/timeseries/%s", Random.id());
        createPublishedFile(uri + "/data.json");

        // When the version function is called for the URI
        ContentItemVersion version = collection.version(publisher1Email, uri, collectionWriter);

        // Then the version directory is created, with the page and associated files
        // copied into it
        // check versions file exists
        Path versionsDirectoryPath = collection.getReviewed()
                .get(Paths.get(uri).resolve(VersionedContentItem.getVersionDirectoryName()).toUri());
        assertTrue(Files.exists(versionsDirectoryPath));

        // check the json file is in there
        assertTrue(Files.exists(collection.getReviewed().get(version.getUri())));

        // check for an associated file
        assertTrue(Files
                .exists(collection.getReviewed().get(Paths.get(version.getUri()).resolve("data.json").toString())));
    }

    @Test(expected = NotFoundException.class)
    public void deleteVersion_shouldThrowNotFoundIfVersionDoesNotExistInCollection() throws Exception {

        // Given a collection and a URI of a version that does not exist in the
        // collection.
        String uri = String.format("/economy/inflationandpriceindices/timeseries/%s/previous/v1", Random.id());

        // When we attempt to delete a version
        collection.deleteVersion("", uri);

        // Then a not found exception is thrown.
    }

    @Test(expected = BadRequestException.class)
    public void deleteVersion_shouldThrowBadRequestIfNotAValidVersionUri() throws Exception {

        // Given a collection and a URI that is not a version.
        String uri = String.format("/economy/inflationandpriceindices/timeseries/%s", Random.id());

        // When we attempt to delete a version
        collection.deleteVersion("", uri);

        // Then a BadRequestException is thrown.
    }

    @Test
    public void deleteVersion_shouldDeleteVersionDirectory() throws Exception {

        // Given an existing version URI
        String uri = String.format("/economy/inflationandpriceindices/timeseries/%s", Random.id());
        createPublishedFile(uri + "/data.json");
        ContentItemVersion version = collection.version(publisher1Email, uri, collectionWriter);

        assertTrue(Files.exists(collection.getReviewed().get(version.getUri())));
        assertTrue(Files.exists(collection.getReviewed().get(version.getUri()).resolve("data.json")));

        // When the delete version function is called for the version URI
        collection.deleteVersion("bob", version.getUri());

        // Then the versions directory is deleted.
        assertNull(collection.getReviewed().get(version.getUri()));
    }

    private Release createRelease(String uri, Date releaseDate) throws IOException {
        String trimmedUri = StringUtils.removeStart(uri, "/");
        Release release = new Release();
        release.setDescription(new PageDescription());
        release.getDescription().setPublished(false);
        release.getDescription().setReleaseDate(releaseDate);
        release.setUri(URI.create(uri));
        String content = ContentUtil.serialise(release);

        Path releasePath = publishedPath.resolve(trimmedUri + "/data.json");
        FileUtils.write(releasePath.toFile(), content);
        return release;
    }

    @Test
    public void moveContent_shouldRenameInprogressFile() throws Exception {

        // Given the content already exists:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        String toUri = "/economy/inflationandpriceindices/timeseries/a9errenamed.html";
        createInProgressFile(uri);

        setUpKeyringMocks();

        zebedeeCollectionReaderMock = Mockito.mockConstruction(ZebedeeCollectionReader.class, (mock, context) -> {
            DummyCollectionReader dummyReader = new DummyCollectionReader(collectionsPath, collection.getId());
            when(mock.getInProgress()).thenReturn(dummyReader.getInProgress());
        });

        zebedeeCollectionWriterMock = Mockito.mockConstruction(ZebedeeCollectionWriter.class, (mock, context) -> {
            DummyCollectionWriter dummyWriter = new DummyCollectionWriter(collectionsPath, collection.getId());
            when(mock.getInProgress()).thenReturn(dummyWriter.getInProgress());
        });

        // When we move content
        boolean edited = collection.moveContent(publisher1Session, uri, toUri);

        // Then the file should exist only in the new location.
        assertTrue(edited);
        Path inProgress = collection.getPath().resolve(Collection.IN_PROGRESS);
        assertFalse(Files.exists(inProgress.resolve(uri.substring(1))));
        assertTrue(Files.exists(inProgress.resolve(toUri.substring(1))));

        // check an event has been created for the content being created.
        assertTrue(collection.getDescription().getEventsByUri().get(uri).hasEventForType(EventType.MOVED));
    }

    @Test
    public void moveContent_shouldRenameCompletedFiles() throws Exception {

        // Given the content already exists:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        String toUri = "/economy/inflationandpriceindices/timeseries/a9errenamed.html";
        createCompleteFile(uri);

        setUpKeyringMocks();

        zebedeeCollectionReaderMock = Mockito.mockConstruction(ZebedeeCollectionReader.class, (mock, context) -> {
            DummyCollectionReader dummyReader = new DummyCollectionReader(collectionsPath, collection.getId());
            when(mock.getInProgress()).thenReturn(dummyReader.getInProgress());
        });

        zebedeeCollectionWriterMock = Mockito.mockConstruction(ZebedeeCollectionWriter.class, (mock, context) -> {
            DummyCollectionWriter dummyWriter = new DummyCollectionWriter(collectionsPath, collection.getId());
            when(mock.getInProgress()).thenReturn(dummyWriter.getInProgress());
        });

        // When we move content
        boolean edited = collection.moveContent(publisher1Session, uri, toUri);

        // Then the file should exist only in the new location.
        assertTrue(edited);
        Path complete = collection.getPath().resolve(Collection.COMPLETE);
        assertFalse(Files.exists(complete.resolve(uri.substring(1))));
        assertTrue(Files.exists(complete.resolve(toUri.substring(1))));

        // check an event has been created for the content being created.
        assertTrue(collection.getDescription().getEventsByUri().get(uri).hasEventForType(EventType.MOVED));
    }

    @Test
    public void moveContent_shouldOverwriteExistingFiles() throws Exception {

        // Given some existing content in progress.
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        String toUri = "/economy/inflationandpriceindices/timeseries/a9errenamed.html";
        createInProgressFile(uri);
        createInProgressFile(toUri);

        setUpKeyringMocks();

        zebedeeCollectionReaderMock = Mockito.mockConstruction(ZebedeeCollectionReader.class, (mock, context) -> {
            DummyCollectionReader dummyReader = new DummyCollectionReader(collectionsPath, collection.getId());
            when(mock.getInProgress()).thenReturn(dummyReader.getInProgress());
        });

        zebedeeCollectionWriterMock = Mockito.mockConstruction(ZebedeeCollectionWriter.class, (mock, context) -> {
            DummyCollectionWriter dummyWriter = new DummyCollectionWriter(collectionsPath, collection.getId());
            when(mock.getInProgress()).thenReturn(dummyWriter.getInProgress());
        });

        // When we move content to a URI where some content already exists.
        boolean edited = collection.moveContent(publisher1Session, uri, toUri);

        // Then the existing content should be overwritten.
        assertTrue(edited);
        Path inProgress = collection.getPath().resolve(Collection.IN_PROGRESS);
        assertFalse(Files.exists(inProgress.resolve(uri.substring(1))));
        assertTrue(Files.exists(inProgress.resolve(toUri.substring(1))));

        // check an event has been created for the content being created.
        assertTrue(collection.getDescription().getEventsByUri().get(uri).hasEventForType(EventType.MOVED));
    }

    @Test
    public void isAllContentReviewed_shouldReturnTrueWhenEmpty() throws IOException, ZebedeeException {

        // Given an empty collection
        Path collectionPath = Files.createTempDirectory(Random.id()); // create a temp directory to generate content
                                                                      // into
        Collection collection = createCollection(collectionPath, "isAllContentReviewed", zebedee);

        // When isAllContentReviewed() is called
        boolean allContentReviewed = collection.isAllContentReviewed(false);

        // Then the result is true
        assertTrue(allContentReviewed);
    }

    @Test
    public void isAllContentReviewed_shouldReturnFalseWhenFileInProgress() throws IOException, ZebedeeException {

        // Given a collection with a file in progress
        Path collectionPath = Files.createTempDirectory(Random.id()); // create a temp directory to generate content
                                                                      // into
        Collection collection = spy(
                CollectionTest.createCollection(collectionPath, "isAllContentReviewed", zebedee));

        ArrayList<String> uriList = new ArrayList<>(Arrays.asList("/some/uri"));
        doReturn(uriList).when(collection).inProgressUris();

        // When isAllContentReviewed() is called
        boolean allContentReviewed = collection.isAllContentReviewed(false);

        // Then the result is false
        assertFalse(allContentReviewed);
    }

    @Test
    public void isAllContentReviewed_shouldReturnFalseWhenFileComplete() throws IOException, ZebedeeException {

        // Given a collection with a file in complete
        Path collectionPath = Files.createTempDirectory(Random.id()); // create a temp directory to generate content
                                                                      // into
        Collection collection = spy(
                CollectionTest.createCollection(collectionPath, "isAllContentReviewed", zebedee));

        ArrayList<String> uriList = new ArrayList<>(Arrays.asList("/some/uri"));
        doReturn(uriList).when(collection).completeUris();

        // When isAllContentReviewed() is called
        boolean allContentReviewed = collection.isAllContentReviewed(false);

        // Then the result is false
        assertFalse(allContentReviewed);
    }

    @Test
    public void isAllContentReviewed_shouldReturnFalseWhenDatasetNotReviewed() throws IOException, ZebedeeException {
        // Given a collection with a dataset that has not been set to reviewed.
        Path collectionPath = Files.createTempDirectory(Random.id()); // create a temp directory to generate content
                                                                      // into
        Collection testCollection = CollectionTest.createCollection(collectionPath, "isAllContentReviewed", zebedee);

        CollectionDataset dataset = new CollectionDataset();
        dataset.setState(ContentStatus.Complete);
        testCollection.getDescription().addDataset(dataset);

        // When isAllContentReviewed() is called
        boolean allContentReviewed = testCollection.isAllContentReviewed(true);

        // Then the result is false
        assertFalse(allContentReviewed);
    }

    @Test
    public void isAllContentReviewed_shouldReturnFalseWhenDatasetVersionNotReviewed()
            throws IOException, ZebedeeException {
        // Given a collection with a dataset version that has not been set to reviewed.
        Path collectionPath = Files.createTempDirectory(Random.id()); // create a temp directory to generate content
                                                                      // into
        Collection testCollection = CollectionTest.createCollection(collectionPath, "isAllContentReviewed", zebedee);

        CollectionDatasetVersion datasetVersion = new CollectionDatasetVersion();
        datasetVersion.setState(ContentStatus.Complete);
        testCollection.getDescription().addDatasetVersion(datasetVersion);

        // When isAllContentReviewed() is called
        boolean allContentReviewed = testCollection.isAllContentReviewed(true);

        // Then the result is false
        assertFalse(allContentReviewed);
    }

    @Test
    public void isAllContentReviewed_shouldReturnTrueWhenDatasetIsReviewed() throws IOException, ZebedeeException {

        // Given a collection with a dataset that has been set to reviewed.
        Path collectionPath = Files.createTempDirectory(Random.id()); // create a temp directory to generate content
                                                                      // into
        Collection collection = CollectionTest.createCollection(collectionPath, "isAllContentReviewed", zebedee);

        CollectionDataset dataset = new CollectionDataset();
        dataset.setState(ContentStatus.Reviewed);
        collection.getDescription().addDataset(dataset);

        // When isAllContentReviewed() is called
        boolean allContentReviewed = collection.isAllContentReviewed(false);

        // Then the result is true
        assertTrue(allContentReviewed);
    }

    @Test
    public void isAllContentReviewed_shouldReturnTrueWhenDatasetVersionIsReviewed()
            throws IOException, ZebedeeException {

        // Given a collection with a dataset version that has been set to reviewed.
        Path collectionPath = Files.createTempDirectory(Random.id()); // create a temp directory to generate content
                                                                      // into
        Collection collection = CollectionTest.createCollection(collectionPath, "isAllContentReviewed", zebedee);

        CollectionDatasetVersion datasetVersion = new CollectionDatasetVersion();
        datasetVersion.setState(ContentStatus.Reviewed);
        collection.getDescription().addDatasetVersion(datasetVersion);

        // When isAllContentReviewed() is called
        boolean allContentReviewed = collection.isAllContentReviewed(false);

        // Then the result is true
        assertTrue(allContentReviewed);
    }

    @Test
    public void getDatasetDetails_shouldReturnCorrectDetails() throws IOException, ZebedeeException {

        // Given a collection with a dataset.
        Path collectionPath = Files.createTempDirectory(Random.id()); // create a temp directory to generate content
                                                                      // into
        Collection collection = CollectionTest.createCollection(collectionPath, "isAllContentReviewed", zebedee);

        CollectionDataset dataset = new CollectionDataset();
        dataset.setUri("http://localhost:1234/datasets/123");
        dataset.setTitle("dataset wut");
        collection.getDescription().addDataset(dataset);

        // When getDatasetDetails() is called
        List<ContentDetail> datasetContent = collection.getDatasetDetails();

        // Then the expected values have been set
        ContentDetail datasetDetail = datasetContent.get(0);

        assertEquals("/datasets/123", datasetDetail.uri);
        assertEquals(PageType.API_DATASET_LANDING_PAGE, datasetDetail.getType());
        assertEquals(dataset.getTitle(), datasetDetail.description.title);
    }

    @Test
    public void getDatasetVersionDetails_shouldReturnCorrectDetails() throws IOException, ZebedeeException {

        // Given a collection with a dataset version.
        Path collectionPath = Files.createTempDirectory(Random.id()); // create a temp directory to generate content
                                                                      // into
        Collection collection = CollectionTest.createCollection(collectionPath, "isAllContentReviewed", zebedee);

        CollectionDatasetVersion datasetVersion = new CollectionDatasetVersion();
        datasetVersion.setId("123");
        datasetVersion.setEdition("2015");
        datasetVersion.setVersion("1");
        datasetVersion.setTitle("dataset version wut");
        collection.getDescription().addDatasetVersion(datasetVersion);

        // When getDatasetVersionDetails() is called
        List<ContentDetail> datasetContent = collection.getDatasetVersionDetails();

        // Then the expected values have been set
        ContentDetail versionDetail = datasetContent.get(0);
        assertEquals("/datasets/123/editions/2015/versions/1", versionDetail.uri);
        assertEquals(PageType.API_DATASET_LANDING_PAGE, versionDetail.getType());
        assertEquals(datasetVersion.getTitle(), versionDetail.description.title);
    }


    @Test
    public void shouldChangeReferencesInFileOnMoveContent() throws IOException, ZebedeeException, URISyntaxException {
        zebedeeCollectionReaderMock = Mockito.mockConstruction(ZebedeeCollectionReader.class, (mock, context) -> {
            DummyCollectionReader dummyReader = new DummyCollectionReader(collectionsPath, collection.getId());
            when(mock.getInProgress()).thenReturn(dummyReader.getInProgress());
        });

        zebedeeCollectionWriterMock = Mockito.mockConstruction(ZebedeeCollectionWriter.class, (mock, context) -> {
            DummyCollectionWriter dummyWriter = new DummyCollectionWriter(collectionsPath, collection.getId());
            when(mock.getInProgress()).thenReturn(dummyWriter.getInProgress());
        });

        // Given
        // an item of content that references something
        createTestArticles();
        martin.getRelatedArticles().add(new Link(bedford.getUri()));
        savePages();

        // When
        // we run the move
        collection.moveContent(publisher1Session, "/places/bedford", "/places/london");

        // Then
        // the link should be updated;
        martin = (Article) readPageFromCollection(collection, martin.getUri().toString());
        assertEquals(1, martin.getRelatedArticles().size());
        assertEquals("/places/london", martin.getRelatedArticles().get(0).getUri().toString());
    }

    @Test
    public void shouldNotChangeExtendedReferencesInFileOnMoveContent() throws URISyntaxException, IOException, ZebedeeException {
        zebedeeCollectionReaderMock = Mockito.mockConstruction(ZebedeeCollectionReader.class, (mock, context) -> {
            DummyCollectionReader dummyReader = new DummyCollectionReader(collectionsPath, collection.getId());
            when(mock.getInProgress()).thenReturn(dummyReader.getInProgress());
        });

        zebedeeCollectionWriterMock = Mockito.mockConstruction(ZebedeeCollectionWriter.class, (mock, context) -> {
            DummyCollectionWriter dummyWriter = new DummyCollectionWriter(collectionsPath, collection.getId());
            when(mock.getInProgress()).thenReturn(dummyWriter.getInProgress());
        });

        // Given
        // an item of content that references something
        createTestArticles();
        martin.getRelatedArticles().add(new Link(new URI("/places/bedfordshire")));
        savePages();

        // When
        // we run the move
        collection.moveContent(publisher1Session, "/places/bedford", "/places/london");

        // Then
        // the link should not be updated;
        martin = (Article) readPageFromCollection(collection, martin.getUri().toString());
        assertEquals(1, martin.getRelatedArticles().size());
        assertEquals("/places/bedfordshire", martin.getRelatedArticles().get(0).getUri().toString());
    }

    @Test
    public void shouldChangeSubReferencesInFileOnMoveContent() throws URISyntaxException, IOException, ZebedeeException {
        zebedeeCollectionReaderMock = Mockito.mockConstruction(ZebedeeCollectionReader.class, (mock, context) -> {
            DummyCollectionReader dummyReader = new DummyCollectionReader(collectionsPath, collection.getId());
            when(mock.getInProgress()).thenReturn(dummyReader.getInProgress());
        });

        zebedeeCollectionWriterMock = Mockito.mockConstruction(ZebedeeCollectionWriter.class, (mock, context) -> {
            DummyCollectionWriter dummyWriter = new DummyCollectionWriter(collectionsPath, collection.getId());
            when(mock.getInProgress()).thenReturn(dummyWriter.getInProgress());
        });

        // Given
        // an item of content that references a sub page
        createTestArticles();
        martin.getRelatedArticles().add(new Link(new URI("/places/bedford/central")));
        savePages();

        // When
        // we run the move on the upper
        collection.moveContent(publisher1Session, "/places/bedford", "/places/london");

        // Then
        // the link should be updated on the lower level
        martin = (Article) readPageFromCollection(collection, martin.getUri().toString());
        assertEquals(1, martin.getRelatedArticles().size());
        assertEquals("/places/london/central", martin.getRelatedArticles().get(0).getUri().toString());
    }

    public static Collection createCollection(Path rootCollectionsPath, String collectionName, Zebedee zebedee)
            throws CollectionNotFoundException, IOException {

        CollectionDescription collectionDescription = new CollectionDescription(collectionName);
        collectionDescription.setType(CollectionType.manual);
        collectionDescription.setEncrypted(false);
        collectionDescription.setName(collectionName);

        String filename = toFilename(collectionName);
        collectionDescription.setId(filename + "-" + Random.id());
        Collection.CreateCollectionFolders(filename, rootCollectionsPath);

        // Create the description:
        Path collectionDescriptionPath = rootCollectionsPath.resolve(filename + ".json");
        try (OutputStream output = Files.newOutputStream(collectionDescriptionPath)) {
            Serialiser.serialise(output, collectionDescription);
        }

        return new Collection(rootCollectionsPath.resolve(filename), zebedee);
    }

    private void setUpKeyringMocks() throws Exception {
        SecretKey key = Keys.newSecretKey();
        when(encryptionKeyFactory.newCollectionKey())
                .thenReturn(key);

        when(collectionKeyring.get(any(), any()))
                .thenReturn(key);
    }

    private void verifyKeyAddedToCollectionKeyring() throws Exception {
        verify(collectionKeyring, times(1)).add(any(), any(), any());
    }

    /**
     * Creates a published file.
     *
     * @param uri The URI to be created.
     * @throws IOException If a filesystem error occurs.
     */
    private void createPublishedFile(String uri) throws IOException {
        Path content = publishedPath.resolve(uri.substring(1));
        Files.createDirectories(content.getParent());
        Files.createFile(content);
    }

    /**
     * Creates a reviewed file.
     *
     * @param uri The URI to be created.
     * @throws IOException If a filesystem error occurs.
     */
    private void createReviewedFile(String uri) throws IOException {

        createFile(Collection.REVIEWED, uri);
    }

    /**
     * Creates a complete file.
     *
     * @param uri The URI to be created.
     * @throws IOException If a filesystem error occurs.
     */
    private void createCompleteFile(String uri) throws IOException {

        createFile(Collection.COMPLETE, uri);
    }

    /**
     * Creates an in progress file.
     *
     * @param uri The URI to be created.
     * @throws IOException If a filesystem error occurs.
     */
    private void createInProgressFile(String uri) throws IOException {

        createFile(Collection.IN_PROGRESS, uri);
    }

    /**
     * Creates a file in the given directory.
     *
     * @param directory The directory to be created.
     * @param uri       The URI to be created.
     * @throws IOException If a filesystem error occurs.
     */
    private void createFile(String directory, String uri) throws IOException {

        Path inProgress = collection.getPath().resolve(directory);
        Path content = inProgress.resolve(uri.substring(1));
        Files.createDirectories(content.getParent());
        Files.createFile(content);
    }

    /**
     * Creates an reviewed file in a different {@link com.github.onsdigital.zebedee.model.Collection}.
     *
     * @param uri        The URI to be created.
     * @throws IOException If a filesystem error occurs.
     */
    private void isBeingEditedElsewhere(String uri) throws IOException {
        Path reviewed = collectionsPath.resolve(Random.id() + "/" + Collection.REVIEWED);
        Path content = reviewed.resolve(uri.substring(1));
        Files.createDirectories(content.getParent());
        Files.createFile(content);
    }

    private String CreateEditedContent() throws IOException, BadRequestException {
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        createPublishedFile(uri);
        collection.edit(publisher1Session, uri, collectionWriter, RECURSIVE);
        return uri;
    }

    private String CreateCompleteContent() throws IOException, BadRequestException {
        String uri = CreateEditedContent();
        collection.complete(publisher1Session, uri, RECURSIVE);
        return uri;
    }

    private void createTestArticles() throws URISyntaxException, IOException {
        martin = createArticle("/people/martin", "Martin");
        bedford = createArticle("/places/bedford", "Bedford");
        bedfordshire = createArticle("/places/bedfordshire", "Bedfordshire");

        savePages();
    }

    private Article createArticle(String uri, String title) throws URISyntaxException {
        Article article = new Article();
        article.setDescription(new PageDescription());
        article.setRelatedArticles(new ArrayList<Link>());
        article.setSections(new ArrayList<MarkdownSection>());
        article.setUri(new URI(uri));
        article.getDescription().setTitle(title);
        return article;
    }

    private void writePageToContent(Content content, Page page) throws IOException {
        Path path = content.toPath(page.getUri().toString()).resolve("data.json");
        path.toFile().getParentFile().mkdirs();

        Files.write(path, ContentUtil.serialise(page).getBytes());
    }

    private void savePages() throws IOException {
        writePageToContent(collection.getInProgress(), martin);
        writePageToContent(collection.getInProgress(), bedford);
        writePageToContent(collection.getInProgress(), bedfordshire);
    }

    private Page readPageFromCollection(Collection collection, String uri) throws IOException {
        Path path = collection.find(uri).resolve("data.json");
        Page page = null;
        try (InputStream stream = Files.newInputStream(path)) {
            page = ContentUtil.deserialiseContent(stream);
        }
        return page;
    }
}
