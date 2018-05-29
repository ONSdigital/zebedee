package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.KeyManangerUtil;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.data.json.DirectoryListing;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ApprovalStatus;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.json.Event;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.model.approval.ApproveTask;
import com.github.onsdigital.zebedee.model.publishing.PublishNotification;
import com.github.onsdigital.zebedee.persistence.CollectionEventType;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDao;
import com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.apache.commons.fileupload.FileUploadException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_DELETED;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_UNLOCKED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Created by dave on 19/05/2017.
 */
public class CollectionsTest {

    private static final String COLLECTION_FILE_NAME = "{0}-{1}.json";
    private static final String TEST_EMAIL = "TEST@ons.gov.uk";

    @Rule
    public TemporaryFolder rootDir = new TemporaryFolder();

    @Mock
    private PermissionsService permissionsServiceMock;

    @Mock
    private Zebedee zebedeeMock;

    @Mock
    private Session sessionMock;

    @Mock
    private UsersService usersServiceMock;

    @Mock
    private Collection collectionMock;

    @Mock
    private CollectionDescription collectionDescriptionMock;

    @Mock
    private CollectionHistoryDao collectionHistoryDaoMock;

    @Mock
    public KeyManangerUtil keyManagerUtilMock;

    @Mock
    private Content publishedContentMock;

    @Mock
    private HttpServletRequest requestMock;

    @Mock
    private CollectionReaderWriterFactory collectionReaderWriterFactoryMock;

    @Mock
    private CollectionWriter collectionWriterMock;

    @Mock
    private CollectionReader collectionReaderMock;

    @Mock
    private PublishNotification publishNotification;

    @Mock
    private ContentWriter contentWriterMock;

    @Mock
    private ContentReader contentReaderMock;

    @Mock
    private Future<Boolean> futureMock;

    private Collections collections;
    private Path collectionsPath;
    private User testUser;
    private Supplier<Zebedee> zebedeeSupplier;
    private BiConsumer<Collection, EventType> publishingNotificationConsumer;
    private Supplier<CollectionHistoryDao> collectionHistoryDaoSupplier;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        Collection.setKeyManagerUtil(keyManagerUtilMock);

        System.setProperty("audit_db_enabled", "false");
        testUser = new User();
        testUser.setEmail(TEST_EMAIL);
        rootDir.create();
        collectionsPath = rootDir.newFolder("collections").toPath();

        // Test target.
        collections = new Collections(collectionsPath, permissionsServiceMock, publishedContentMock);

        zebedeeSupplier = () -> zebedeeMock;
        publishingNotificationConsumer = (c, e) -> publishNotification.sendNotification(e);
        collectionHistoryDaoSupplier = () -> collectionHistoryDaoMock;

        when(sessionMock.getEmail())
                .thenReturn(TEST_EMAIL);

        when(collectionMock.getDescription())
                .thenReturn(collectionDescriptionMock);

