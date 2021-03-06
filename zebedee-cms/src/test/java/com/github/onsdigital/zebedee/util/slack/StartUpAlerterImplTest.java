package com.github.onsdigital.zebedee.util.slack;

import com.github.onsdigital.slack.Profile;
import com.github.onsdigital.slack.client.PostMessageResponse;
import com.github.onsdigital.slack.client.SlackClient;
import com.github.onsdigital.slack.messages.Colour;
import com.github.onsdigital.slack.messages.PostMessage;
import com.github.onsdigital.slack.messages.PostMessageAttachment;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class StartUpAlerterImplTest {

    @Mock
    private SlackClient slackClient;

    @Mock
    private Profile profile;

    @Mock
    private PostMessage postMessage;

    @Mock
    private PostMessageResponse messageResponse;

    private StartUpAlerter alerter;
    private List<String> channels;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        channels = new ArrayList<>();
        channels.add("chan-A");
        channels.add("chan-B");

        alerter = new StartUpAlerterImpl(slackClient, channels);
    }

    @Test
    public void testNotifyLocked_success_shouldSendAlerts() throws Exception {
        when(slackClient.getProfile())
                .thenReturn(profile);

        when(profile.newPostMessage(any(), any()))
                .thenReturn(postMessage);

        when(postMessage.addAttachment(any()))
                .thenReturn(postMessage);

        when(slackClient.sendMessage(any(PostMessage.class)))
                .thenReturn(messageResponse);

        when(slackClient.sendMessage(postMessage))
                .thenReturn(messageResponse);

        when(messageResponse.getTs())
                .thenReturn("time.now");

        when(postMessage.ts(any()))
                .thenReturn(postMessage);

        when(postMessage.channel(any()))
                .thenReturn(postMessage);

        ArgumentCaptor<PostMessageAttachment> captor = ArgumentCaptor.forClass(PostMessageAttachment.class);

        alerter.queueLocked();

        verify(profile, times(1)).newPostMessage("chan-A", "Publishing system restart complete");
        verify(profile, times(1)).newPostMessage("chan-B", "Publishing system restart complete");
        verify(postMessage, times(2)).addAttachment(captor.capture());

        assertThat(captor.getAllValues().size(), equalTo(2));

        captor.getAllValues().stream().forEach(attch -> {
            assertThat(attch.getTitle(), equalTo("Administrator log in required"));
            assertThat(attch.getText(), equalTo("Please log out of any existing session and log in again to unlock the publishing queue."));
            assertThat(attch.getColor(), equalTo(Colour.WARNING.getColor()));
        });
    }

    @Test
    public void testNotifyLocked_notifyError_shouldDoNothing() throws Exception {
        when(slackClient.getProfile())
                .thenReturn(profile);

        when(profile.newPostMessage(any(), any()))
                .thenReturn(postMessage);

        when(postMessage.addAttachment(any()))
                .thenReturn(postMessage);

        when(slackClient.sendMessage(any(PostMessage.class)))
                .thenReturn(messageResponse)
                .thenThrow(RuntimeException.class);

        alerter.queueLocked();

        verify(slackClient, times(2)).sendMessage(any());
    }

    @Test
    public void testNotifyLocked_alreadySent_shouldDoNothing() throws Exception {
        alerter = new StartUpAlerterImpl(slackClient, channels, true);

        // If queueUnlockedAlerts is not empty it means the locked notification has already been sent.
        // Set this to a non empty value
        List<Callable<PostMessage>> tasks = new ArrayList<>();
        tasks.add(() -> null);
        ReflectionTestUtils.setField(alerter, "queueUnlockedAlerts", tasks);

        alerter.queueLocked();

        verifyZeroInteractions(slackClient);
    }

    @Test
    public void testNotifyLocked_queueNotLocked_shouldDoNothing() throws Exception {
        alerter = new StartUpAlerterImpl(slackClient, channels, false);

        alerter.queueLocked();

        verifyZeroInteractions(slackClient);
    }

    @Test
    public void testNotifyLocked_noChannelsConfigured_shouldDoNothing() throws Exception {
        alerter = new StartUpAlerterImpl(slackClient, new ArrayList<>(), true);

        alerter.queueLocked();

        verifyZeroInteractions(slackClient);
    }

    @Test
    public void testNotifyUnlocked_queueUnlockedAlertsNull_shouldDoNothing() throws Exception {
        alerter = new StartUpAlerterImpl(slackClient, channels, true);

        alerter.queueUnlocked();

        verifyZeroInteractions(slackClient);
    }

    @Test
    public void testNotifyUnlocked_queueUnlockedAlertsEmpty_shouldDoNothing() throws Exception {
        alerter = new StartUpAlerterImpl(slackClient, channels, true);
        ReflectionTestUtils.setField(alerter, "queueUnlockedAlerts", new ArrayList<>());

        alerter.queueUnlocked();

        verifyZeroInteractions(slackClient);
    }


    @Test
    public void testNotifyUnlocked_queueNotLocked_shouldDoNothing() throws Exception {
        alerter = new StartUpAlerterImpl(slackClient, channels, false);

        alerter.queueUnlocked();

        verifyZeroInteractions(slackClient);
    }

    @Test
    public void testNotifyUnlocked_sendMessageError_shouldFailSilently() throws Exception {
        StartUpAlerterImpl alerter = new StartUpAlerterImpl(slackClient, channels, true);

        when(slackClient.getProfile())
                .thenReturn(profile);

        when(postMessage.getAttachments())
                .thenReturn(new ArrayList<PostMessageAttachment>() {{
                    add(mock(PostMessageAttachment.class));
                }});

        when(slackClient.updateMessage(postMessage))
                .thenThrow(new RuntimeException("Something went wrong sending slack notification"));

        List<Callable<Void>> queueUnlockedAlerts = new ArrayList<>();
        queueUnlockedAlerts.add(alerter.newQueueUnlockedAlertTask(postMessage));
        ReflectionTestUtils.setField(alerter, "queueUnlockedAlerts", queueUnlockedAlerts);

        alerter.queueUnlocked();

        verify(slackClient, times(1)).updateMessage(postMessage);

        ArgumentCaptor<PostMessageAttachment> captor = ArgumentCaptor.forClass(PostMessageAttachment.class);
        verify(postMessage, times(1)).addAttachment(captor.capture());

        PostMessageAttachment attachment = captor.getValue();
        assertThat(attachment.getTitle(), equalTo("Resolved"));
        assertThat(attachment.getColor(), equalTo(Colour.GOOD.getColor()));
        assertTrue(StringUtils.startsWith(attachment.getText(), "Publishing queue successfully unlocked `"));
    }

    @Test
    public void testNotifyUnlocked_success_shouldSendAlerts() throws Exception {
        StartUpAlerterImpl alerter = new StartUpAlerterImpl(slackClient, channels, true);

        List<Callable<Void>> queueUnlockedAlerts = new ArrayList<>();

        PostMessageAttachment attachment = new PostMessageAttachment();
        attachment.setTitle("original message");
        attachment.setColor(Colour.WARNING.toString());

        PostMessage originalMessage = new PostMessage();
        originalMessage.addAttachment(attachment);

        queueUnlockedAlerts.add(alerter.newQueueUnlockedAlertTask(originalMessage));

        ReflectionTestUtils.setField(alerter, "queueUnlockedAlerts", queueUnlockedAlerts);

        when(slackClient.getProfile())
                .thenReturn(profile);

        alerter.queueUnlocked();

        ArgumentCaptor<PostMessage> captor = ArgumentCaptor.forClass(PostMessage.class);
        verify(slackClient, times(1)).updateMessage(captor.capture());

        List<PostMessage> updatedMessages = captor.getAllValues();
        assertThat(updatedMessages.size(), equalTo(1));
        PostMessage msg = updatedMessages.get(0);

        assertThat(msg.getAttachments().size(), equalTo(2));


        PostMessageAttachment attch1 = msg.getAttachments().get(0);
        assertThat(attch1.getColor(), equalTo(Colour.GOOD.getColor()));
        assertThat(attch1.getTitle(), equalTo("original message"));

        PostMessageAttachment attch2 = msg.getAttachments().get(1);
        assertThat(attch2.getColor(), equalTo(Colour.GOOD.getColor()));
        assertThat(attch2.getTitle(), equalTo("Resolved"));
        assertTrue(attch2.getText().startsWith("Publishing queue successfully unlocked `"));
    }

    @Test
    public void testNotifyUnlocked_success_shouldClearUnlockTasksOnCompletion() throws Exception {
        StartUpAlerterImpl alerter = new StartUpAlerterImpl(slackClient, channels, true);

        List<Callable<Void>> queueUnlockedAlerts = new ArrayList<>();
        queueUnlockedAlerts.add(alerter.newQueueUnlockedAlertTask(postMessage));

        ReflectionTestUtils.setField(alerter, "queueUnlockedAlerts", queueUnlockedAlerts);

        alerter.queueUnlocked();

        verify(slackClient, times(1)).updateMessage(any());

        List<Callable<PostMessage>> unlockTasks =
                (List<Callable<PostMessage>>)ReflectionTestUtils.getField(alerter, "queueUnlockedAlerts");

        assertTrue(unlockTasks.isEmpty());
    }
}
