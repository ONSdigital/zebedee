package com.github.onsdigital.zebedee.keyring.migration;

import com.github.onsdigital.slack.messages.PostMessage;
import com.github.onsdigital.slack.messages.PostMessageAttachment;
import com.github.onsdigital.slack.messages.PostMessageField;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.json.Event;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.json.Events;
import com.github.onsdigital.zebedee.keyring.CollectionKeyCache;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.slack.Notifier;
import com.github.onsdigital.zebedee.util.slack.SlackNotifier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.onsdigital.slack.messages.Colour.DANGER;
import static com.github.onsdigital.zebedee.json.CollectionType.manual;
import static com.github.onsdigital.zebedee.keyring.migration.KeyringHealthCheckerImpl.MESSAGE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class KeyringHealthCheckerImplTest {

    private static final String TEST_EMAIL = "123@ons.gov.uk";

    @Mock
    private Session session;

    @Mock
    private PermissionsService permissionsService;

    @Mock
    private CollectionKeyCache keyCache;

    @Mock
    private Collections collections;

    @Mock
    private Collection collection;

    @Mock
    private CollectionDescription description;

    @Mock
    private Notifier slackNotifier;

    private KeyringHealthChecker healthChecker;
    private Date createdDate;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        createdDate = new Date();

        this.healthChecker = new KeyringHealthCheckerImpl(permissionsService, collections, keyCache,
                slackNotifier);

        when(session.getEmail())
                .thenReturn(TEST_EMAIL);

        when(collection.getDescription())
                .thenReturn(description);

        when(description.getName())
                .thenReturn("col1");

        when(description.getType())
                .thenReturn(manual);

        Events events = new Events() {{
            add(new Event(createdDate, EventType.CREATED, "admin@ons.gov.uk"));
        }};

        when(description.getEvents())
                .thenReturn(events);
    }

    @Test
    public void check_sessionNull_shouldDoNothing() {
        session = null;

        healthChecker.check(session);

        verifyZeroInteractions(collections, keyCache, slackNotifier);
    }

    @Test
    public void check_sessionEmailNull_shouldDoNothing() {
        when(session.getEmail())
                .thenReturn(null);

        healthChecker.check(session);

        verifyZeroInteractions(collections, keyCache, slackNotifier);
    }

    @Test
    public void check_sessionEmailEmpty_shouldDoNothing() {
        when(session.getEmail())
                .thenReturn("");

        healthChecker.check(session);

        verifyZeroInteractions(collections, keyCache, slackNotifier);

    }

    @Test
    public void check_permissionsServiceError_shouldDoNothing() throws Exception {
        when(permissionsService.isAdministrator(session))
                .thenThrow(IOException.class);

        healthChecker.check(session);

        verify(permissionsService, times(1)).isAdministrator(session);
        verifyZeroInteractions(collections, keyCache, slackNotifier);
    }

    @Test
    public void check_isAdminReturnsFalse_shouldDoNothing() throws Exception {
        when(permissionsService.isAdministrator(session))
                .thenReturn(false);

        healthChecker.check(session);

        verify(permissionsService, times(1)).isAdministrator(session);
        verifyZeroInteractions(collections, keyCache, slackNotifier);
    }

    @Test
    public void check_listCollectionsError_shouldDoNothing() throws Exception {
        when(permissionsService.isAdministrator(session))
                .thenReturn(true);

        when(collections.list())
                .thenThrow(IOException.class);

        healthChecker.check(session);

        verify(permissionsService, times(1)).isAdministrator(session);
        verify(collections, times(1)).list();
        verifyZeroInteractions(keyCache, slackNotifier);
    }

    @Test
    public void check_listCollectionsReturnsNull_shouldDoNothing() throws Exception {
        when(permissionsService.isAdministrator(session))
                .thenReturn(true);

        when(collections.list())
                .thenReturn(null);

        healthChecker.check(session);

        verify(permissionsService, times(1)).isAdministrator(session);
        verify(collections, times(1)).list();
        verifyZeroInteractions(keyCache, slackNotifier);
    }

    @Test
    public void check_listCollectionsReturnsEmpty_shouldDoNothing() throws Exception {
        when(permissionsService.isAdministrator(session))
                .thenReturn(true);

        when(collections.list())
                .thenReturn(new Collections.CollectionList());

        healthChecker.check(session);

        verify(permissionsService, times(1)).isAdministrator(session);
        verify(collections, times(1)).list();
        verifyZeroInteractions(keyCache, slackNotifier);
    }

    @Test
    public void check_schedulerKeyCacheListError_shouldDoNothing() throws Exception {
        when(permissionsService.isAdministrator(session))
                .thenReturn(true);

        when(collection.getId())
                .thenReturn("12345");

        Collections.CollectionList collectionList = new Collections.CollectionList() {{
            add(collection);
        }};

        when(collections.list())
                .thenReturn(collectionList);

        when(keyCache.list())
                .thenThrow(IOException.class);

        healthChecker.check(session);

        verify(permissionsService, times(1)).isAdministrator(session);
        verify(collections, times(1)).list();
        verify(keyCache, times(1)).list();
    }

    @Test
    public void check_noAbsentKeys_shouldDoNothing() throws Exception {
        when(permissionsService.isAdministrator(session))
                .thenReturn(true);

        when(collection.getId())
                .thenReturn("12345");

        Collections.CollectionList collectionList = new Collections.CollectionList() {{
            add(collection);
        }};

        when(collections.list())
                .thenReturn(collectionList);

        Set<String> schedulerKeys = new HashSet<String>() {{
            add("12345");
        }};

        when(keyCache.list())
                .thenReturn(schedulerKeys);

        healthChecker.check(session);

        verify(permissionsService, times(1)).isAdministrator(session);
        verify(collections, times(1)).list();
        verify(keyCache, times(1)).list();
        verifyZeroInteractions(slackNotifier);
    }

    @Test
    public void check_slackNotifierError_shouldDoNothing() throws Exception {
        when(permissionsService.isAdministrator(session))
                .thenReturn(true);

        when(collection.getId())
                .thenReturn("12345");

        Collections.CollectionList collectionList = new Collections.CollectionList() {{
            add(collection);
        }};

        when(collections.list())
                .thenReturn(collectionList);

        Set<String> schedulerKeys = new HashSet<>() ;

        when(keyCache.list())
                .thenReturn(schedulerKeys);

        ArgumentCaptor<PostMessage> captor = ArgumentCaptor.forClass(PostMessage.class);

        doThrow(Exception.class)
                .when(slackNotifier)
                        .sendSlackMessage(captor.capture());

        healthChecker.check(session);


        verify(permissionsService, times(1)).isAdministrator(session);
        verify(collections, times(1)).list();
        verify(keyCache, times(1)).list();
        verify(slackNotifier, times(1)).sendSlackMessage(any());

        PostMessage msg = captor.getValue();
        assertPostMessage(msg);
    }

    @Test
    public void check_success_shouldSendSlackNotification() throws Exception {
        when(permissionsService.isAdministrator(session))
                .thenReturn(true);

        when(collection.getId())
                .thenReturn("12345");

        Collections.CollectionList collectionList = new Collections.CollectionList() {{
            add(collection);
        }};

        when(collections.list())
                .thenReturn(collectionList);

        Set<String> schedulerKeys = new HashSet<>() ;

        when(keyCache.list())
                .thenReturn(schedulerKeys);

        ArgumentCaptor<PostMessage> captor = ArgumentCaptor.forClass(PostMessage.class);
        doNothing()
                .when(slackNotifier)
                .sendSlackMessage(captor.capture());

        healthChecker.check(session);

        verify(permissionsService, times(1)).isAdministrator(session);
        verify(collections, times(1)).list();
        verify(keyCache, times(1)).list();
        verify(slackNotifier, times(1)).sendSlackMessage(any());

        PostMessage msg = captor.getValue();
        assertPostMessage(msg);
    }

    @Test
    public void check_createdEventNotFound_shouldSendSlackNotificationWithoutCreatedFields() throws Exception {
        when(permissionsService.isAdministrator(session))
                .thenReturn(true);

        when(collection.getId())
                .thenReturn("12345");

        when(description.getEvents())
                .thenReturn(new Events());

        Collections.CollectionList collectionList = new Collections.CollectionList() {{
            add(collection);
        }};

        when(collections.list())
                .thenReturn(collectionList);

        Set<String> schedulerKeys = new HashSet<>() ;

        when(keyCache.list())
                .thenReturn(schedulerKeys);

        ArgumentCaptor<PostMessage> captor = ArgumentCaptor.forClass(PostMessage.class);
        doNothing()
                .when(slackNotifier)
                .sendSlackMessage(captor.capture());

        healthChecker.check(session);

        verify(permissionsService, times(1)).isAdministrator(session);
        verify(collections, times(1)).list();
        verify(keyCache, times(1)).list();
        verify(slackNotifier, times(1)).sendSlackMessage(any());

        PostMessage msg = captor.getValue();
        assertThat(msg, is(notNullValue()));
        assertThat(msg.getText(), equalTo(MESSAGE));
        assertThat(msg.getEmoji(), equalTo(":flo:"));
        assertThat(msg.getAttachments(), is(notNullValue()));
        assertThat(msg.getAttachments().size(), equalTo(1));

        PostMessageAttachment attch = msg.getAttachments().get(0);
        assertThat(attch.getText(), equalTo("12345"));
        assertThat(attch.getTitle(), equalTo("Collection ID"));
        assertThat(attch.getColor(), equalTo(DANGER.getColor()));
        assertThat(attch.getFields(), is(notNullValue()));

        // 4 fields: Colleciton name, creation date, created by and publish type.
        assertThat(attch.getFields().size(), equalTo(3));

        List<PostMessageField> fields = attch.getFields();
        assertThat(fields.get(0).getTitle(), equalTo("Collection Name"));
        assertThat(fields.get(0).getValue(), equalTo("col1"));
        assertThat(fields.get(0).isShort(), equalTo(false));

        assertThat(fields.get(1).getTitle(), equalTo("Missing from"));
        assertThat(fields.get(1).getValue(), equalTo(TEST_EMAIL));
        assertThat(fields.get(1).isShort(), equalTo(true));

        assertThat(fields.get(2).getTitle(), equalTo("Publish Type"));
        assertThat(fields.get(2).getValue(), equalTo(manual.name()));
        assertThat(fields.get(2).isShort(), equalTo(true));
    }

    void assertPostMessage(PostMessage msg) {
        assertThat(msg, is(notNullValue()));
        assertThat(msg.getText(), equalTo(MESSAGE));
        assertThat(msg.getEmoji(), equalTo(":flo:"));
        assertThat(msg.getAttachments(), is(notNullValue()));
        assertThat(msg.getAttachments().size(), equalTo(1));

        PostMessageAttachment attch = msg.getAttachments().get(0);
        assertThat(attch.getText(), equalTo("12345"));
        assertThat(attch.getTitle(), equalTo("Collection ID"));
        assertThat(attch.getColor(), equalTo(DANGER.getColor()));
        assertThat(attch.getFields(), is(notNullValue()));

        // 5 fields: Colleciton name, affected user, publish type, created by and creation date
        assertThat(attch.getFields().size(), equalTo(5));

        int i = 0;
        List<PostMessageField> fields = attch.getFields();
        assertThat(fields.get(i).getTitle(), equalTo("Collection Name"));
        assertThat(fields.get(i).getValue(), equalTo("col1"));
        assertThat(fields.get(i).isShort(), equalTo(false));
        i++;

        assertThat(fields.get(i).getTitle(), equalTo("Missing from"));
        assertThat(fields.get(i).getValue(), equalTo(TEST_EMAIL));
        assertThat(fields.get(i).isShort(), equalTo(true));
        i++;

        assertThat(fields.get(i).getTitle(), equalTo("Publish Type"));
        assertThat(fields.get(i).getValue(), equalTo(manual.name()));
        assertThat(fields.get(i).isShort(), equalTo(true));
        i++;

        assertThat(fields.get(i).getTitle(), equalTo("Creation Date"));
        assertThat(fields.get(i).getValue(), equalTo(createdDate.toString()));
        assertThat(fields.get(i).isShort(), equalTo(true));
        i++;

        assertThat(fields.get(i).getTitle(), equalTo("Created By"));
        assertThat(fields.get(i).getValue(), equalTo("admin@ons.gov.uk"));
        assertThat(fields.get(i).isShort(), equalTo(true));
    }
}