        ReflectionTestUtils.setField(collections, "zebedeeSupplier", zebedeeSupplier);
        ReflectionTestUtils.setField(collections, "collectionReaderWriterFactory", collectionReaderWriterFactoryMock);
        ReflectionTestUtils.setField(collections, "publishingNotificationConsumer", publishingNotificationConsumer);
        ReflectionTestUtils.setField(collections, "collectionHistoryDaoSupplier", collectionHistoryDaoSupplier);
    }

    @Test
    public void shouldFindCollection() throws Exception {
        CollectionDescription desc = new CollectionDescription();
        desc.setName("test");
        desc.setType(CollectionType.manual);

        when(zebedeeMock.getCollections())
                .thenReturn(collections);
        when(zebedeeMock.getUsersService())
                .thenReturn(usersServiceMock);
        when(usersServiceMock.getUserByEmail(anyString()))
                .thenReturn(testUser);
        when(collectionReaderWriterFactoryMock.getWriter(zebedeeMock, collectionMock, sessionMock))
                .thenReturn(collectionWriterMock);

        Collection created = Collection.create(desc, zebedeeMock, sessionMock);
        Collection found = collections.getCollection(created.getDescription().getId());

        assertThat(created.getDescription().getId(), equalTo(found.getDescription().getId()));
        assertThat(created.getDescription().getName(), equalTo(found.getDescription().getName()));
        assertThat(created.getDescription().getType(), equalTo(found.getDescription().getType()));
    }

    @Test
    public void shouldReturnNullIfNotFound() throws Exception {
        assertThat(collections.getCollection("A_Girl_Is_No_One"), equalTo(null));
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForNullCollectionOnApprove() throws IOException, ZebedeeException {
        collections.approve(null, sessionMock);
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForNullCollectionOnListDirectory() throws IOException, UnauthorizedException,
            BadRequestException, ConflictException, NotFoundException {
        collections.listDirectory(null, "somefile.json", sessionMock);
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForNullCollectionOnComplete() throws IOException, ZebedeeException {
        collections.complete(null, "someURI", sessionMock, false);
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForNullCollectionOnDelete() throws IOException, ZebedeeException {
        collections.delete(null, sessionMock);
    }

    @Test(expected = NotFoundException.class)
    public void shouldThrowNotFoundForNullCollectionOnWriteContent() throws IOException, ZebedeeException,
            FileUploadException {
        HttpServletRequest request = null;
        InputStream inputStream = null;

        when(collectionReaderWriterFactoryMock.getWriter(zebedeeMock, null, sessionMock))
                .thenThrow(new NotFoundException(""));

        collections.writeContent(null, "someURI", sessionMock, request,
                inputStream, false, CollectionEventType.COLLECTION_PAGE_SAVED, false);
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForNullCollectionOnDeleteContent() throws IOException, ZebedeeException {
        collections.deleteContent(null, "someURI", sessionMock);
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForNullCollectionOnMoveContent() throws IOException, ZebedeeException {
        collections.moveContent(sessionMock, null, "to", "from");
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForBlankUriOnMoveContent() throws IOException, ZebedeeException {

        when(permissionsServiceMock.canEdit(TEST_EMAIL))
                .thenReturn(true);

        try {
            collections.moveContent(sessionMock, collectionMock, "", "toURI");
        } catch (BadRequestException e) {
            verify(permissionsServiceMock, times(1))
                    .canEdit(TEST_EMAIL);
            verify(sessionMock, times(1))
                    .getEmail();
            verifyZeroInteractions(collectionMock, publishedContentMock);
            throw e;
        }
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForBlankToUriOnMoveContent() throws IOException, ZebedeeException {
        when(permissionsServiceMock.canEdit(TEST_EMAIL))
                .thenReturn(true);

        try {
            collections.moveContent(sessionMock, collectionMock, "fromURI", "");
        } catch (BadRequestException e) {
            verify(permissionsServiceMock, times(1))
                    .canEdit(TEST_EMAIL);
            verify(sessionMock, times(1))
                    .getEmail();
            verifyZeroInteractions(collectionMock, publishedContentMock);
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldThrowUnauthorizedIfNotLoggedInOnApprove() throws IOException, ZebedeeException {
        when(permissionsServiceMock.canEdit(TEST_EMAIL))
                .thenReturn(false);
        try {
            collections.approve(collectionMock, sessionMock);
        } catch (UnauthorizedException e) {
            verify(permissionsServiceMock, times(1))
                    .canEdit(TEST_EMAIL);
            verify(sessionMock, times(1))
                    .getEmail();
            verifyZeroInteractions(collectionMock, publishedContentMock, zebedeeMock);
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldThrowUnauthorizedIfNotLoggedInOnListDirectory() throws IOException, UnauthorizedException,
            BadRequestException,
            ConflictException, NotFoundException {
        when(permissionsServiceMock.canEdit(TEST_EMAIL))
                .thenReturn(false);

        try {
            collections.listDirectory(collectionMock, "someURI", sessionMock);
        } catch (UnauthorizedException e) {
            verify(permissionsServiceMock, times(1))
                    .canView(sessionMock, collectionDescriptionMock);
            verify(collectionMock, times(1))
                    .getDescription();
            verify(collectionMock, never())
                    .find(any());
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldThrowUnauthorizedIfNotLoggedInOnComplete() throws IOException, ZebedeeException {
        when(permissionsServiceMock.canEdit(sessionMock))
                .thenReturn(false);

        try {
            collections.complete(collectionMock, "someURI", sessionMock, false);
        } catch (UnauthorizedException e) {
            verify(permissionsServiceMock, times(1)).canEdit(sessionMock);
            verifyZeroInteractions(collectionMock);
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldThrowUnauthorizedIfNotLoggedInOnDelete() throws IOException, ZebedeeException {
        when(permissionsServiceMock.canEdit(sessionMock))
                .thenReturn(false);
        try {
            collections.delete(collectionMock, sessionMock);
        } catch (UnauthorizedException e) {
            verify(permissionsServiceMock, times(1)).canEdit(sessionMock);
            verifyZeroInteractions(collectionMock);
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldThrowUnauthorizedIfNotLoggedInOnWriteContent() throws IOException, ZebedeeException,
            FileUploadException {
        InputStream inputStreamMock = mock(InputStream.class);
        when(collectionReaderWriterFactoryMock.getWriter(zebedeeMock, collectionMock, sessionMock))
                .thenThrow(new UnauthorizedException(""));
        try {
            collections.writeContent(collectionMock, "someURI", sessionMock, requestMock,
                    inputStreamMock, false, CollectionEventType.COLLECTION_PAGE_SAVED, true);
        } catch (UnauthorizedException e) {
            verify(collectionReaderWriterFactoryMock, times(1)).getWriter(zebedeeMock, collectionMock, sessionMock);
            verifyZeroInteractions(collectionMock);
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldThrowUnauthorizedIfNotLoggedInOnDeleteContent() throws IOException, ZebedeeException {
        try {
            when(permissionsServiceMock.canEdit(TEST_EMAIL))
                    .thenReturn(false);
            collections.deleteContent(collectionMock, "someURI", sessionMock);
        } catch (UnauthorizedException e) {
            verify(permissionsServiceMock, times(1)).canEdit(TEST_EMAIL);
            verifyZeroInteractions(collectionMock);
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldThrowUnauthorizedIfNotLoggedInOnMoveContent() throws IOException, ZebedeeException {
        when(permissionsServiceMock.canEdit(TEST_EMAIL))
                .thenReturn(false);
        try {
            collections.moveContent(sessionMock, collectionMock, "from", "to");
        } catch (UnauthorizedException e) {
            verify(permissionsServiceMock, times(1)).canEdit(TEST_EMAIL);
            verifyZeroInteractions(collectionMock, publishedContentMock);
            throw e;
        }
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestIfNoUriOnWriteContent() throws IOException, ZebedeeException, FileUploadException {
        InputStream inputStreamMock = mock(InputStream.class);

        when(collectionReaderWriterFactoryMock.getWriter(zebedeeMock, collectionMock, sessionMock))
                .thenReturn(collectionWriterMock);
        when(collectionDescriptionMock.getName())
                .thenReturn("AGirlIsNoOne");
        when(collectionDescriptionMock.getApprovalStatus())
                .thenReturn(ApprovalStatus.IN_PROGRESS);

        try {
            collections.writeContent(collectionMock, null, sessionMock, requestMock,
                    inputStreamMock, false, CollectionEventType.COLLECTION_PAGE_SAVED, true);
        } catch (BadRequestException e) {
            verify(collectionReaderWriterFactoryMock, times(1)).getWriter(zebedeeMock, collectionMock, sessionMock);
            verify(sessionMock, times(1)).getEmail();
            verify(collectionMock, times(2)).getDescription();
            verify(collectionDescriptionMock, times(1)).getName();
            verifyZeroInteractions(permissionsServiceMock);
            verify(collectionMock, never()).find(anyString());
            verify(collectionMock, never()).create(anyString(), anyString());
            verify(collectionMock, never()).edit(anyString(), anyString(), eq(collectionWriterMock), anyBoolean());
            throw e;
        }
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestIfNoUriOnDeleteContent() throws IOException, ZebedeeException {
        when(permissionsServiceMock.canEdit(TEST_EMAIL))
                .thenReturn(true);
        try {
            collections.deleteContent(collectionMock, null, sessionMock);
        } catch (BadRequestException e) {
            verify(permissionsServiceMock, times(1)).canEdit(TEST_EMAIL);
            verifyZeroInteractions(collectionMock, collectionDescriptionMock, zebedeeMock);
            throw e;
        }
    }

    @Test(expected = NotFoundException.class)
    public void shouldThrowNotFoundIfUriNotInProgressOnComplete() throws IOException, ZebedeeException {
        Content inProg = mock(Content.class);

        when(permissionsServiceMock.canEdit(sessionMock))
                .thenReturn(true);
        when(collectionMock.getInProgress())
                .thenReturn(inProg);
        try {
            collections.complete(collectionMock, "someURI", sessionMock, false);
        } catch (NotFoundException e) {
            verify(permissionsServiceMock, times(1)).canEdit(sessionMock);
            verify(collectionMock, times(1)).getInProgress();
            verify(inProg, times(1)).get(anyString());
            verify(collectionMock, never()).complete(anyString(), anyString(), anyBoolean());
            verify(collectionMock, never()).save();
            throw e;
        }
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestIfUriIsADirectoryOnComplete() throws IOException, ZebedeeException {
        String uri = "someURI";
        Path p = rootDir.newFolder("test").toPath();
        Content inProg = mock(Content.class);

        when(permissionsServiceMock.canEdit(sessionMock))
                .thenReturn(true);
        when(collectionMock.getInProgress())
                .thenReturn(inProg);
        when(inProg.get(uri))
                .thenReturn(p);

        try {
            collections.complete(collectionMock, uri, sessionMock, false);
        } catch (NotFoundException e) {
            verify(permissionsServiceMock, times(1)).canEdit(sessionMock);
            verify(collectionMock, times(1)).getInProgress();
            verify(inProg, times(1)).get(uri);
            verify(collectionMock, never()).complete(anyString(), anyString(), anyBoolean());
            verify(collectionMock, never()).save();
            throw e;
        }
    }

    @Test
    public void shouldCompleteContent() throws IOException, ZebedeeException {
        Path p = rootDir.newFile("data.json").toPath();
        Content inProg = mock(Content.class);

        when(permissionsServiceMock.canEdit(sessionMock))
                .thenReturn(true);
        when(collectionMock.getInProgress())
                .thenReturn(inProg);
        when(inProg.get(p.toString()))
                .thenReturn(p);
        ;
        when(collectionDescriptionMock.getName())
                .thenReturn("AGirlIsNoOne");
        when(collectionDescriptionMock.getId())
                .thenReturn("1234567890");
        when(collectionMock.complete(TEST_EMAIL, p.toString(), false))
                .thenReturn(true);

        collections.complete(collectionMock, p.toString(), sessionMock, false);

        verify(permissionsServiceMock, times(1)).canEdit(sessionMock);
        verify(collectionMock, times(1)).getInProgress();
        verify(inProg, times(1)).get(p.toString());
        verify(collectionMock, times(1)).complete(TEST_EMAIL, p.toString(), false);
        verify(collectionMock, times(1)).save();
        verify(collectionHistoryDaoMock, times(1)).saveCollectionHistoryEvent(any(CollectionHistoryEvent.class));
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowErrorIfCompleteUnsucessful() throws IOException, ZebedeeException {
        Path p = rootDir.newFile("data.json").toPath();
        Content inProg = mock(Content.class);

        when(permissionsServiceMock.canEdit(sessionMock))
                .thenReturn(true);
        when(collectionMock.getInProgress())
                .thenReturn(inProg);
        when(inProg.get(p.toString()))
                .thenReturn(p);
        when(collectionDescriptionMock.getName())
                .thenReturn("AGirlIsNoOne");
        when(collectionDescriptionMock.getId())
                .thenReturn("1234567890");
        when(collectionMock.complete(TEST_EMAIL, p.toString(), false))
                .thenReturn(false);

        try {
            collections.complete(collectionMock, p.toString(), sessionMock, false);
        } catch (BadRequestException e) {
            verify(permissionsServiceMock, times(1)).canEdit(sessionMock);
            verify(collectionMock, times(1)).getInProgress();
            verify(inProg, times(1)).get(p.toString());
            verify(collectionMock, times(1)).complete(TEST_EMAIL, p.toString(), false);
            verify(collectionMock, never()).save();
            throw e;
        }
    }

    @Test(expected = ConflictException.class)
    public void shouldNotApproveIfAUriIsInProgress() throws IOException, ZebedeeException {
        List<String> inProgressURIS = new ArrayList();
        inProgressURIS.add("data.json");

        when(permissionsServiceMock.canEdit(TEST_EMAIL))
                .thenReturn(true);
        when(collectionMock.inProgressUris())
                .thenReturn(inProgressURIS);
        when(collectionMock.completeUris())
                .thenReturn(new ArrayList<String>());

        try {
            collections.approve(collectionMock, sessionMock);
        } catch (ConflictException e) {
            verify(permissionsServiceMock, times(1)).canEdit(TEST_EMAIL);
            verify(sessionMock, times(1)).getEmail();
            verify(collectionMock, times(1)).inProgressUris();
            verify(collectionReaderWriterFactoryMock, never()).getReader(any(), any(), any());
            verify(collectionReaderWriterFactoryMock, never()).getWriter(any(), any(), any());
            verify(collectionHistoryDaoMock, never()).saveCollectionHistoryEvent(any(CollectionHistoryEvent.class));
            throw e;
        }
    }

    @Test(expected = ConflictException.class)
    public void shouldNotApproveIfAUriIsComplete() throws IOException, ZebedeeException {
        List<String> completeURIS = new ArrayList();
        completeURIS.add("data.json");

        when(permissionsServiceMock.canEdit(TEST_EMAIL))
                .thenReturn(true);
        when(collectionMock.inProgressUris())
                .thenReturn(new ArrayList<String>());
        when(collectionMock.completeUris())
                .thenReturn(completeURIS);

        try {
            collections.approve(collectionMock, sessionMock);
        } catch (ConflictException e) {
            verify(permissionsServiceMock, times(1)).canEdit(TEST_EMAIL);
            verify(sessionMock, times(1)).getEmail();
            verify(collectionMock, times(1)).inProgressUris();
            verify(collectionMock, times(1)).completeUris();
            verify(collectionReaderWriterFactoryMock, never()).getReader(any(), any(), any());
            verify(collectionReaderWriterFactoryMock, never()).getWriter(any(), any(), any());
            throw e;
        }
    }

    @Test
    public void shouldApproveCollection() throws IOException, ZebedeeException, ExecutionException,
            InterruptedException {
        Function<Path, ContentReader> contentReaderFunction = (p) -> contentReaderMock;
        Function<ApproveTask, Future<Boolean>> addTaskToQueue = (t) -> futureMock;

        ReflectionTestUtils.setField(collections, "contentReaderFactory", contentReaderFunction);
        ReflectionTestUtils.setField(collections, "addTaskToQueue", addTaskToQueue);

        when(permissionsServiceMock.canEdit(TEST_EMAIL))
                .thenReturn(true);
        when(collectionMock.inProgressUris())
                .thenReturn(new ArrayList<String>());
        when(collectionMock.completeUris())
                .thenReturn(new ArrayList<String>());
        when(collectionReaderWriterFactoryMock.getReader(zebedeeMock, collectionMock, sessionMock))
                .thenReturn(collectionReaderMock);
        when(collectionReaderWriterFactoryMock.getWriter(zebedeeMock, collectionMock, sessionMock))
                .thenReturn(collectionWriterMock);

        assertThat(futureMock, equalTo(collections.approve(collectionMock, sessionMock)));

        verify(permissionsServiceMock, times(1)).canEdit(TEST_EMAIL);
        verify(collectionMock, times(1)).inProgressUris();
        verify(collectionMock, times(1)).completeUris();
        verify(collectionReaderWriterFactoryMock, times(1)).getReader(zebedeeMock, collectionMock, sessionMock);
        verify(collectionReaderWriterFactoryMock, times(1)).getWriter(zebedeeMock, collectionMock, sessionMock);
        verify(collectionHistoryDaoMock, times(1)).saveCollectionHistoryEvent(any(), any(), any());
    }

    @Test
    public void shouldUnlockCollection() throws IOException, ZebedeeException, ExecutionException, InterruptedException {
        when(permissionsServiceMock.canEdit(TEST_EMAIL))
                .thenReturn(true);
        when(collectionDescriptionMock.getApprovalStatus())
                .thenReturn(ApprovalStatus.COMPLETE);
        when(collectionMock.save())
                .thenReturn(true);

        boolean result = collections.unlock(collectionMock, sessionMock);

        assertThat(result, is(true));
        verify(permissionsServiceMock, times(1)).canEdit(TEST_EMAIL);
        verify(collectionMock, times(3)).getDescription();
        verify(collectionDescriptionMock, times(1)).getApprovalStatus();
        verify(collectionDescriptionMock, times(1)).setApprovalStatus(ApprovalStatus.NOT_STARTED);
        verify(collectionDescriptionMock, times(1)).addEvent(any(Event.class));
        verify(publishNotification, times(1)).sendNotification(EventType.UNLOCKED);
        verify(collectionMock, times(1)).save();
        verify(collectionHistoryDaoMock, times(1)).saveCollectionHistoryEvent(collectionMock, sessionMock,
                COLLECTION_UNLOCKED);
    }

    @Test
    public void shouldUnlockWithoutAddingEventIfAlreadyUnlocked() throws Exception {
        when(permissionsServiceMock.canEdit(TEST_EMAIL))
                .thenReturn(true);
        when(collectionDescriptionMock.getApprovalStatus())
                .thenReturn(ApprovalStatus.IN_PROGRESS);
        when(collectionMock.save())
                .thenReturn(true);

        boolean result = collections.unlock(collectionMock, sessionMock);

        assertThat(result, is(true));
        verify(permissionsServiceMock, times(1)).canEdit(TEST_EMAIL);
        verify(collectionMock, times(1)).getDescription();
        verify(collectionDescriptionMock, times(1)).getApprovalStatus();
        verify(collectionDescriptionMock, never()).setApprovalStatus(any(ApprovalStatus.class));
        verify(collectionDescriptionMock, never()).addEvent(any(Event.class));
        verify(publishNotification, never()).sendNotification(any(EventType.class));
        verify(collectionMock, never()).save();
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldThrowUnauthorizedIfNotLoggedInOnUnlock() throws Exception {
        when(permissionsServiceMock.canEdit(TEST_EMAIL))
                .thenReturn(false);
        try {
            collections.unlock(collectionMock, sessionMock);
        } catch (UnauthorizedException e) {
            verify(permissionsServiceMock, times(1)).canEdit(TEST_EMAIL);
            verify(collectionMock, never()).getDescription();
            verify(collectionDescriptionMock, never()).getApprovalStatus();
            verify(collectionDescriptionMock, never()).setApprovalStatus(any(ApprovalStatus.class));
            verify(collectionDescriptionMock, never()).addEvent(any(Event.class));
            verify(publishNotification, never()).sendNotification(any(EventType.class));
            verify(collectionMock, never()).save();
            throw e;
        }
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForNullCollectionOnUnlock() throws IOException, ZebedeeException {
        try {
            collections.unlock(null, sessionMock);
        } catch (UnauthorizedException e) {
            verify(permissionsServiceMock, never()).canEdit(TEST_EMAIL);
            verify(collectionMock, never()).getDescription();
            verify(collectionDescriptionMock, never()).getApprovalStatus();
            verify(collectionDescriptionMock, never()).setApprovalStatus(any(ApprovalStatus.class));
            verify(collectionDescriptionMock, never()).addEvent(any(Event.class));
            verify(publishNotification, never()).sendNotification(any(EventType.class));
            verify(collectionMock, never()).save();
            throw e;
        }
    }

    @Test(expected = NotFoundException.class)
    public void shouldGetNotFoundIfAttemptingToListNonexistentDirectory() throws IOException, UnauthorizedException,
            BadRequestException, ConflictException, NotFoundException {
        String uri = "someURI";
        when(permissionsServiceMock.canView(sessionMock, collectionDescriptionMock))
                .thenReturn(true);
        when(collectionMock.find(uri))
                .thenReturn(null);
        try {
            collections.listDirectory(collectionMock, uri, sessionMock);
        } catch (NotFoundException e) {
            verify(permissionsServiceMock, times(1)).canView(sessionMock, collectionDescriptionMock);
            verify(collectionMock, times(1)).find(uri);
            throw e;
        }
    }

    @Test(expected = BadRequestException.class)
    public void shouldGetBadRequestIfAttemptingToListDirectoryOnAFile() throws IOException, UnauthorizedException,
            BadRequestException, ConflictException, NotFoundException {
        Path uri = rootDir.newFile("data.json").toPath();
        when(permissionsServiceMock.canView(sessionMock, collectionDescriptionMock))
                .thenReturn(true);
        when(collectionMock.find(uri.toString()))
                .thenReturn(uri);
        try {
            collections.listDirectory(collectionMock, uri.toString(), sessionMock);
        } catch (BadRequestException e) {
            verify(permissionsServiceMock, times(1)).canView(sessionMock, collectionDescriptionMock);
            verify(collectionMock, times(1)).find(uri.toString());
            throw e;
        }
    }

    @Test
    public void shouldListDirectory() throws IOException, UnauthorizedException, BadRequestException, ConflictException,
            NotFoundException {
        Path file1 = rootDir.newFile("data.json").toPath();
        Path file2 = rootDir.newFile("chart.png").toPath();

        DirectoryListing expected = new DirectoryListing();
        expected.getFiles().put(file1.getFileName().toString(), file1.toString());
        expected.getFiles().put(file2.getFileName().toString(), file2.toString());
        expected.getFolders().put(collectionsPath.getFileName().toString(), collectionsPath.toString());

        when(permissionsServiceMock.canView(sessionMock, collectionDescriptionMock))
                .thenReturn(true);
        when(collectionMock.find(rootDir.getRoot().toString()))
                .thenReturn(rootDir.getRoot().toPath());

        DirectoryListing result = collections.listDirectory(collectionMock, rootDir.getRoot().toString(), sessionMock);

        assertThat(result, equalTo(expected));
        verify(permissionsServiceMock, times(1)).canView(sessionMock, collectionDescriptionMock);
        verify(collectionMock, times(1)).find(rootDir.getRoot().toString());
    }

    @Test
    public void shouldListDirectoryOverlayed() throws IOException, ZebedeeException {
        Path uri = rootDir.getRoot().toPath().resolve("economy");
        uri.toFile().mkdir();

        File f1 = uri.resolve("data.json").toFile();
        File f2 = uri.resolve("data1.json").toFile();
        f1.createNewFile();
        f2.createNewFile();

        DirectoryListing expected = new DirectoryListing();
        expected.getFiles().put(f1.getName(), f1.toPath().toString());
        expected.getFiles().put(f2.getName(), f2.toPath().toString());

        when(permissionsServiceMock.canView(sessionMock, collectionDescriptionMock))
                .thenReturn(true);
        when(collectionMock.find(uri.toString()))
                .thenReturn(uri);
        when(publishedContentMock.get(uri.toString()))
                .thenReturn(uri);

        DirectoryListing result = collections.listDirectoryOverlayed(collectionMock, uri.toString(), sessionMock);

        assertThat(result, equalTo(expected));
        verify(permissionsServiceMock, times(1)).canView(sessionMock, collectionDescriptionMock);
        verify(collectionMock, times(1)).find(uri.toString());
    }

    @Test(expected = BadRequestException.class)
    public void shouldNotDeleteCollectionIfNotEmpty() throws IOException, ZebedeeException {
        when(permissionsServiceMock.canEdit(sessionMock))
                .thenReturn(true);
        when(collectionMock.isEmpty())
                .thenReturn(false);
        try {
            collections.delete(collectionMock, sessionMock);
        } catch (BadRequestException e) {
            verify(permissionsServiceMock, times(1)).canEdit(sessionMock);
            verify(collectionMock, times(1)).isEmpty();
            verify(collectionMock, never()).delete();
            throw e;
        }
    }

    @Test
    public void shouldDeleteCollection() throws IOException, ZebedeeException {
        when(permissionsServiceMock.canEdit(sessionMock))
                .thenReturn(true);
        when(collectionMock.isEmpty())
                .thenReturn(true);

        collections.delete(collectionMock, sessionMock);

        verify(permissionsServiceMock, times(1)).canEdit(sessionMock);
        verify(collectionMock, times(1)).isEmpty();
        verify(collectionMock, times(1)).delete();
        verify(collectionHistoryDaoMock, times(1)).saveCollectionHistoryEvent(eq(collectionMock), eq(sessionMock),
                eq(COLLECTION_DELETED));
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForWritingADirectoryAsAFile() throws IOException, ZebedeeException,
            FileUploadException {
        Path uri = rootDir.newFolder("test").toPath();

        when(collectionReaderWriterFactoryMock.getWriter(zebedeeMock, collectionMock, sessionMock))
                .thenReturn(collectionWriterMock);
        when(collectionDescriptionMock.getApprovalStatus())
                .thenReturn(ApprovalStatus.IN_PROGRESS);
        when(collectionMock.find(uri.toString()))
                .thenReturn(uri);
        try {
            collections.writeContent(collectionMock, uri.toString(), sessionMock, requestMock, mock(InputStream.class), false,
                    CollectionEventType.COLLECTION_PAGE_SAVED, false);
        } catch (BadRequestException e) {
            verify(collectionReaderWriterFactoryMock, times(1)).getWriter(zebedeeMock, collectionMock, sessionMock);
            verify(collectionDescriptionMock, times(1)).getApprovalStatus();
            verify(collectionMock, times(1)).find(uri.toString());
            verify(collectionMock, never()).save();
            verify(collectionMock, never()).edit(anyString(), anyString(), any(), anyBoolean());
            verify(collectionMock, never()).create(anyString(), anyString());
            verify(collectionMock, never()).getInProgressPath(anyString());
            verify(collectionWriterMock, never()).getInProgress();
            throw e;
        }
    }


    @Test(expected = ConflictException.class)
    public void shouldThrowConflictForCreatingFileBeingEditedElsewhere() throws IOException, ZebedeeException,
            FileUploadException {
        Path uri = rootDir.newFile("data.json").toPath();

        when(collectionReaderWriterFactoryMock.getWriter(zebedeeMock, collectionMock, sessionMock))
                .thenReturn(collectionWriterMock);
        when(collectionDescriptionMock.getApprovalStatus())
                .thenReturn(ApprovalStatus.IN_PROGRESS);
        when(collectionMock.find(uri.toString()))
                .thenReturn(null);
        when(collectionMock.edit(TEST_EMAIL, uri.toString(), collectionWriterMock, false))
                .thenReturn(false);
        try {
            collections.writeContent(collectionMock, uri.toString(), sessionMock, requestMock, mock(InputStream.class), false,
                    CollectionEventType.COLLECTION_PAGE_SAVED, false);
        } catch (BadRequestException e) {
            verify(collectionReaderWriterFactoryMock, times(1)).getWriter(zebedeeMock, collectionMock, sessionMock);
            verify(collectionDescriptionMock, times(1)).getApprovalStatus();
            verify(collectionMock, times(1)).find(uri.toString());
            verify(collectionMock, times(1)).edit(TEST_EMAIL, uri.toString(), collectionWriterMock, false);
            verify(collectionMock, never()).save();
            verify(collectionMock, never()).create(anyString(), anyString());
            verify(collectionMock, never()).edit(anyString(), anyString(), any(), anyBoolean());
            verify(collectionMock, never()).getInProgressPath(anyString());
            verify(collectionWriterMock, never()).getInProgress();
            throw e;
        }
    }

    @Test(expected = ConflictException.class)
    public void shouldThrowConflictForEditingFileBeingEditedElsewhere() throws IOException, ZebedeeException,
            FileUploadException {
        Path uri = rootDir.newFile("data.json").toPath();

        when(collectionReaderWriterFactoryMock.getWriter(zebedeeMock, collectionMock, sessionMock))
                .thenReturn(collectionWriterMock);
        when(collectionDescriptionMock.getApprovalStatus())
                .thenReturn(ApprovalStatus.IN_PROGRESS);
        when(collectionMock.find(uri.toString()))
                .thenReturn(uri);
        when(collectionMock.edit(TEST_EMAIL, uri.toString(), collectionWriterMock, false))
                .thenReturn(false);
        try {
            collections.writeContent(collectionMock, uri.toString(), sessionMock, requestMock, mock(InputStream.class), false,
                    CollectionEventType.COLLECTION_PAGE_SAVED, false);
        } catch (BadRequestException e) {
            verify(collectionReaderWriterFactoryMock, times(1)).getWriter(zebedeeMock, collectionMock, sessionMock);
            verify(collectionDescriptionMock, times(1)).getApprovalStatus();
            verify(collectionMock, times(1)).find(uri.toString());
            verify(collectionMock, times(1)).edit(TEST_EMAIL, uri.toString(), collectionWriterMock, false);
            verify(collectionMock, never()).save();
            verify(collectionMock, never()).create(anyString(), anyString());
            verify(collectionMock, never()).edit(anyString(), anyString(), any(), anyBoolean());
            verify(collectionMock, never()).getInProgressPath(anyString());
            verify(collectionWriterMock, never()).getInProgress();
            throw e;
        }
    }

    @Test
    public void shouldWriteContent() throws IOException, ZebedeeException, FileUploadException {
        Path uri = rootDir.newFile("data.json").toPath();
        InputStream in = mock(InputStream.class);

        when(collectionReaderWriterFactoryMock.getWriter(zebedeeMock, collectionMock, sessionMock))
                .thenReturn(collectionWriterMock);
        when(collectionDescriptionMock.getApprovalStatus())
                .thenReturn(ApprovalStatus.IN_PROGRESS);
        when(collectionMock.find(uri.toString()))
                .thenReturn(uri);
        when(collectionMock.edit(TEST_EMAIL, uri.toString(), collectionWriterMock, false))
                .thenReturn(true);
        when(collectionMock.getInProgressPath(uri.toString()))
                .thenReturn(uri);
        when(collectionWriterMock.getInProgress())
                .thenReturn(contentWriterMock);

        collections.writeContent(collectionMock, uri.toString(), sessionMock, requestMock, in, false,
                CollectionEventType.COLLECTION_PAGE_SAVED, false);

        verify(collectionReaderWriterFactoryMock, times(1)).getWriter(zebedeeMock, collectionMock, sessionMock);
        verify(collectionDescriptionMock, times(1)).getApprovalStatus();
        verify(collectionMock, times(1)).find(uri.toString());
        verify(collectionMock, times(1)).edit(TEST_EMAIL, uri.toString(), collectionWriterMock, false);
        verify(collectionMock, times(1)).save();
        verify(collectionMock, never()).create(anyString(), anyString());
        verify(collectionMock, times(1)).getInProgressPath(uri.toString());
        verify(collectionWriterMock, times(1)).getInProgress();
        verify(contentWriterMock, times(1)).write(in, uri.toString());
        verify(collectionHistoryDaoMock, times(1)).saveCollectionHistoryEvent(any(CollectionHistoryEvent.class));
    }

    @Test(expected = NotFoundException.class)
    public void shouldThrowNotFoundForDeletingNonexistentFile() throws IOException, ZebedeeException {
        String uri = "someURI";
        when(permissionsServiceMock.canEdit(TEST_EMAIL))
                .thenReturn(true);
        try {
            collections.deleteContent(collectionMock, uri, sessionMock);
        } catch (NotFoundException e) {
            verify(permissionsServiceMock, times(1)).canEdit(TEST_EMAIL);
            verify(collectionMock, times(1)).find(uri);
            verify(collectionMock, never()).isInCollection(anyString());
            verify(collectionMock, never()).getDescription();
            /*verify(collectionMock, never()).deleteDataVisContent(any(), any());*/
            verify(collectionMock, never()).deleteContentDirectory(anyString(), anyString());
            verify(collectionMock, never()).deleteFile(anyString());
            verify(collectionMock, never()).save();
            throw e;
        }
    }

    @Test
    public void shouldDeleteFile() throws IOException, ZebedeeException {
        Path uri = rootDir.newFile("data.json").toPath();

        when(permissionsServiceMock.canEdit(TEST_EMAIL))
                .thenReturn(true);
        when(collectionMock.find(uri.toString()))
                .thenReturn(uri);
        when(collectionMock.isInCollection(uri.toString()))
                .thenReturn(true);
        when(collectionMock.deleteFile(uri.toString()))
                .thenReturn(true);


        assertThat(collections.deleteContent(collectionMock, uri.toString(), sessionMock), is(true));

        verify(permissionsServiceMock, times(1)).canEdit(TEST_EMAIL);
        verify(collectionMock, times(1)).find(uri.toString());
        verify(collectionMock, times(1)).isInCollection(uri.toString());
        verify(collectionMock, times(2)).getDescription();
        verify(collectionMock, never()).deleteContentDirectory(any(), any());
        verify(collectionMock, never()).deleteContentDirectory(anyString(), anyString());
        verify(collectionMock, times(1)).deleteFile(uri.toString());
        verify(collectionMock, times(1)).save();
        verify(collectionHistoryDaoMock, times(1)).saveCollectionHistoryEvent(any(CollectionHistoryEvent.class));
    }

    @Test
    public void shouldDeleteFolderRecursively() throws IOException, ZebedeeException {
        Path uri = collectionsPath.resolve("inprogress");
        uri.toFile().mkdir();
        uri = uri.resolve("test");
        uri.toFile().mkdir();

        when(permissionsServiceMock.canEdit(TEST_EMAIL))
                .thenReturn(true);
        when(collectionMock.find(uri.toString()))
                .thenReturn(uri);
        when(collectionMock.isInCollection(uri.toString()))
                .thenReturn(true);
        when(collectionMock.deleteContentDirectory(TEST_EMAIL, uri.toString()))
                .thenReturn(true);

        assertThat(collections.deleteContent(collectionMock, uri.toString(), sessionMock), is(true));
        assertThat(uri.toFile().exists(), is(false));
        assertThat(uri.getParent().toFile().exists(), is(true));
        verify(permissionsServiceMock, times(1)).canEdit(TEST_EMAIL);
        verify(collectionMock, times(1)).find(uri.toString());
        verify(collectionMock, times(1)).isInCollection(uri.toString());
        verify(collectionMock, times(2)).getDescription();
        /*verify(collectionMock, never()).deleteContentDirectory(any(), any());*/
        verify(collectionMock, times(1)).deleteContentDirectory(TEST_EMAIL, uri.toString());
        verify(collectionMock, never()).deleteFile(uri.toString());
        verify(collectionMock, times(1)).save();
        verify(collectionHistoryDaoMock, times(1)).saveCollectionHistoryEvent(any(CollectionHistoryEvent.class));
    }

    @Test(expected = ConflictException.class)
    public void shouldThrowConflictExceptionOnCreateContentIfAlreadyPublished() throws Exception {
        String uri = "someURI";
        when(publishedContentMock.exists(uri))
                .thenReturn(true);
        try {
            collections.createContent(collectionMock, uri, sessionMock, null, null, null, false);
        } catch (ConflictException e) {
            verify(publishedContentMock, times(1)).exists(uri);
            verifyZeroInteractions(zebedeeMock, collectionReaderWriterFactoryMock, collectionDescriptionMock,
                    collectionWriterMock, collectionMock);
            throw e;
        }
    }

    @Test(expected = ConflictException.class)
    public void shouldThrowConflictExceptionOnCreateContentIfAlreadyInCollection() throws Exception {
        String uri = "someURI";
        Collection blocker = mock(Collection.class);

        when(publishedContentMock.exists(uri))
                .thenReturn(false);
        when(zebedeeMock.checkForCollectionBlockingChange(collectionMock, uri))
                .thenReturn(Optional.of(blocker));
        when(zebedeeMock.checkForCollectionBlockingChange(uri))
                .thenReturn(Optional.of(blocker));
        when(blocker.getDescription())
                .thenReturn(collectionDescriptionMock);
        when(collectionDescriptionMock.getName())
                .thenReturn("Bob")
                .thenReturn("Steve");
        try {
            collections.createContent(collectionMock, uri, sessionMock, null, null, null, false);
        } catch (ConflictException e) {
            verify(publishedContentMock, times(1)).exists(uri);
            verify(zebedeeMock, times(1)).checkForCollectionBlockingChange(collectionMock, uri);
            verify(collectionMock, times(1)).getDescription();
            verify(blocker, times(1)).getDescription();
            verify(collectionDescriptionMock, times(2)).getName();
            verifyNoMoreInteractions(zebedeeMock);
            verifyZeroInteractions(collectionReaderWriterFactoryMock, collectionWriterMock, collectionMock);
            throw e;
        }
    }
}
