package com.github.onsdigital.zebedee.util.slack;

import com.github.onsdigital.slack.Profile;
import com.github.onsdigital.slack.client.PostMessageResponse;
import com.github.onsdigital.slack.client.SlackClient;
import com.github.onsdigital.slack.messages.Colour;
import com.github.onsdigital.slack.messages.PostMessage;
import com.github.onsdigital.slack.messages.PostMessageAttachment;
import com.github.onsdigital.zebedee.notification.NotificationException;
import com.github.onsdigital.zebedee.notification.StartUpNotifier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.util.slack.SlackStartUpNotifier.HELP_HINT_FMT;
import static java.text.MessageFormat.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class SlackStartUpNotifierTest {

    private StartUpNotifier notifier;

    @Mock
    private SlackClient sClient;

    @Mock
    private PostMessageResponse response;

    private Profile profile;
    private List<String> channels;
    private String supportChannelID;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        channels = new ArrayList<>();
        channels.add("AAA");
        channels.add("BBB");

        supportChannelID = "666";

        notifier = new SlackStartUpNotifier(sClient, channels, supportChannelID);

        setHasRun(false); // reset the static var to its initial state before each test.
    }

    @Test
    public void notifyStartUpComplete_success_shouldSendNotifications() throws Exception {
        profile = newProfile();

        when(sClient.getProfile())
                .thenReturn(profile);

        ArgumentCaptor<PostMessage> captor = ArgumentCaptor.forClass(PostMessage.class);
        when(sClient.sendMessage(captor.capture()))
                .thenReturn(response);

        when(response.isOk())
                .thenReturn(true);

        boolean notificationSent = notifier.notifyStartUpComplete();

        assertTrue(notificationSent);
        verify(sClient, times(2)).sendMessage(any(PostMessage.class));

        List<PostMessage> sentMessages = captor.getAllValues();
        assertSentMessages(sentMessages);
    }

    @Test
    public void notifyStartUpComplete_unsuccessful_shouldReturnFalse() throws Exception {
        profile = newProfile();

        when(sClient.getProfile())
                .thenReturn(profile);

        ArgumentCaptor<PostMessage> captor = ArgumentCaptor.forClass(PostMessage.class);
        when(sClient.sendMessage(captor.capture()))
                .thenReturn(response);

        when(response.isOk())
                .thenReturn(false);

        boolean notificationSent = notifier.notifyStartUpComplete();

        assertFalse(notificationSent);
        verify(sClient, times(2)).sendMessage(any(PostMessage.class));

        List<PostMessage> sentMessages = captor.getAllValues();
        assertSentMessages(sentMessages);
    }

    @Test
    public void notifyStartUpComplete_sendMessageErr_shouldThrowEx() throws Exception {
        profile = newProfile();

        when(sClient.getProfile())
                .thenReturn(profile);

        ArgumentCaptor<PostMessage> captor = ArgumentCaptor.forClass(PostMessage.class);

        RuntimeException innerEx = new RuntimeException("borked");
        when(sClient.sendMessage(captor.capture()))
                .thenThrow(innerEx);

        NotificationException ex = assertThrows(NotificationException.class, () -> notifier.notifyStartUpComplete());

        assertThat(ex.getMessage(), startsWith("error sending publishing system start up notification"));
        Throwable cause = ex.getCause();
        assertTrue(cause instanceof NotificationException);
        assertThat(cause.getMessage(), startsWith("error sending start up notification to slack channel"));

        verify(sClient, times(2)).sendMessage(any(PostMessage.class));
        List<PostMessage> sentMessages = captor.getAllValues();
        assertSentMessages(sentMessages);
    }

    @Test
    public void notifyStartUpComplete_alreadyNotified_shouldNotSendNotifications() throws Exception {
        setHasRun(true);

        boolean notificationSent = notifier.notifyStartUpComplete();

        assertFalse(notificationSent);
        assertFalse(notificationSent);
        verifyNoInteractions(sClient);
    }

    private void assertSentMessages(List<PostMessage> sentMessages) {
        assertThat(sentMessages, is(notNullValue()));
        assertThat(sentMessages.size(), equalTo(channels.size()));

        // Create a mapping of each sent message to the channel it was sent to.
        Map<String, PostMessage> mapping = sentMessages.stream()
                .collect(Collectors.toMap(e1 -> e1.getChannel(), e2 -> e2));

        channels.stream().forEach(chan -> {
            assertTrue(mapping.containsKey(chan));
            PostMessage msg = mapping.get(chan);

            assertThat(msg.getChannel(), equalTo(chan));
            assertThat(msg.getText(), equalTo("Publishing system restart completed successfully :tada:"));
            assertThat(msg.getAttachments().size(), equalTo(2));

            List<PostMessageAttachment> attachments = msg.getAttachments();
            assertThat(attachments.get(0).getTitle(), equalTo("Time"));
            assertThat(attachments.get(0).getColor(), equalTo(Colour.GOOD.getColor()));

            assertThat(attachments.get(1).getTitle(), equalTo("Not expecting this alert?"));
            assertThat(attachments.get(1).getText(), equalTo(format(HELP_HINT_FMT, supportChannelID)));
            assertThat(attachments.get(1).getColor(), equalTo(Colour.GOOD.getColor()));
        });
    }

    void setHasRun(boolean val) throws Exception {
        Field f = SlackStartUpNotifier.class.getDeclaredField("hasRun");
        f.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);

        f.set(null, new AtomicBoolean(val));
    }

    Profile newProfile() throws Exception {
        Constructor<Profile> constr = (Constructor<Profile>) Profile.class.getDeclaredConstructors()[0];
        constr.setAccessible(true);
        return constr.newInstance("", "", "");
    }
}
