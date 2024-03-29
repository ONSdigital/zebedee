package com.github.onsdigital.zebedee.util.slack;

import com.github.onsdigital.slack.Profile;
import com.github.onsdigital.slack.client.PostMessageResponse;
import com.github.onsdigital.slack.client.SlackClient;
import com.github.onsdigital.slack.messages.Colour;
import com.github.onsdigital.slack.messages.PostMessage;
import com.github.onsdigital.slack.messages.PostMessageAttachment;
import com.github.onsdigital.slack.messages.PostMessageField;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.github.onsdigital.zebedee.json.CollectionType.manual;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SlackNotifierTest {
    @Mock
    private SlackClient slackClient;

    @Mock
    private PostMessageResponse messageResponse;

    @Mock
    private PostMessage pm;

    @Mock
    private Collection collection;

    @Mock
    private CollectionDescription description;

    @Mock
    private Profile profile;

    @Mock
    private AttachmentField field;

    private SlackNotifier sn;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        when(collection.getDescription())
                .thenReturn(description);
        when(description.getType())
                .thenReturn(manual);
        when(description.getName())
                .thenReturn("Test");
        when(collection.getId())
                .thenReturn("101");
        when(slackClient.getProfile())
                .thenReturn(profile);
        when(profile.newPostMessage(anyString(), anyString()))
                .thenReturn(pm);
        when(field.getTitle())
                .thenReturn("WarningTitle");
        when(field.getMessage())
                .thenReturn("Ons Warning message");
        when(field.isShort())
                .thenReturn(true);
        sn = new SlackNotifier(slackClient);
    }

    @Test
    public void sendSlackMessageNil() throws Exception {
        when(slackClient.sendMessage(pm))
                .thenReturn(messageResponse);

        sn.sendSlackMessage(pm);

        verify(slackClient, times(1)).sendMessage(any());
    }

    @Test
    public void sendSlackMessageException() {
        when(slackClient.sendMessage(pm))
                .thenThrow(RuntimeException.class);

        Exception actual = assertThrows(Exception.class, () -> sn.sendSlackMessage(pm));
        assertThat(actual.getMessage(), equalTo("unexpected error sending slack message"));
        verify(slackClient, times(1)).sendMessage(pm);
    }

    @Test
    public void sendCollectionAlarmException() {
        PostMessage pm = new PostMessage("username", "channel", ":grinning:", "yes");

        when(profile.newPostMessage(anyString(), anyString()))
                .thenReturn(pm);
        when(slackClient.sendMessage(pm))
                .thenThrow(RuntimeException.class);

        boolean returnValue = sn.sendCollectionAlarm(collection, "Any", "custom", new Exception("Hello test"));

        verify(slackClient, times(1)).sendMessage(pm);
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionAlarmExceptionNull() {
        boolean returnValue = sn.sendCollectionAlarm(collection, "", "message", new NullPointerException());

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionAlarmExceptionEmpty() {
        boolean returnValue = sn.sendCollectionAlarm(collection, "", "message", new Exception(""));

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionAlarmChannelEmpty() {
        boolean returnValue = sn.sendCollectionAlarm(collection, "", "message", new Exception("Hello test"));

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionAlarmChannelNull() {
        boolean returnValue = sn.sendCollectionAlarm(collection, null, "message", new Exception("Hello test"));

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionAlarmMessageEmpty() {
        boolean returnValue = sn.sendCollectionAlarm(collection, "SlackChannel", "", new Exception("Hello test"));

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }


    @Test
    public void sendCollectionAlarmMessageNull() {
        boolean returnValue = sn.sendCollectionAlarm(collection, "SlackChannel", null, new Exception("Hello test"));

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionAlarmCollectionNull() {
        collection = null;
        boolean returnValue = sn.sendCollectionAlarm(collection, "SlackChannel", "Ons custom message", new Exception("Hello test"));

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionAlarmCollectionDescriptionNull() {
        when(collection.getDescription())
                .thenReturn(null);

        boolean returnValue = sn.sendCollectionAlarm(collection, "SlackChannel", "Ons custom message", new Exception("Hello test"));

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionAlarmCollectionDescriptionTypeNull() {
        when(description.getType())
                .thenReturn(null);

        boolean returnValue = sn.sendCollectionAlarm(collection, "SlackChannel", "Ons custom message", new Exception("Hello test"));

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionAlarmCollectionDescriptionNameEmpty() {
        when(description.getName())
                .thenReturn("");

        boolean returnValue = sn.sendCollectionAlarm(collection, "SlackChannel", "Ons custom message", new Exception("Hello test"));

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionAlarmCollectionDescriptionNameNull() {
        when(description.getName())
                .thenReturn(null);

        boolean returnValue = sn.sendCollectionAlarm(collection, "SlackChannel", "Ons custom message", new Exception("Hello test"));

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionAlarmCollectionDescriptionIdEmpty() {
        when(collection.getId())
                .thenReturn("");

        boolean returnValue = sn.sendCollectionAlarm(collection, "SlackChannel", "Ons custom message", new Exception("Hello test"));

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionAlarmCollectionDescriptionIdNull() {
        when(collection.getId())
                .thenReturn(null);

        boolean returnValue = sn.sendCollectionAlarm(collection, "SlackChannel", "Ons custom message", new Exception("Hello test"));

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionAlarmSuccess() {
        PostMessage pm = new PostMessage();
        when(profile.newPostMessage(anyString(), anyString()))
                .thenReturn(pm);

        boolean returnValue = sn.sendCollectionAlarm(collection, "Any", "custom", new Exception("Hello test"));
        PostMessageAttachment attach = pm.getAttachments().get(0);

        assertThat(pm.getAttachments().size(), equalTo(1));
        verifyAttachment(attach, "Alert", "Collection Alarm", Colour.DANGER.getColor());
        assertThat(attach.getFields().size(), equalTo(4));
        verifyFields(attach.getFields().get(0), "Publishing Type", "manual", true);
        verifyFields(attach.getFields().get(1), "CollectionID", "101", false);
        verifyFields(attach.getFields().get(2), "Collection Name", "Test", false);
        verifyFields(attach.getFields().get(3), "exception", "Hello test", false);
        verify(slackClient, times(1)).sendMessage(pm);
        assertTrue(returnValue);
    }


    @Test
    public void sendCollectionWarningException() {
        PostMessage pm = new PostMessage("username", "channel", ":grinning:", "yes");

        when(profile.newPostMessage(anyString(), anyString()))
                .thenReturn(pm);
        when(slackClient.sendMessage(pm))
                .thenThrow(RuntimeException.class);

        boolean returnValue = sn.sendCollectionWarning(collection, "Any", "custom", field);

        Exception actual = assertThrows(Exception.class, () -> sn.sendSlackMessage(pm));
        verify(slackClient, times(2)).sendMessage(pm);
        assertFalse(returnValue);
    }


    @Test
    public void sendCollectionWarningChannelEmpty() {
        boolean returnValue = sn.sendCollectionWarning(collection, "", "message", field);

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionWarningChannelNull() {
        boolean returnValue = sn.sendCollectionWarning(collection, null, "message", field);

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionWarningMessageEmpty() {
        boolean returnValue = sn.sendCollectionWarning(collection, "SlackChannel", "", field);

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionWarningMessageNull() {
        boolean returnValue = sn.sendCollectionWarning(collection, "SlackChannel", null, field);

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionWarningCollectionNull() {
        collection = null;

        boolean returnValue = sn.sendCollectionWarning(collection, "SlackChannel", "Ons custom message", field);

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionWarningCollectionDescriptionNull() {
        when(collection.getDescription())
                .thenReturn(null);

        boolean returnValue = sn.sendCollectionWarning(collection, "SlackChannel", "Ons custom message", field);

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionWarningCollectionDescriptionTypeNull() {
        when(description.getType())
                .thenReturn(null);

        boolean returnValue = sn.sendCollectionWarning(collection, "SlackChannel", "Ons custom message", field);

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionWarningCollectionDescriptionNameEmpty() {
        when(description.getName())
                .thenReturn("");

        boolean returnValue = sn.sendCollectionWarning(collection, "SlackChannel", "Ons custom message", field);

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionWarningCollectionDescriptionIdEmpty() {
        when(collection.getId())
                .thenReturn("");

        boolean returnValue = sn.sendCollectionWarning(collection, "SlackChannel", "Ons custom message", field);

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionWarningCollectionDescriptionIdNull() {
        when(collection.getId())
                .thenReturn(null);

        boolean returnValue = sn.sendCollectionWarning(collection, "SlackChannel", "Ons custom message", field);

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionWarningCollectionDescriptionNameNull() {
        when(description.getName())
                .thenReturn(null);

        boolean returnValue = sn.sendCollectionWarning(collection, "SlackChannel", "Ons custom message", field);

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionWarningSuccess() {
        when(field.getTitle())
                .thenReturn("WarningTitle");
        when(field.getMessage())
                .thenReturn("Ons Warning message");
        when(field.isShort())
                .thenReturn(true);

        PostMessage pm = new PostMessage();
        when(profile.newPostMessage(anyString(), anyString()))
                .thenReturn(pm);

        boolean returnValue = sn.sendCollectionWarning(collection, "Any", "custom", field);
        PostMessageAttachment attach = pm.getAttachments().get(0);

        assertThat(pm.getAttachments().size(), equalTo(1));
        verifyAttachment(attach, "Warning", "Collection Warning", Colour.WARNING.getColor());
        assertThat(attach.getFields().size(), equalTo(4));
        verifyFields(attach.getFields().get(0), "Publishing Type", "manual", true);
        verifyFields(attach.getFields().get(1), "CollectionID", "101", false);
        verifyFields(attach.getFields().get(2), "Collection Name", "Test", false);
        verifyFields(attach.getFields().get(3), "WarningTitle", "Ons Warning message", true);
        verify(slackClient, times(1)).sendMessage(pm);
        assertTrue(returnValue);
    }


    @Test
    public void sendCollectionAlarmWithFieldsException() {
        PostMessage pm = new PostMessage("username", "channel", ":grinning:", "yes");

        when(profile.newPostMessage(anyString(), anyString()))
                .thenReturn(pm);
        when(slackClient.sendMessage(pm))
                .thenThrow(RuntimeException.class);

        boolean returnValue = sn.sendCollectionAlarm(collection, "Any", "custom", field);

        verify(slackClient, times(1)).sendMessage(pm);
        assertFalse(returnValue);
    }


    @Test
    public void sendCollectionAlarmWithFieldsChannelEmpty() {
        boolean returnValue = sn.sendCollectionWarning(collection, "", "message", field);

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionAlarmWithFieldsChannelNull() {
        boolean returnValue = sn.sendCollectionWarning(collection, null, "message", field);

        verify(slackClient, times(0)).sendMessage(pm);
        assertThat(pm.getAttachments().size(), equalTo(0));
        assertFalse(returnValue);
    }

    @Test
    public void sendCollectionAlarmWithFieldsSuccess() {
        PostMessage pm = new PostMessage();
        when(profile.newPostMessage(anyString(), anyString()))
                .thenReturn(pm);

        boolean returnValue = sn.sendCollectionWarning(collection, "Any", "custom", field);
        PostMessageAttachment attach = pm.getAttachments().get(0);

        assertThat(pm.getAttachments().size(), equalTo(1));
        verifyAttachment(attach, "Warning", "Collection Warning", Colour.WARNING.getColor());
        assertThat(attach.getFields().size(), equalTo(4));
        verifyFields(attach.getFields().get(0), "Publishing Type", "manual", true);
        verifyFields(attach.getFields().get(1), "CollectionID", "101", false);
        verifyFields(attach.getFields().get(2), "Collection Name", "Test", false);
        verifyFields(attach.getFields().get(3), "WarningTitle", "Ons Warning message", true);
        verify(slackClient, times(1)).sendMessage(pm);
        assertTrue(returnValue);
    }


    private void verifyFields(PostMessageField actual, String title, String value, Boolean wrapText) {
        assertThat(actual.getTitle(), equalTo(title));
        assertThat(actual.getValue(), equalTo(value));
        assertThat(actual.isShort(), equalTo(wrapText));
    }


    private void verifyAttachment(PostMessageAttachment actual, String title, String value, String colour) {
        assertThat(actual.getTitle(), equalTo(title));
        assertThat(actual.getText(), equalTo(value));
        assertThat(actual.getColor(), equalTo(colour));
    }
}

