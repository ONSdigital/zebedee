package com.github.onsdigital.zebedee.model.approval;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.json.ApprovalStatus;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.Event;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.json.PendingDelete;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionTest;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.publishing.PublishNotification;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.session.model.Session;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApproveTaskTest {

    static final String COLLECTION_ID = "138"; // we are 138
    static final String EMAIL = "danzig@misfits.com";

    @Mock
    private Collection collection;

    @Mock
    private CollectionDescription collectionDescription;

    @Mock
    private Session session;

    @Mock
    private CollectionReader collectionReader;

    @Mock
    private CollectionWriter collectionWriter;

    @Mock
    private ContentReader contentReader;

    @Mock
    private DataIndex dataIndex;

    @Mock
    private ContentDetailResolver contentDetailResolver;

    private ExecutorService executorService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() throws Exception {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Test
    public void createPublishNotificationShouldIncludePendingDeletes() throws Exception {
        // Given a collection that contains pending deletes.
        Path collectionPath = Files.createTempDirectory(Random.id()); // create a temp directory to generate content into
        Collection collection = CollectionTest.createCollection(collectionPath, "createPublishNotificationShouldIncludePendingDeletes");
        String uriToDelete = "some/uri/to/check";
        ContentDetail contentDetail = new ContentDetail("Title", uriToDelete, "type");
        PendingDelete pendingDelete = new PendingDelete("", contentDetail);
        collection.description.getPendingDeletes().add(pendingDelete);

        // When the publish notification is created as part of the approval process.
        PublishNotification publishNotification = ApproveTask.createPublishNotification(new ArrayList<>(), collection);

        // Then the publish notification contains the expected directory to delete.
        Assert.assertNotNull(publishNotification);
        Assert.assertTrue(publishNotification.hasUriToDelete(uriToDelete));
    }

    @Test
    public void shouldReturnFalseIfCollecionNull() throws Exception {
        when(collection.getId())
                .thenReturn("1234");
        Future<Boolean> result = executorService.submit(new ApproveTask(collection, null, null, null, null, null, contentDetailResolver));
        assertFalse(result.get());
    }

    @Test
    public void shouldReturnFalseIfCollecionDescriptionNull() throws Exception {
        Future<Boolean> result = executorService.submit(
                new ApproveTask(collection, null, null, null, null, null, contentDetailResolver));
        assertFalse(result.get());
    }

    @Test
    public void shouldReturnFalseIfSessionNull() throws Exception {
        when(collection.getDescription())
                .thenReturn(collectionDescription);
        when(collectionDescription.getId())
                .thenReturn(COLLECTION_ID);

        Future<Boolean> result = executorService.submit(
                new ApproveTask(collection, null, null, null, null, null, contentDetailResolver));
        assertFalse(result.get());
    }

    @Test
    public void shouldReturnFalseIfSessionEmailNull() throws Exception {
        when(collection.getDescription())
                .thenReturn(collectionDescription);
        when(collectionDescription.getId())
                .thenReturn(COLLECTION_ID);
        when(session.getEmail())
                .thenReturn(null);

        Future<Boolean> result = executorService.submit(
                new ApproveTask(collection, session, null, null, null, null, contentDetailResolver));
        assertFalse(result.get());
    }

    @Test
    public void shouldReturnFalseIfContentResolveReturnsIOEx() throws Exception {
        when(collection.getDescription())
                .thenReturn(collectionDescription);
        when(collectionDescription.getId())
                .thenReturn(COLLECTION_ID);
        when(session.getEmail())
                .thenReturn(EMAIL);
        when(contentDetailResolver.resolve(any(), any()))
                .thenThrow(new IOException("CABOOOM!"));

        ArgumentCaptor<Event> eventTypeArgumentCaptor = ArgumentCaptor.forClass(Event.class);


        doNothing().when(collectionDescription).addEvent(eventTypeArgumentCaptor.capture());

        Callable<Boolean> task = new ApproveTask(collection, session, collectionReader, collectionWriter,
                contentReader, dataIndex, contentDetailResolver);

        Future<Boolean> result = executorService.submit(task);
        assertFalse(result.get());
        assertThat(eventTypeArgumentCaptor.getValue().type, equalTo(EventType.APPROVAL_FAILED));
        verify(contentDetailResolver, times(1)).resolve(any(), any());
    }

    @Test
    public void shouldSetCollectionStateToApproved() throws IOException {
        CollectionDescription description = new CollectionDescription();

        when(collection.getDescription())
                .thenReturn(description);
        when(session.getEmail())
                .thenReturn("test@ons.gov.uk");

        ApproveTask approveTask = new ApproveTask(collection, session, collectionReader, collectionWriter,
                contentReader, dataIndex, contentDetailResolver);

        approveTask.approveCollection();

        assertThat(description.approvalStatus, equalTo(ApprovalStatus.COMPLETE));
        assertThat(description.events.size(), equalTo(1));

        Event event = description.events.get(0);
        assertThat(event.getEmail(), equalTo("test@ons.gov.uk"));
        assertThat(event.getType(), equalTo(EventType.APPROVED));

        verify(collection, times(2)).getDescription();
    }

    @Test(expected = IOException.class)
    public void shouldSetCollectionStateToErrorIfSaveFails() throws IOException {
        CollectionDescription description = new CollectionDescription();

        when(collection.getDescription())
                .thenReturn(description);
        when(session.getEmail())
                .thenReturn("test@ons.gov.uk");
        when(collection.save())
                .thenThrow(new RuntimeException());

        ApproveTask approveTask = new ApproveTask(collection, session, collectionReader, collectionWriter,
                contentReader, dataIndex, contentDetailResolver);

        approveTask.approveCollection();

        assertThat(description.approvalStatus, equalTo(ApprovalStatus.ERROR));
        assertThat(description.events.size(), equalTo(2));

        Event event = description.events.get(0);
        assertThat(event.getEmail(), equalTo("test@ons.gov.uk"));
        assertThat(event.getType(), equalTo(EventType.APPROVED));

        Event errorEvent = description.events.get(0);
        assertThat(errorEvent.getEmail(), equalTo("system"));
        assertThat(errorEvent.getType(), equalTo(EventType.APPROVAL_FAILED));

        verify(collection, times(4)).getDescription();
    }
}